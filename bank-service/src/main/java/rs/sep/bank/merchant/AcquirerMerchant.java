package rs.sep.bank.merchant;

import jakarta.persistence.*;
import rs.sep.bank.account.Account;

/** Prodavac sa kojim banka ima ugovor o prihvatanju kartica. */
@Entity
@Table(name = "acquirer_merchant")
public class AcquirerMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    protected AcquirerMerchant() {
    }

    public AcquirerMerchant(String merchantId, String name, Account account) {
        this.merchantId = merchantId;
        this.name = name;
        this.account = account;
    }

    public Long getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public Account getAccount() { return account; }
}
