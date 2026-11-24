package rs.sep.psp.plugin.card;

import org.springframework.stereotype.Component;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.payment.PaymentTransaction;
import rs.sep.psp.payment.TransactionStatus;
import rs.sep.psp.plugin.PaymentOutcome;
import rs.sep.psp.plugin.PaymentPlugin;
import rs.sep.psp.plugin.PaymentStart;

import java.util.Optional;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

/** Placanje platnom karticom preko Acquirer banke. */
@Component
public class CardPaymentPlugin implements PaymentPlugin {

    public static final String CODE = "CARD";

    private final AcquirerClient acquirer;

    public CardPaymentPlugin(AcquirerClient acquirer) {
        this.acquirer = acquirer;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public PaymentStart start(PaymentTransaction tx) {
        AcquirerClient.CreatePaymentResponse response = acquirer.createPayment(acquirerMerchantId(tx),
                tx.getAmount(), tx.getCurrency(), tx.getStan(), tx.getPspTimestamp());
        return new PaymentStart(response.paymentId(), response.paymentUrl());
    }

    @Override
    public Optional<PaymentOutcome> checkStatus(PaymentTransaction tx) {
        AcquirerClient.StatusResponse response = acquirer.status(acquirerMerchantId(tx), tx.getStan(),
                tx.getPspTimestamp());
        return toOutcome(response.status())
                .map(status -> new PaymentOutcome(status, response.globalTransactionId(),
                        response.acquirerTimestamp()));
    }

    /** Prevodi status banke u status PSP transakcije; prazno dok banka jos obradjuje placanje. */
    static Optional<TransactionStatus> toOutcome(String bankStatus) {
        return switch (bankStatus) {
            case "SUCCESS" -> Optional.of(TransactionStatus.SUCCESS);
            // Kupac nije platio u roku vazenja forme
            case "FAILED", "EXPIRED" -> Optional.of(TransactionStatus.FAILED);
            case "ERROR" -> Optional.of(TransactionStatus.ERROR);
            default -> Optional.empty();
        };
    }

    private static String acquirerMerchantId(PaymentTransaction tx) {
        String id = tx.getMerchant().getAcquirerMerchantId();
        if (id == null) {
            throw new BusinessException(UNPROCESSABLE_ENTITY, "Prodavac nije povezan sa bankom za placanje karticom");
        }
        return id;
    }
}
