package rs.sep.psp.paymentmethod.dto;

/** Jedan nacin placanja iz ugla klijenta: da li je trenutno aktivan za njega. */
public record SubscriptionResponse(String code, String name, boolean active) {
}
