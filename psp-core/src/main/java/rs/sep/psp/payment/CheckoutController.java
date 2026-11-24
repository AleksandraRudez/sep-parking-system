package rs.sep.psp.payment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** PSP stranica na koju prodavac preusmerava kupca; kupac ovde bira nacin placanja. */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final PaymentService paymentService;

    public CheckoutController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public String checkout(@PathVariable UUID paymentId, Model model) {
        CheckoutView view = paymentService.checkout(paymentId);
        if (view.status().isFinal()) {
            return "redirect:" + paymentService.resultUrl(paymentId);
        }
        if (view.status() == TransactionStatus.PENDING) {
            model.addAttribute("message", "Placanje je vec zapoceto. Zavrsite ga u kartici u kojoj ste ga otvorili.");
            return "checkout-message";
        }
        model.addAttribute("checkout", view);
        return "checkout";
    }

    @PostMapping("/{paymentId}")
    public String choose(@PathVariable UUID paymentId, @RequestParam String method) {
        return "redirect:" + paymentService.startPayment(paymentId, method);
    }

    /** Servis za placanje vraca kupca ovde; PSP ga zatim salje na SUCCESS/FAILED/ERROR stranicu prodavca. */
    @GetMapping("/return/{method}")
    public String returnFromProvider(@PathVariable String method, @RequestParam String paymentId, Model model) {
        return paymentService.handleReturn(method.toUpperCase(), paymentId)
                .map(url -> "redirect:" + url)
                .orElseGet(() -> {
                    model.addAttribute("message", "Placanje se obradjuje, sacekajte...");
                    model.addAttribute("refresh", true);
                    return "checkout-message";
                });
    }
}
