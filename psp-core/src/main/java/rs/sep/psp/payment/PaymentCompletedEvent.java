package rs.sep.psp.payment;

import java.util.UUID;

/** Transakcija je dobila konacan ishod; prodavca treba obavestiti. */
public record PaymentCompletedEvent(UUID transactionId) {
}
