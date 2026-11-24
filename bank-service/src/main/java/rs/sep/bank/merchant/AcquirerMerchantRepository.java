package rs.sep.bank.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcquirerMerchantRepository extends JpaRepository<AcquirerMerchant, Long> {

    Optional<AcquirerMerchant> findByMerchantId(String merchantId);
}
