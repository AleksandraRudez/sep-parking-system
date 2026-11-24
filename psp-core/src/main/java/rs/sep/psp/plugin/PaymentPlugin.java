package rs.sep.psp.plugin;

import rs.sep.psp.payment.PaymentTransaction;

import java.util.Optional;

/**
 * Nacin placanja (plugin). Novi nacin placanja se dodaje implementacijom ovog interfejsa,
 * bez izmene ostatka PSP-a.
 */
public interface PaymentPlugin {

    /** Kod nacina placanja iz tabele payment_method (npr. CARD). */
    String code();

    /** Pokrece placanje kod spoljnog servisa i vraca adresu na koju se kupac preusmerava. */
    PaymentStart start(PaymentTransaction transaction);

    /**
     * Pita spoljni servis za konacan ishod (kada obavestenje od servisa nije stiglo).
     * Prazno ako placanje jos nije zavrseno.
     */
    Optional<PaymentOutcome> checkStatus(PaymentTransaction transaction);
}
