package rs.sep.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class ShopController {

    private final OrderStore orders;
    private final PspClient psp;

    public ShopController(OrderStore orders, PspClient psp) {
        this.orders = orders;
        this.psp = psp;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("orders", orders.all());
        return "index";
    }

    @PostMapping("/orders")
    public String order(@RequestParam String description, @RequestParam BigDecimal amount,
                        @RequestParam String currency, Model model) {
        ShopOrder order = orders.save(new ShopOrder("ORD-" + UUID.randomUUID().toString().substring(0, 8),
                description, amount, currency));
        try {
            PspClient.InitResponse response = psp.initialize(order);
            order.paymentStarted(response.paymentId());
            return "redirect:" + response.paymentUrl();
        } catch (RestClientResponseException e) {
            order.confirmed("GRESKA", null);
            model.addAttribute("orders", orders.all());
            model.addAttribute("error", "PSP je odbio zahtev: " + e.getResponseBodyAsString());
            return "index";
        }
    }

    /** SUCCESS_URL / FAILED_URL / ERROR_URL. Prikazuje se poruka, a status narudzbine dolazi iz webhook-a. */
    @GetMapping("/payment/{result}")
    public String result(@PathVariable String result, @RequestParam(required = false) String merchantOrderId,
                         Model model) {
        model.addAttribute("result", result);
        model.addAttribute("order", merchantOrderId == null ? null : orders.find(merchantOrderId).orElse(null));
        return "result";
    }
}
