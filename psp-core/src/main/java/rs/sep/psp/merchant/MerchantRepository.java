package rs.sep.psp.merchant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByKeycloakUserId(String keycloakUserId);

    Optional<Merchant> findByMerchantId(String merchantId);

    /** Zakljucava red klijenta da dve istovremene izmene pretplata ne bi zaobisle pravilo "bar jedna aktivna". */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Merchant m where m.keycloakUserId = :keycloakUserId")
    Optional<Merchant> findByKeycloakUserIdForUpdate(String keycloakUserId);

    boolean existsByEmailIgnoreCase(String email);

    List<Merchant> findAllByOrderByCreatedAtDesc();
}
