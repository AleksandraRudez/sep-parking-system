package rs.sep.psp.paymentmethod;

import jakarta.persistence.*;
import rs.sep.psp.merchant.Merchant;

import java.time.Instant;

/** Pretplata klijenta na jedan nacin placanja. Deaktivacija ne brise red, vec gasi {@code active}. */
@Entity
@Table(name = "merchant_subscription")
public class MerchantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MerchantSubscription() {
    }

    public MerchantSubscription(Merchant merchant, PaymentMethod paymentMethod) {
        this.merchant = merchant;
        this.paymentMethod = paymentMethod;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Merchant getMerchant() { return merchant; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public boolean isActive() { return active; }
}
