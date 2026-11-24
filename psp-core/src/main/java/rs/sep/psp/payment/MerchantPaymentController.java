package rs.sep.psp.payment;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.sep.psp.payment.dto.InitPaymentRequest;
import rs.sep.psp.payment.dto.InitPaymentResponse;
import rs.sep.psp.payment.dto.PaymentStatusResponse;
import rs.sep.psp.security.MerchantApiPrincipal;

import java.util.UUID;

/** API koji pozivaju sistemi prodavaca (npr. parking sistem). Autentifikacija: X-Merchant-Id + X-Api-Key. */
@RestController
@RequestMapping("/api/v1/payments")
public class MerchantPaymentController {

    private final PaymentService paymentService;

    public MerchantPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public InitPaymentResponse initialize(@AuthenticationPrincipal MerchantApiPrincipal merchant,
                                          @Valid @RequestBody InitPaymentRequest request) {
        return paymentService.initialize(merchant.id(), request);
    }

    @GetMapping("/{paymentId}")
    public PaymentStatusResponse status(@AuthenticationPrincipal MerchantApiPrincipal merchant,
                                        @PathVariable UUID paymentId) {
        return paymentService.statusForMerchant(merchant.id(), paymentId);
    }

    @GetMapping
    public PaymentStatusResponse statusByOrder(@AuthenticationPrincipal MerchantApiPrincipal merchant,
                                               @RequestParam String merchantOrderId) {
        return paymentService.statusForMerchantByOrder(merchant.id(), merchantOrderId);
    }
}
