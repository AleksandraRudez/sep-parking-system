package rs.sep.psp.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantSubscriptionRepository extends JpaRepository<MerchantSubscription, Long> {

    List<MerchantSubscription> findByMerchantId(UUID merchantId);

    Optional<MerchantSubscription> findByMerchantIdAndPaymentMethodCode(UUID merchantId, String code);

    long countByMerchantIdAndActiveTrue(UUID merchantId);
}
