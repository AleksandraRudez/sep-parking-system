package rs.sep.bank.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import rs.sep.bank.account.AccountRepository;
import rs.sep.bank.card.Card;
import rs.sep.bank.card.CardBrand;
import rs.sep.bank.card.CardRepository;
import rs.sep.bank.card.CardSecrets;
import rs.sep.bank.card.CardValidation;
import rs.sep.bank.common.BankException;
import rs.sep.bank.config.BankProperties;
import rs.sep.bank.exchange.ExchangeRateService;
import rs.sep.bank.merchant.AcquirerMerchant;
import rs.sep.bank.merchant.AcquirerMerchantRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PaymentSessionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSessionService.class);

    private final PaymentSessionRepository sessions;
    private final AcquirerMerchantRepository merchants;
    private final CardRepository cards;
    private final AccountRepository accounts;
    private final CardSecrets cardSecrets;
    private final ExchangeRateService exchange;
    private final PspNotifier pspNotifier;
    private final BankProperties props;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PaymentSessionService(PaymentSessionRepository sessions, AcquirerMerchantRepository merchants,
                                 CardRepository cards, AccountRepository accounts, CardSecrets cardSecrets,
                                 ExchangeRateService exchange, PspNotifier pspNotifier, BankProperties props,
                                 TransactionTemplate tx, Clock clock) {
        this.sessions = sessions;
        this.merchants = merchants;
        this.cards = cards;
        this.accounts = accounts;
        this.cardSecrets = cardSecrets;
        this.exchange = exchange;
        this.pspNotifier = pspNotifier;
        this.props = props;
        this.tx = tx;
        this.clock = clock;
    }

    /**
     * PSP trazi formu za placanje (koraci 2-3 u specifikaciji). Isti zahtev (MERCHANT_ID + STAN + PSP_TIMESTAMP)
     * vraca istu formu, pa ponovljen zahtev PSP-a ne pravi novu.
     */
    @Transactional
    public PaymentSession create(String merchantId, BigDecimal amount, String currency, String stan,
                                 Instant pspTimestamp) {
        AcquirerMerchant merchant = merchants.findByMerchantId(merchantId)
                .orElseThrow(() -> new BankException(BAD_REQUEST, "Nepoznat MERCHANT_ID"));
        if (!exchange.supports(currency)) {
            throw new BankException(BAD_REQUEST, "Valuta " + currency + " nije podrzana");
        }
        Optional<PaymentSession> existing = sessions.findByTrace(merchantId, stan, pspTimestamp);
        if (existing.isPresent()) {
            return existing.get();
        }
        Instant now = clock.instant();
        PaymentSession session = sessions.save(new PaymentSession(merchant, amount, currency, stan, pspTimestamp,
                now, now.plus(props.paymentUrlTtl())));
        log.info("Kreirana forma za placanje {} (prodavac {}, STAN {})", session.getId(), merchantId, stan);
        return session;
    }

    public String paymentUrl(PaymentSession session) {
        return props.publicUrl() + "/pay/" + session.getId();
    }

    /** Forma za prikaz; prazno ako je istekla ili vec iskoriscena. */
    @Transactional(readOnly = true)
    public Optional<PaymentSession> usableSession(UUID id) {
        return sessions.findWithMerchant(id).filter(s -> s.isUsable(clock.instant()));
    }

    /**
     * Kupac salje podatke o kartici (koraci 4-5). Format se proverava pre zauzimanja forme, pa greska u
     * kucanju ne trosi jedini pokusaj placanja.
     */
    public PaymentResult pay(UUID id, CardForm form) {
        String pan = CardValidation.normalizePan(form.pan());
        Map<String, String> errors = validate(pan, form);
        if (!errors.isEmpty()) {
            return new PaymentResult.Invalid(errors);
        }
        Instant now = clock.instant();
        if (tx.execute(status -> sessions.claim(id, now)) == 0) {
            return new PaymentResult.Unavailable("Link za placanje je istekao ili je vec iskoriscen.");
        }

        PaymentSession session;
        try {
            session = tx.execute(status -> authorize(id, pan, form, now));
        } catch (RuntimeException e) {
            log.error("Greska pri obradi placanja {}", id, e);
            session = tx.execute(status -> {
                PaymentSession s = sessions.findWithMerchant(id).orElseThrow();
                s.fail("Interna greska banke", clock.instant());
                return s;
            });
        }
        pspNotifier.notifyOutcome(session);
        return new PaymentResult.Processed(session.getStatus(), message(session), returnUrl(session));
    }

    /** Provera statusa po MERCHANT_ID + STAN + PSP_TIMESTAMP (kada PSP nije dobio obavestenje). */
    @Transactional
    public PaymentSession status(String merchantId, String stan, Instant pspTimestamp) {
        PaymentSession session = sessions.findByTrace(merchantId, stan, pspTimestamp)
                .orElseThrow(() -> new BankException(NOT_FOUND, "Transakcija ne postoji"));
        Instant now = clock.instant();
        if (session.getStatus() == PaymentSessionStatus.CREATED && !now.isBefore(session.getExpiresAt())) {
            session.expire(now);
        }
        return session;
    }

    private PaymentSession authorize(UUID id, String pan, CardForm form, Instant now) {
        PaymentSession session = sessions.findWithMerchant(id).orElseThrow();
        String last4 = pan.substring(pan.length() - 4);
        Optional<Card> card = cards.findByPanHmac(cardSecrets.panHmac(pan));
        YearMonth expiry = CardValidation.parseExpiry(form.expiry()).orElseThrow();

        boolean cardDataMatches = card.isPresent()
                && card.get().getExpiry().equals(expiry)
                && cardSecrets.matches(card.get().getCvvHmac(), cardSecrets.cvvHmac(pan, form.cvv()))
                && card.get().getHolderName().equalsIgnoreCase(form.holderName().trim());
        if (!cardDataMatches) {
            // Kupcu se ne otkriva koji podatak nije tacan
            session.decline("Podaci o kartici nisu ispravni", last4, now);
            log.info("Placanje {} odbijeno: neispravni podaci o kartici *{}", id, last4);
            return session;
        }

        var account = card.get().getAccount();
        BigDecimal debit = exchange.convert(session.getAmount(), session.getCurrency(), account.getCurrency());
        if (accounts.reserve(account.getId(), debit) == 0) {
            session.decline("Nedovoljno sredstava na racunu", last4, now);
            log.info("Placanje {} odbijeno: nedovoljno sredstava (*{})", id, last4);
            return session;
        }
        session.approve(account, last4, debit, account.getCurrency(), UUID.randomUUID().toString(), now);
        log.info("Placanje {} odobreno, rezervisano {} {} (*{})", id, debit, account.getCurrency(), last4);
        return session;
    }

    private Map<String, String> validate(String pan, CardForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!CardValidation.isValidLuhn(pan)) {
            errors.put("pan", "Broj kartice nije ispravan");
        } else if (CardBrand.detect(pan).isEmpty()) {
            errors.put("pan", "Ova vrsta kartice nije podrzana");
        }
        if (form.holderName() == null || form.holderName().isBlank()) {
            errors.put("holderName", "Unesite ime vlasnika kartice");
        }
        Optional<YearMonth> expiry = CardValidation.parseExpiry(form.expiry());
        if (expiry.isEmpty()) {
            errors.put("expiry", "Datum mora biti u formatu MM/YY");
        } else if (CardValidation.isExpired(expiry.get(), YearMonth.now(clock.withZone(ZoneOffset.UTC)))) {
            errors.put("expiry", "Kartica je istekla");
        }
        if (!CardValidation.isValidCvv(form.cvv())) {
            errors.put("cvv", "Kontrolni broj mora imati 3 cifre");
        }
        return errors;
    }

    private String returnUrl(PaymentSession session) {
        return UriComponentsBuilder.fromUriString(props.psp().returnUrl())
                .queryParam("paymentId", session.getId())
                .toUriString();
    }

    private static String message(PaymentSession session) {
        return switch (session.getStatus()) {
            case SUCCESS -> "Placanje je uspesno izvrseno.";
            case FAILED -> "Placanje je odbijeno: " + session.getFailureReason() + ".";
            default -> "Doslo je do greske pri obradi placanja.";
        };
    }
}
