package rs.sep.psp.merchant;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Klijent PSP-a (npr. parking sistem). */
@Entity
@Table(name = "merchant")
public class Merchant {

    @Id
    private UUID id;

    /** Javni identifikator koji klijent salje uz svaki zahtev (MERCHANT_ID iz tabele 1). */
    @Column(nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String keycloakUserId;

    /** SHA-256 hash API kljuca; sam kljuc se nikad ne cuva. */
    @Column(nullable = false)
    private String apiKeyHash;

    @Embedded
    private MerchantUrls urls;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    protected Merchant() {
    }

    public Merchant(String merchantId, String name, String email, String keycloakUserId,
                    String apiKeyHash, MerchantUrls urls) {
        this.id = UUID.randomUUID();
        this.merchantId = merchantId;
        this.name = name;
        this.email = email;
        this.keycloakUserId = keycloakUserId;
        this.apiKeyHash = apiKeyHash;
        this.urls = urls;
        this.createdAt = Instant.now();
    }

    public void changeUrls(MerchantUrls urls) {
        this.urls = urls;
    }

    public void changeApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public UUID getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getKeycloakUserId() { return keycloakUserId; }
    public String getApiKeyHash() { return apiKeyHash; }
    public MerchantUrls getUrls() { return urls; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
