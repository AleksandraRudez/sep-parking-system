package rs.sep.psp.merchant.dto;

/**
 * Odgovor na registraciju klijenta. Privremena lozinka i API kljuc se prikazuju samo ovde,
 * jednom, jer se na PSP-u ne cuvaju u citljivom obliku.
 */
public record MerchantCreatedResponse(MerchantResponse merchant, String temporaryPassword, String apiKey) {
}
