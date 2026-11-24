package rs.sep.psp.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcquirerMerchantRequest(@NotBlank @Size(max = 32) String acquirerMerchantId) {
}
