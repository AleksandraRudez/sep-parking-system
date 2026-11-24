package rs.sep.psp.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

/** Zahtev super admina za registraciju novog klijenta PSP-a. */
public record RegisterMerchantRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @URL String successUrl,
        @NotBlank @URL String failedUrl,
        @NotBlank @URL String errorUrl,
        @NotBlank @URL String webhookUrl,
        @NotEmpty(message = "klijent mora imati bar jedan nacin placanja") Set<String> paymentMethods) {
}
