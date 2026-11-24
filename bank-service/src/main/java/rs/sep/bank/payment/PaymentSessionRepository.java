package rs.sep.bank.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentSessionRepository extends JpaRepository<PaymentSession, UUID> {

    @Query("select s from PaymentSession s join fetch s.merchant where s.id = :id")
    Optional<PaymentSession> findWithMerchant(UUID id);

    @Query("""
            select s from PaymentSession s join fetch s.merchant m
             where m.merchantId = :merchantId and s.stan = :stan and s.pspTimestamp = :pspTimestamp
            """)
    Optional<PaymentSession> findByTrace(String merchantId, String stan, Instant pspTimestamp);

    /**
     * Zauzima formu za jedan pokusaj placanja. Uspeva samo jednom, i to samo dok forma vazi,
     * pa ni dvostruki klik ni ponovno slanje forme ne mogu da naplate dvaput.
     *
     * @return 1 ako je forma zauzeta, 0 ako je istekla ili vec iskoriscena
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentSession s set s.status = rs.sep.bank.payment.PaymentSessionStatus.PROCESSING, s.updatedAt = :now
             where s.id = :id and s.status = rs.sep.bank.payment.PaymentSessionStatus.CREATED and s.expiresAt > :now
            """)
    int claim(UUID id, Instant now);
}
