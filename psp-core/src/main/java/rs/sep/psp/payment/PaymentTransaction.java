package rs.sep.psp.payment;

import jakarta.persistence.*;
import rs.sep.psp.merchant.Merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Interna PSP transakcija: jedna kupovina kod jednog prodavca. */
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false)
    private String merchantOrderId;

    @Column(nullable = false)
    private Instant merchantTimestamp;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    private String paymentMethod;
    private String stan;
    private Instant pspTimestamp;

    /** ID placanja kod spoljnog servisa (npr. PAYMENT_ID banke). */
    private String externalPaymentId;

    private String globalTransactionId;
    private Instant acquirerTimestamp;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(Merchant merchant, String merchantOrderId, Instant merchantTimestamp,
                              BigDecimal amount, String currency, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.merchantOrderId = merchantOrderId;
        this.merchantTimestamp = merchantTimestamp;
        this.amount = amount;
        this.currency = currency;
        this.status = TransactionStatus.CREATED;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isExpired(Instant now) {
        return status == TransactionStatus.CREATED && now.isAfter(expiresAt);
    }

    /** Kupac je izabrao nacin placanja; od ovog trenutka transakcija ne moze ponovo da se pokrene. */
    public void startPayment(String paymentMethod, String stan, Instant pspTimestamp) {
        requireStatus(TransactionStatus.CREATED);
        this.paymentMethod = paymentMethod;
        this.stan = stan;
        this.pspTimestamp = pspTimestamp;
        changeStatus(TransactionStatus.PENDING);
    }

    public void assignExternalPaymentId(String externalPaymentId) {
        this.externalPaymentId = externalPaymentId;
        this.updatedAt = Instant.now();
    }

    /**
     * Upisuje konacan ishod. Vraca false ako je ishod vec ranije upisan (npr. banka je poslala
     * callback, a PSP je u medjuvremenu i sam proverio status), pa se obavestenje ne salje dvaput.
     */
    public boolean complete(TransactionStatus result, String globalTransactionId, Instant acquirerTimestamp) {
        if (!result.isFinal()) {
            throw new IllegalArgumentException("Ishod mora biti konacan status: " + result);
        }
        if (status.isFinal()) {
            return false;
        }
        this.globalTransactionId = globalTransactionId;
        this.acquirerTimestamp = acquirerTimestamp;
        changeStatus(result);
        return true;
    }

    public void expire() {
        requireStatus(TransactionStatus.CREATED);
        changeStatus(TransactionStatus.EXPIRED);
    }

    private void requireStatus(TransactionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Transakcija " + id + " je u statusu " + status + ", ocekivan " + expected);
        }
    }

    private void changeStatus(TransactionStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Merchant getMerchant() { return merchant; }
    public String getMerchantOrderId() { return merchantOrderId; }
    public Instant getMerchantTimestamp() { return merchantTimestamp; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStan() { return stan; }
    public Instant getPspTimestamp() { return pspTimestamp; }
    public String getExternalPaymentId() { return externalPaymentId; }
    public String getGlobalTransactionId() { return globalTransactionId; }
    public Instant getAcquirerTimestamp() { return acquirerTimestamp; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
