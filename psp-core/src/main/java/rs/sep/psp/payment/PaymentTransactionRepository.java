package rs.sep.psp.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByMerchantIdAndMerchantOrderId(UUID merchantId, String merchantOrderId);

    Optional<PaymentTransaction> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<PaymentTransaction> findByPaymentMethodAndExternalPaymentId(String paymentMethod, String externalPaymentId);

    /** Zakljucava transakciju da dva istovremena klika na "Plati" ne bi pokrenula placanje dvaput. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymentTransaction t join fetch t.merchant where t.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(UUID id);

    @Query("select t from PaymentTransaction t join fetch t.merchant where t.id = :id")
    Optional<PaymentTransaction> findWithMerchant(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymentTransaction t where t.paymentMethod = :paymentMethod and t.externalPaymentId = :externalPaymentId")
    Optional<PaymentTransaction> findByExternalIdForUpdate(String paymentMethod, String externalPaymentId);

    @Query(value = "select lpad(cast(nextval('stan_seq') as varchar), 6, '0')", nativeQuery = true)
    String nextStan();
}
