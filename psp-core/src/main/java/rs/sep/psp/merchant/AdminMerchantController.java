package rs.sep.psp.merchant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rs.sep.psp.merchant.dto.AcquirerMerchantRequest;
import rs.sep.psp.merchant.dto.MerchantCreatedResponse;
import rs.sep.psp.merchant.dto.MerchantResponse;
import rs.sep.psp.merchant.dto.RegisterMerchantRequest;
import rs.sep.psp.paymentmethod.PaymentMethodRepository;
import rs.sep.psp.paymentmethod.dto.PaymentMethodResponse;

import java.util.List;
import java.util.UUID;

/** Administratorski portal (uloga SUPER_ADMIN). */
@RestController
@RequestMapping("/api/admin")
public class AdminMerchantController {

    private final MerchantService merchantService;
    private final PaymentMethodRepository paymentMethods;

    public AdminMerchantController(MerchantService merchantService, PaymentMethodRepository paymentMethods) {
        this.merchantService = merchantService;
        this.paymentMethods = paymentMethods;
    }

    @GetMapping("/merchants")
    public List<MerchantResponse> merchants() {
        return merchantService.findAll();
    }

    @PostMapping("/merchants")
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantCreatedResponse register(@Valid @RequestBody RegisterMerchantRequest request) {
        return merchantService.register(request);
    }

    @PutMapping("/merchants/{id}/acquirer")
    public MerchantResponse changeAcquirer(@PathVariable UUID id,
                                           @Valid @RequestBody AcquirerMerchantRequest request) {
        return merchantService.changeAcquirerMerchantId(id, request.acquirerMerchantId());
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> paymentMethods() {
        return paymentMethods.findByEnabledTrueOrderByIdAsc().stream().map(PaymentMethodResponse::of).toList();
    }
}
