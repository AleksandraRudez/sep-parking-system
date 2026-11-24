package rs.sep.bank.payment;

import java.util.Map;

/** Ishod slanja forme za placanje. */
public sealed interface PaymentResult {

    /** Podaci nisu u ispravnom formatu; forma se prikazuje ponovo, pokusaj nije potrosen. */
    record Invalid(Map<String, String> errors) implements PaymentResult {
    }

    /** Forma je istekla ili je vec iskoriscena. */
    record Unavailable(String message) implements PaymentResult {
    }

    /** Placanje je obradjeno (uspesno ili odbijeno). */
    record Processed(PaymentSessionStatus status, String message, String returnUrl) implements PaymentResult {
    }
}
