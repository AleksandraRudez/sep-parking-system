package rs.sep.bank.card;

import jakarta.persistence.*;
import rs.sep.bank.account.Account;

import java.time.YearMonth;

@Entity
@Table(name = "bank_card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String panHmac;

    @Column(nullable = false)
    private String panLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardBrand brand;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private short expiryMonth;

    @Column(nullable = false)
    private short expiryYear;

    @Column(nullable = false)
    private String cvvHmac;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    protected Card() {
    }

    public Card(String panHmac, String panLast4, CardBrand brand, String holderName, YearMonth expiry,
                String cvvHmac, Account account) {
        this.panHmac = panHmac;
        this.panLast4 = panLast4;
        this.brand = brand;
        this.holderName = holderName;
        this.expiryMonth = (short) expiry.getMonthValue();
        this.expiryYear = (short) expiry.getYear();
        this.cvvHmac = cvvHmac;
        this.account = account;
    }

    public YearMonth getExpiry() {
        return YearMonth.of(expiryYear, expiryMonth);
    }

    public Long getId() { return id; }
    public String getPanHmac() { return panHmac; }
    public String getPanLast4() { return panLast4; }
    public CardBrand getBrand() { return brand; }
    public String getHolderName() { return holderName; }
    public String getCvvHmac() { return cvvHmac; }
    public Account getAccount() { return account; }
}
