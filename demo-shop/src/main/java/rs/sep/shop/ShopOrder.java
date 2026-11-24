package rs.sep.shop;

import java.math.BigDecimal;
import java.time.Instant;

/** Narudzbina u test prodavnici. Status menja samo potvrda od PSP-a (webhook), ne redirect kupca. */
public class ShopOrder {

    private final String orderId;
    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final Instant createdAt;
    private volatile String paymentId;
    private volatile String status = "NOVA";
    private volatile String globalTransactionId;

    public ShopOrder(String orderId, String description, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public void paymentStarted(String paymentId) {
        this.paymentId = paymentId;
        this.status = "CEKA_PLACANJE";
    }

    public void confirmed(String status, String globalTransactionId) {
        this.status = status;
        this.globalTransactionId = globalTransactionId;
    }

    public String getOrderId() { return orderId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPaymentId() { return paymentId; }
    public String getStatus() { return status; }
    public String getGlobalTransactionId() { return globalTransactionId; }
}
