package rs.sep.psp.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/**
 * @param publicUrl      javna adresa PSP-a (za linkove ka stranici za izbor nacina placanja)
 * @param currencies     valute koje PSP prihvata
 * @param checkoutTtl    koliko dugo kupac ima da izabere nacin placanja
 * @param maxClockSkew   najvece dozvoljeno odstupanje MERCHANT_TIMESTAMP-a od vremena PSP-a
 */
@ConfigurationProperties(prefix = "psp.payment")
public record PaymentProperties(String publicUrl, Set<String> currencies, Duration checkoutTtl, Duration maxClockSkew) {
}
