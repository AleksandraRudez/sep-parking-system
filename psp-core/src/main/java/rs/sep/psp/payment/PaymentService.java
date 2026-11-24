package rs.sep.psp.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.merchant.MerchantUrls;
import rs.sep.psp.payment.dto.InitPaymentRequest;
import rs.sep.psp.payment.dto.InitPaymentResponse;
import rs.sep.psp.payment.dto.PaymentStatusResponse;
import rs.sep.psp.paymentmethod.SubscriptionService;
import rs.sep.psp.plugin.PaymentOutcome;
import rs.sep.psp.plugin.PaymentPlugin;
import rs.sep.psp.plugin.PaymentStart;
import rs.sep.psp.plugin.PluginRegistry;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** Zivotni ciklus PSP transakcije, zajednicki za sve nacine placanja. */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentTransactionRepository transactions;
    private final MerchantRepository merchants;
    private final SubscriptionService subscriptions;
    private final PluginRegistry plugins;
    private final PaymentProperties props;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PaymentService(PaymentTransactionRepository transactions, MerchantRepository merchants,
                          SubscriptionService subscriptions, PluginRegistry plugins, PaymentProperties props,
                          ApplicationEventPublisher events, TransactionTemplate tx, Clock clock) {
        this.transactions = transactions;
        this.merchants = merchants;
        this.subscriptions = subscriptions;
        this.plugins = plugins;
        this.props = props;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    /**
     * Prodavac inicijalizuje transakciju. Ponovljen zahtev sa istim MERCHANT_ORDER_ID-jem vraca postojecu
     * transakciju (npr. kada prodavac nije dobio odgovor pa salje ponovo), a ne pravi novu.
     */
    public InitPaymentResponse initialize(UUID merchantId, InitPaymentRequest req) {
        Instant now = clock.instant();
        if (!props.currencies().contains(req.currency())) {
            throw new BusinessException(BAD_REQUEST, "Valuta " + req.currency() + " nije podrzana");
        }
        if (Duration.between(req.merchantTimestamp(), now).abs().compareTo(props.maxClockSkew()) > 0) {
            throw new BusinessException(BAD_REQUEST, "MERCHANT_TIMESTAMP previse odstupa od vremena PSP-a");
        }

        Optional<PaymentTransaction> existing = transactions.findByMerchantIdAndMerchantOrderId(merchantId,
                req.merchantOrderId());
        if (existing.isPresent()) {
            return sameOrConflict(existing.get(), req);
        }
        try {
            PaymentTransaction created = tx.execute(status -> transactions.saveAndFlush(new PaymentTransaction(
                    merchants.getReferenceById(merchantId), req.merchantOrderId(), req.merchantTimestamp(),
                    req.amount(), req.currency(), now.plus(props.checkoutTtl()))));
            log.info("Inicijalizovana transakcija {} (prodavac {}, nalog {})", created.getId(), merchantId,
                    req.merchantOrderId());
            return response(created);
        } catch (DataIntegrityViolationException e) {
            // Dva istovremena zahteva sa istim nalogom: drugi zahtev dobija transakciju koju je napravio prvi
            return sameOrConflict(transactions.findByMerchantIdAndMerchantOrderId(merchantId, req.merchantOrderId())
                    .orElseThrow(() -> e), req);
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse statusForMerchant(UUID merchantId, UUID paymentId) {
        return transactions.findByIdAndMerchantId(paymentId, merchantId)
                .map(PaymentStatusResponse::of)
                .orElseThrow(() -> BusinessException.notFound("Transakcija ne postoji"));
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse statusForMerchantByOrder(UUID merchantId, String merchantOrderId) {
        return transactions.findByMerchantIdAndMerchantOrderId(merchantId, merchantOrderId)
                .map(PaymentStatusResponse::of)
                .orElseThrow(() -> BusinessException.notFound("Transakcija ne postoji"));
    }

    /** Podaci za stranicu izbora nacina placanja; prikazuju se samo metode koje je prodavac aktivirao. */
    @Transactional
    public CheckoutView checkout(UUID paymentId) {
        PaymentTransaction t = transactions.findWithMerchant(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Placanje ne postoji"));
        if (t.isExpired(clock.instant())) {
            t.expire();
            events.publishEvent(new PaymentCompletedEvent(t.getId()));
        }
        Merchant m = t.getMerchant();
        var methods = subscriptions.activeMethods(m.getId()).stream()
                .filter(pm -> plugins.supports(pm.getCode()))
                .toList();
        return new CheckoutView(t.getId(), m.getName(), t.getMerchantOrderId(), t.getAmount(), t.getCurrency(),
                t.getStatus(), methods);
    }

    /**
     * Kupac je izabrao nacin placanja. Transakcija se prvo (pod zakljucavanjem) prebacuje u PENDING, pa tek
     * onda poziva spoljni servis, tako da dvostruki klik ne moze da pokrene dva placanja.
     *
     * @return adresa na koju se kupac preusmerava
     */
    public URI startPayment(UUID paymentId, String code) {
        PaymentPlugin plugin = plugins.find(code)
                .orElseThrow(() -> new BusinessException(BAD_REQUEST, "Nacin placanja nije podrzan"));
        PaymentTransaction t = tx.execute(status -> {
            PaymentTransaction locked = transactions.findByIdForUpdate(paymentId)
                    .orElseThrow(() -> BusinessException.notFound("Placanje ne postoji"));
            Instant now = clock.instant();
            if (locked.isExpired(now)) {
                locked.expire();
                events.publishEvent(new PaymentCompletedEvent(locked.getId()));
                return locked;
            }
            if (locked.getStatus() != TransactionStatus.CREATED) {
                throw BusinessException.conflict("Placanje je vec zapoceto");
            }
            boolean active = subscriptions.activeMethods(locked.getMerchant().getId()).stream()
                    .anyMatch(pm -> pm.getCode().equals(code));
            if (!active) {
                throw new BusinessException(BAD_REQUEST, "Prodavac ne prihvata ovaj nacin placanja");
            }
            locked.startPayment(code, transactions.nextStan(), now);
            return locked;
        });
        if (t.getStatus() != TransactionStatus.PENDING) {
            return resultUrl(t);
        }

        try {
            PaymentStart start = plugin.start(t);
            tx.executeWithoutResult(status -> transactions.findById(paymentId)
                    .orElseThrow()
                    .assignExternalPaymentId(start.externalPaymentId()));
            log.info("Transakcija {} preusmerena na {} (STAN {})", paymentId, code, t.getStan());
            return URI.create(start.redirectUrl());
        } catch (RuntimeException e) {
            log.error("Pokretanje placanja {} za transakciju {} nije uspelo", code, paymentId, e);
            PaymentTransaction failed = completeAndPublish(paymentId,
                    new PaymentOutcome(TransactionStatus.ERROR, null, null));
            return resultUrl(failed);
        }
    }

    /** Spoljni servis (npr. banka) javlja ishod placanja. Ponovljeno obavestenje se ignorise. */
    public void complete(String code, String externalPaymentId, String stan, PaymentOutcome outcome) {
        tx.executeWithoutResult(status -> {
            PaymentTransaction t = transactions.findByExternalIdForUpdate(code, externalPaymentId)
                    .orElseThrow(() -> BusinessException.notFound("Transakcija ne postoji"));
            if (stan != null && !stan.equals(t.getStan())) {
                throw new BusinessException(BAD_REQUEST, "STAN se ne poklapa sa transakcijom");
            }
            if (t.complete(outcome.status(), outcome.globalTransactionId(), outcome.acquirerTimestamp())) {
                log.info("Transakcija {} zavrsena: {}", t.getId(), outcome.status());
                events.publishEvent(new PaymentCompletedEvent(t.getId()));
            }
        });
    }

    /**
     * Kupac se vratio sa stranice servisa za placanje. Ako obavestenje servisa jos nije stiglo, PSP sam proverava
     * status, pa ne zavisi samo od callback-a.
     *
     * @return adresa stranice prodavca, ili prazno ako je placanje jos u obradi
     */
    public Optional<URI> handleReturn(String code, String externalPaymentId) {
        UUID id = transactions.findByPaymentMethodAndExternalPaymentId(code, externalPaymentId)
                .map(PaymentTransaction::getId)
                .orElseThrow(() -> BusinessException.notFound("Placanje ne postoji"));
        // Prodavac se ucitava zajedno sa transakcijom jer su njegovi URL-ovi potrebni za preusmeravanje
        PaymentTransaction t = transactions.findWithMerchant(id).orElseThrow();
        if (t.getStatus() == TransactionStatus.PENDING) {
            PaymentTransaction pending = t;
            try {
                plugins.find(code).flatMap(p -> p.checkStatus(pending))
                        .ifPresent(outcome -> complete(code, externalPaymentId, null, outcome));
            } catch (RuntimeException e) {
                log.warn("Provera statusa transakcije {} nije uspela: {}", id, e.getMessage());
            }
            t = transactions.findWithMerchant(id).orElseThrow();
        }
        return t.getStatus() == TransactionStatus.PENDING ? Optional.empty() : Optional.of(resultUrl(t));
    }

    @Transactional(readOnly = true)
    public URI resultUrl(UUID paymentId) {
        return resultUrl(transactions.findWithMerchant(paymentId)
                .orElseThrow(() -> BusinessException.notFound("Placanje ne postoji")));
    }

    private PaymentTransaction completeAndPublish(UUID paymentId, PaymentOutcome outcome) {
        return tx.execute(status -> {
            PaymentTransaction t = transactions.findByIdForUpdate(paymentId).orElseThrow();
            if (t.complete(outcome.status(), outcome.globalTransactionId(), outcome.acquirerTimestamp())) {
                events.publishEvent(new PaymentCompletedEvent(t.getId()));
            }
            return t;
        });
    }

    /** SUCCESS_URL, FAILED_URL ili ERROR_URL prodavca, prema ishodu transakcije. */
    private URI resultUrl(PaymentTransaction t) {
        MerchantUrls urls = t.getMerchant().getUrls();
        String base = switch (t.getStatus()) {
            case SUCCESS -> urls.successUrl();
            case FAILED, EXPIRED -> urls.failedUrl();
            default -> urls.errorUrl();
        };
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("merchantOrderId", t.getMerchantOrderId())
                .queryParam("paymentId", t.getId())
                .queryParam("status", t.getStatus())
                .build()
                .toUri();
    }

    private InitPaymentResponse sameOrConflict(PaymentTransaction t, InitPaymentRequest req) {
        if (t.getAmount().compareTo(req.amount()) != 0 || !t.getCurrency().equals(req.currency())) {
            throw BusinessException.conflict("Nalog " + req.merchantOrderId() + " vec postoji sa drugim iznosom");
        }
        return response(t);
    }

    private InitPaymentResponse response(PaymentTransaction t) {
        return new InitPaymentResponse(t.getId(), props.publicUrl() + "/checkout/" + t.getId(), t.getStatus());
    }
}
