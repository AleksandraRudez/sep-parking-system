package rs.sep.psp.payment.dto;

import rs.sep.psp.payment.PaymentTransaction;
import rs.sep.psp.payment.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusResponse(
        UUID paymentId,
        String merchantOrderId,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String globalTransactionId,
        Instant acquirerTimestamp) {

    public static PaymentStatusResponse of(PaymentTransaction t) {
        return new PaymentStatusResponse(t.getId(), t.getMerchantOrderId(), t.getStatus(), t.getAmount(),
                t.getCurrency(), t.getPaymentMethod(), t.getGlobalTransactionId(), t.getAcquirerTimestamp());
    }
}
