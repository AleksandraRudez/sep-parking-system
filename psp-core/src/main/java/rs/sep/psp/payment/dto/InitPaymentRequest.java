package rs.sep.psp.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Zahtev prodavca za inicijalizaciju transakcije (tabela 1 u specifikaciji).
 * MERCHANT_ID i API_KEY se salju u zaglavljima, a SUCCESS/FAILED/ERROR URL-ovi su unapred podeseni na PSP-u.
 */
public record InitPaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "mora biti ISO 4217 kod valute (npr. RSD)") String currency,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String merchantOrderId,
        @NotNull Instant merchantTimestamp) {
}
