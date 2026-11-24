package rs.sep.psp.plugin;

/** @param externalPaymentId ID placanja kod spoljnog servisa (npr. PAYMENT_ID banke) */
public record PaymentStart(String externalPaymentId, String redirectUrl) {
}
