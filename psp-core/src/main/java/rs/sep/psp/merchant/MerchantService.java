package rs.sep.psp.merchant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.common.SecretGenerator;
import rs.sep.psp.keycloak.KeycloakAdminClient;
import rs.sep.psp.merchant.dto.ApiKeyResponse;
import rs.sep.psp.merchant.dto.MerchantCreatedResponse;
import rs.sep.psp.merchant.dto.MerchantResponse;
import rs.sep.psp.merchant.dto.RegisterMerchantRequest;
import rs.sep.psp.merchant.dto.UpdateMerchantUrlsRequest;
import rs.sep.psp.paymentmethod.SubscriptionService;

import java.util.List;
import java.util.Locale;

@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);
    private static final String MERCHANT_ROLE = "MERCHANT";

    private final MerchantRepository merchants;
    private final SubscriptionService subscriptionService;
    private final KeycloakAdminClient keycloak;
    private final SecretGenerator secrets;
    private final TransactionTemplate tx;

    public MerchantService(MerchantRepository merchants, SubscriptionService subscriptionService,
                           KeycloakAdminClient keycloak, SecretGenerator secrets, TransactionTemplate tx) {
        this.merchants = merchants;
        this.subscriptionService = subscriptionService;
        this.keycloak = keycloak;
        this.secrets = secrets;
        this.tx = tx;
    }

    /**
     * Super admin registruje klijenta: kreira se Keycloak nalog sa privremenom lozinkom (obavezna promena
     * pri prvoj prijavi), MERCHANT_ID, API kljuc i pocetne pretplate. Ako upis u bazu ne uspe,
     * Keycloak nalog se brise da ne bi ostao "siroce".
     */
    public MerchantCreatedResponse register(RegisterMerchantRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (merchants.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("Klijent sa ovim email-om vec postoji");
        }
        List<String> codes = req.paymentMethods().stream()
                .map(subscriptionService::requireEnabled)
                .map(pm -> pm.getCode())
                .distinct()
                .sorted()
                .toList();

        String temporaryPassword = secrets.newTemporaryPassword();
        String apiKey = secrets.newApiKey();

        String keycloakUserId;
        try {
            keycloakUserId = keycloak.createUserWithTemporaryPassword(email, req.name(), temporaryPassword,
                    MERCHANT_ROLE);
        } catch (HttpClientErrorException.Conflict e) {
            throw BusinessException.conflict("Nalog sa ovim email-om vec postoji u Keycloak-u");
        }

        try {
            Merchant saved = tx.execute(status -> {
                Merchant merchant = merchants.save(new Merchant(secrets.newMerchantId(), req.name().trim(), email,
                        keycloakUserId, secrets.sha256(apiKey),
                        new MerchantUrls(req.successUrl(), req.failedUrl(), req.errorUrl(), req.webhookUrl())));
                subscriptionService.subscribeAll(merchant, codes);
                return merchant;
            });
            log.info("Registrovan klijent {} ({})", saved.getMerchantId(), saved.getId());
            return new MerchantCreatedResponse(MerchantResponse.of(saved, codes), temporaryPassword, apiKey);
        } catch (RuntimeException e) {
            deleteKeycloakUserQuietly(keycloakUserId);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> findAll() {
        return merchants.findAllByOrderByCreatedAtDesc().stream()
                .map(m -> MerchantResponse.of(m, subscriptionService.activeCodes(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantResponse me(String keycloakUserId) {
        Merchant merchant = byKeycloakUser(keycloakUserId);
        return MerchantResponse.of(merchant, subscriptionService.activeCodes(merchant.getId()));
    }

    @Transactional
    public MerchantResponse updateUrls(String keycloakUserId, UpdateMerchantUrlsRequest req) {
        Merchant merchant = byKeycloakUser(keycloakUserId);
        merchant.changeUrls(new MerchantUrls(req.successUrl(), req.failedUrl(), req.errorUrl(), req.webhookUrl()));
        return MerchantResponse.of(merchant, subscriptionService.activeCodes(merchant.getId()));
    }

    /** Generise novi API kljuc; stari odmah prestaje da vazi. */
    @Transactional
    public ApiKeyResponse rotateApiKey(String keycloakUserId) {
        Merchant merchant = byKeycloakUser(keycloakUserId);
        String apiKey = secrets.newApiKey();
        merchant.changeApiKeyHash(secrets.sha256(apiKey));
        log.info("Rotiran API kljuc za klijenta {}", merchant.getMerchantId());
        return new ApiKeyResponse(merchant.getMerchantId(), apiKey);
    }

    private Merchant byKeycloakUser(String keycloakUserId) {
        return merchants.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> BusinessException.notFound("Klijent ne postoji"));
    }

    private void deleteKeycloakUserQuietly(String keycloakUserId) {
        try {
            keycloak.deleteUser(keycloakUserId);
        } catch (RuntimeException e) {
            log.error("Neuspelo brisanje Keycloak korisnika {} posle greske pri registraciji", keycloakUserId, e);
        }
    }
}
