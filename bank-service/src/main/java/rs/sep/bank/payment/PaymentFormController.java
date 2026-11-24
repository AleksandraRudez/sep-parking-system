package rs.sep.bank.payment;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/** Stranica banke na kojoj kupac unosi podatke o kartici (PAYMENT_URL). */
@Controller
@RequestMapping("/pay")
public class PaymentFormController {

    private static final DateTimeFormatter VALID_UNTIL = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String UNAVAILABLE = "Link za placanje je istekao ili je vec iskoriscen.";

    private final PaymentSessionService service;

    public PaymentFormController(PaymentSessionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public String form(@PathVariable UUID id, Model model) {
        return service.usableSession(id)
                .map(session -> showForm(session, Map.of(), null, model))
                .orElseGet(() -> message(UNAVAILABLE, null, model));
    }

    @PostMapping("/{id}")
    public String submit(@PathVariable UUID id, @ModelAttribute CardForm form, Model model) {
        PaymentResult result = service.pay(id, form);
        return switch (result) {
            case PaymentResult.Invalid invalid -> service.usableSession(id)
                    .map(session -> showForm(session, invalid.errors(), form, model))
                    .orElseGet(() -> message(UNAVAILABLE, null, model));
            case PaymentResult.Unavailable unavailable -> message(unavailable.message(), null, model);
            case PaymentResult.Processed processed -> {
                model.addAttribute("success", processed.status() == PaymentSessionStatus.SUCCESS);
                yield message(processed.message(), processed.returnUrl(), model);
            }
        };
    }

    private String showForm(PaymentSession session, Map<String, String> errors, CardForm form, Model model) {
        model.addAttribute("payment", session);
        model.addAttribute("validUntil", VALID_UNTIL.format(session.getExpiresAt()));
        model.addAttribute("errors", errors);
        // PAN i CVV se nikad ne vracaju nazad u formu
        model.addAttribute("holderName", form == null ? "" : form.holderName());
        model.addAttribute("expiry", form == null ? "" : form.expiry());
        return "payment-form";
    }

    private String message(String message, String returnUrl, Model model) {
        model.addAttribute("message", message);
        model.addAttribute("returnUrl", returnUrl);
        return "payment-message";
    }
}
