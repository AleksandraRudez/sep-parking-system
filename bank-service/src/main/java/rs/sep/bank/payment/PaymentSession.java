package rs.sep.bank.payment;

import jakarta.persistence.*;
import rs.sep.bank.account.Account;
import rs.sep.bank.merchant.AcquirerMerchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Forma za placanje (PAYMENT_ID / PAYMENT_URL), vezana za tacno jednu transakciju PSP-a. */
@Entity
@Table(name = "payment_session")
public class PaymentSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private AcquirerMerchant merchant;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String stan;

    @Column(nullable = false)
    private Instant pspTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSessionStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id")
    private Account payerAccount;

    private String cardLast4;
    private BigDecimal debitedAmount;
    private String debitedCurrency;
    private String failureReason;
    private String globalTransactionId;
    private Instant acquirerTimestamp;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PaymentSession() {
    }

    public PaymentSession(AcquirerMerchant merchant, BigDecimal amount, String currency, String stan,
                          Instant pspTimestamp, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.stan = stan;
        this.pspTimestamp = pspTimestamp;
        this.status = PaymentSessionStatus.CREATED;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isUsable(Instant now) {
        return status == PaymentSessionStatus.CREATED && now.isBefore(expiresAt);
    }

    public void approve(Account payer, String cardLast4, BigDecimal debitedAmount, String debitedCurrency,
                        String globalTransactionId, Instant now) {
        this.payerAccount = payer;
        this.cardLast4 = cardLast4;
        this.debitedAmount = debitedAmount;
        this.debitedCurrency = debitedCurrency;
        this.globalTransactionId = globalTransactionId;
        this.acquirerTimestamp = now;
        finish(PaymentSessionStatus.SUCCESS, now);
    }

    public void decline(String reason, String cardLast4, Instant now) {
        this.failureReason = reason;
        this.cardLast4 = cardLast4;
        this.acquirerTimestamp = now;
        finish(PaymentSessionStatus.FAILED, now);
    }

    public void fail(String reason, Instant now) {
        this.failureReason = reason;
        this.acquirerTimestamp = now;
        finish(PaymentSessionStatus.ERROR, now);
    }

    public void expire(Instant now) {
        finish(PaymentSessionStatus.EXPIRED, now);
    }

    private void finish(PaymentSessionStatus result, Instant now) {
        this.status = result;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public AcquirerMerchant getMerchant() { return merchant; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStan() { return stan; }
    public Instant getPspTimestamp() { return pspTimestamp; }
    public PaymentSessionStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getCardLast4() { return cardLast4; }
    public BigDecimal getDebitedAmount() { return debitedAmount; }
    public String getDebitedCurrency() { return debitedCurrency; }
    public String getFailureReason() { return failureReason; }
    public String getGlobalTransactionId() { return globalTransactionId; }
    public Instant getAcquirerTimestamp() { return acquirerTimestamp; }
}
