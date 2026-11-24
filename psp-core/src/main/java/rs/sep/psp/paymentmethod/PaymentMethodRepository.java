package rs.sep.psp.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByCode(String code);

    List<PaymentMethod> findByEnabledTrueOrderByIdAsc();
}
