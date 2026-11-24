package rs.sep.psp.payment.dto;

import rs.sep.psp.payment.TransactionStatus;

import java.util.UUID;

/** @param paymentUrl adresa PSP stranice na koju prodavac preusmerava kupca */
public record InitPaymentResponse(UUID paymentId, String paymentUrl, TransactionStatus status) {
}
