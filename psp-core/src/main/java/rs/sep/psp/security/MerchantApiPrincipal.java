package rs.sep.psp.security;

import java.util.UUID;

/** Prodavac (sistem, ne covek) autentifikovan preko MERCHANT_ID + API_KEY. */
public record MerchantApiPrincipal(UUID id, String merchantId) {
}
