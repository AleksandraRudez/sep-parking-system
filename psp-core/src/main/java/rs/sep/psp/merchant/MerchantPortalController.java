package rs.sep.psp.merchant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rs.sep.psp.merchant.dto.ApiKeyResponse;
import rs.sep.psp.merchant.dto.MerchantResponse;
import rs.sep.psp.merchant.dto.UpdateMerchantUrlsRequest;
import rs.sep.psp.paymentmethod.SubscriptionService;
import rs.sep.psp.paymentmethod.dto.SubscriptionResponse;

import java.util.List;

/** Klijentski portal (uloga MERCHANT). Klijent se prepoznaje po "sub" claim-u iz Keycloak tokena. */
@RestController
@RequestMapping("/api/merchant/me")
public class MerchantPortalController {

    private final MerchantService merchantService;
    private final SubscriptionService subscriptionService;

    public MerchantPortalController(MerchantService merchantService, SubscriptionService subscriptionService) {
        this.merchantService = merchantService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public MerchantResponse me(@AuthenticationPrincipal Jwt jwt) {
        return merchantService.me(jwt.getSubject());
    }

    @PutMapping("/urls")
    public MerchantResponse updateUrls(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody UpdateMerchantUrlsRequest request) {
        return merchantService.updateUrls(jwt.getSubject(), request);
    }

    @PostMapping("/api-key")
    public ApiKeyResponse rotateApiKey(@AuthenticationPrincipal Jwt jwt) {
        return merchantService.rotateApiKey(jwt.getSubject());
    }

    @GetMapping("/payment-methods")
    public List<SubscriptionResponse> paymentMethods(@AuthenticationPrincipal Jwt jwt) {
        return subscriptionService.listForMerchant(jwt.getSubject());
    }

    @PutMapping("/payment-methods/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        subscriptionService.activate(jwt.getSubject(), code);
    }

    @DeleteMapping("/payment-methods/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        subscriptionService.deactivate(jwt.getSubject(), code);
    }
}
