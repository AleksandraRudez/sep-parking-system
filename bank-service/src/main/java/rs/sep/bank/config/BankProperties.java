package rs.sep.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * @param paymentUrlTtl  koliko dugo vazi forma za placanje
 * @param cardHmacKey    tajni kljuc kojim se racuna HMAC PAN-a i CVV-a
 * @param exchangeRates  kurs banke: vrednost jedne jedinice valute u RSD
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(String publicUrl, Duration paymentUrlTtl, String cardHmacKey, Psp psp,
                             Map<String, BigDecimal> exchangeRates) {

    /**
     * @param callbackUrl PSP endpoint na koji banka javlja ishod placanja
     * @param returnUrl   PSP stranica na koju se kupac vraca posle placanja
     */
    public record Psp(String hmacSecret, String callbackUrl, String returnUrl) {
    }
}
