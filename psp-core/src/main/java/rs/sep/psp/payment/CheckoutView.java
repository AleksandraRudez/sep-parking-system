package rs.sep.psp.payment;

import rs.sep.psp.paymentmethod.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Podaci za PSP stranicu na kojoj kupac bira nacin placanja. */
public record CheckoutView(UUID paymentId, String merchantName, String merchantOrderId, BigDecimal amount,
                           String currency, TransactionStatus status, List<PaymentMethod> methods) {
}
