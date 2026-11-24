package rs.sep.bank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.sep.bank.account.Account;
import rs.sep.bank.account.AccountRepository;
import rs.sep.bank.card.Card;
import rs.sep.bank.card.CardBrand;
import rs.sep.bank.card.CardRepository;
import rs.sep.bank.card.CardSecrets;
import rs.sep.bank.merchant.AcquirerMerchant;
import rs.sep.bank.merchant.AcquirerMerchantRepository;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Test podaci za lokalni razvoj (upisuju se samo u praznu bazu). Brojevi kartica su javni test
 * brojevi koji prolaze Lunovu proveru; spisak je u README.md.
 */
@Component
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final AccountRepository accounts;
    private final CardRepository cards;
    private final AcquirerMerchantRepository merchants;
    private final CardSecrets secrets;

    public DevDataSeeder(AccountRepository accounts, CardRepository cards, AcquirerMerchantRepository merchants,
                         CardSecrets secrets) {
        this.accounts = accounts;
        this.cards = cards;
        this.merchants = merchants;
        this.secrets = secrets;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (cards.count() > 0) {
            return;
        }
        card("4242424242424242", "Petar Petrovic", YearMonth.of(2029, 12), "123",
                account("265-0000000000001-11", "Petar Petrovic", "100000.00"));
        card("5555555555554444", "Ana Anic", YearMonth.of(2028, 6), "456",
                account("265-0000000000002-22", "Ana Anic", "500.00"));
        card("4000056655665556", "Marko Markovic", YearMonth.of(2030, 3), "789",
                account("265-0000000000003-33", "Marko Markovic", "20000.00"));

        merchants.save(new AcquirerMerchant("ACQ-1001", "Parking Servis d.o.o.",
                account("265-0000000001001-44", "Parking Servis d.o.o.", "0.00")));
        merchants.save(new AcquirerMerchant("ACQ-1002", "Demo Shop d.o.o.",
                account("265-0000000001002-55", "Demo Shop d.o.o.", "0.00")));
        log.info("Upisani test racuni, kartice i prodavci");
    }

    private Account account(String number, String owner, String balance) {
        return accounts.save(new Account(number, owner, "RSD", new BigDecimal(balance)));
    }

    private void card(String pan, String holder, YearMonth expiry, String cvv, Account account) {
        cards.save(new Card(secrets.panHmac(pan), pan.substring(pan.length() - 4), CardBrand.detect(pan).orElseThrow(),
                holder, expiry, secrets.cvvHmac(pan, cvv), account));
    }
}
