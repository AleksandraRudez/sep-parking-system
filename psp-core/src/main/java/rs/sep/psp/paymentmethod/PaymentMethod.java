package rs.sep.psp.paymentmethod;

import jakarta.persistence.*;

/** Nacin placanja koji PSP podrzava; svaki odgovara jednom plugin-u. */
@Entity
@Table(name = "payment_method")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String serviceUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    protected PaymentMethod() {
    }

    public PaymentMethod(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getServiceUrl() { return serviceUrl; }
    public boolean isEnabled() { return enabled; }
}
