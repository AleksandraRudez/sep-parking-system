package rs.sep.bank.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Rezervise sredstva jednim atomskim upitom: uspeva samo ako ima dovoljno raspolozivog novca.
     * Zbog toga ni istovremena placanja sa istog racuna ne mogu da odu u minus.
     *
     * @return 1 ako je rezervacija uspela, 0 ako nema dovoljno sredstava
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Account a
               set a.availableBalance = a.availableBalance - :amount,
                   a.reservedBalance = a.reservedBalance + :amount
             where a.id = :id and a.availableBalance >= :amount
            """)
    int reserve(Long id, BigDecimal amount);
}
