package rs.sep.psp.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UpdateMerchantUrlsRequest(
        @NotBlank @URL String successUrl,
        @NotBlank @URL String failedUrl,
        @NotBlank @URL String errorUrl,
        @NotBlank @URL String webhookUrl) {
}
