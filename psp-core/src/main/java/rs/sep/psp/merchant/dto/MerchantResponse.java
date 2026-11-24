package rs.sep.psp.merchant.dto;

import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantUrls;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String merchantId,
        String name,
        String email,
        MerchantUrls urls,
        boolean active,
        Instant createdAt,
        List<String> activePaymentMethods) {

    public static MerchantResponse of(Merchant m, List<String> activePaymentMethods) {
        return new MerchantResponse(m.getId(), m.getMerchantId(), m.getName(), m.getEmail(), m.getUrls(),
                m.isActive(), m.getCreatedAt(), activePaymentMethods);
    }
}
