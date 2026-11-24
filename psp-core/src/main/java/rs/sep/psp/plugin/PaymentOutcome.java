package rs.sep.psp.plugin;

import rs.sep.psp.payment.TransactionStatus;

import java.time.Instant;

public record PaymentOutcome(TransactionStatus status, String globalTransactionId, Instant acquirerTimestamp) {
}
