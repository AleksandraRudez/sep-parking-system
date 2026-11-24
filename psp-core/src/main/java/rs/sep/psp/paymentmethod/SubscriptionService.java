package rs.sep.psp.paymentmethod;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.paymentmethod.dto.SubscriptionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Upravljanje pretplatama klijenta na nacine placanja. Invarijanta: klijent uvek ima bar jedan aktivan nacin. */
@Service
public class SubscriptionService {

    private final MerchantRepository merchants;
    private final MerchantSubscriptionRepository subscriptions;
    private final PaymentMethodRepository paymentMethods;

    public SubscriptionService(MerchantRepository merchants, MerchantSubscriptionRepository subscriptions,
                               PaymentMethodRepository paymentMethods) {
        this.merchants = merchants;
        this.subscriptions = subscriptions;
        this.paymentMethods = paymentMethods;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listForMerchant(String keycloakUserId) {
        Merchant merchant = merchants.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> BusinessException.notFound("Klijent ne postoji"));
        Map<String, Boolean> activeByCode = subscriptions.findByMerchantId(merchant.getId()).stream()
                .collect(Collectors.toMap(s -> s.getPaymentMethod().getCode(), MerchantSubscription::isActive));
        return paymentMethods.findByEnabledTrueOrderByIdAsc().stream()
                .map(pm -> new SubscriptionResponse(pm.getCode(), pm.getName(),
                        activeByCode.getOrDefault(pm.getCode(), false)))
                .toList();
    }

    @Transactional
    public void activate(String keycloakUserId, String code) {
        Merchant merchant = lockMerchant(keycloakUserId);
        PaymentMethod method = requireEnabled(code);
        subscriptions.findByMerchantIdAndPaymentMethodCode(merchant.getId(), method.getCode())
                .ifPresentOrElse(MerchantSubscription::activate,
                        () -> subscriptions.save(new MerchantSubscription(merchant, method)));
    }

    @Transactional
    public void deactivate(String keycloakUserId, String code) {
        Merchant merchant = lockMerchant(keycloakUserId);
        MerchantSubscription subscription = subscriptions
                .findByMerchantIdAndPaymentMethodCode(merchant.getId(), normalize(code))
                .filter(MerchantSubscription::isActive)
                .orElseThrow(() -> BusinessException.notFound("Nacin placanja nije aktivan za ovog klijenta"));
        if (subscriptions.countByMerchantIdAndActiveTrue(merchant.getId()) <= 1) {
            throw BusinessException.conflict("Mora ostati bar jedan aktivan nacin placanja");
        }
        subscription.deactivate();
    }

    /** Poziva se pri registraciji klijenta, unutar iste transakcije. */
    public void subscribeAll(Merchant merchant, Collection<String> codes) {
        codes.forEach(code -> subscriptions.save(new MerchantSubscription(merchant, requireEnabled(code))));
    }

    @Transactional(readOnly = true)
    public List<String> activeCodes(UUID merchantId) {
        return subscriptions.findByMerchantId(merchantId).stream()
                .filter(MerchantSubscription::isActive)
                .map(s -> s.getPaymentMethod().getCode())
                .sorted()
                .toList();
    }

    public PaymentMethod requireEnabled(String code) {
        return paymentMethods.findByCode(normalize(code))
                .filter(PaymentMethod::isEnabled)
                .orElseThrow(() -> BusinessException.notFound("Nepoznat nacin placanja: " + code));
    }

    private Merchant lockMerchant(String keycloakUserId) {
        return merchants.findByKeycloakUserIdForUpdate(keycloakUserId)
                .orElseThrow(() -> BusinessException.notFound("Klijent ne postoji"));
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase();
    }
}
