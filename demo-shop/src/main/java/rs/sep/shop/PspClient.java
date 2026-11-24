package rs.sep.shop;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Poziva PSP API kao sistem prodavca (MERCHANT_ID + API_KEY u zaglavljima). */
@Component
public class PspClient {

    private final RestClient restClient;

    public PspClient(ShopProperties props, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(props.psp().baseUrl())
                .defaultHeader("X-Merchant-Id", props.psp().merchantId())
                .defaultHeader("X-Api-Key", props.psp().apiKey())
                .build();
    }

    public InitResponse initialize(ShopOrder order) {
        return restClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "amount", order.getAmount(),
                        "currency", order.getCurrency(),
                        "merchantOrderId", order.getOrderId(),
                        "merchantTimestamp", Instant.now().toString()))
                .retrieve()
                .body(InitResponse.class);
    }

    public record InitResponse(String paymentId, String paymentUrl, String status) {
    }

    public record PaymentStatus(String paymentId, String merchantOrderId, String status, BigDecimal amount,
                                String currency, String paymentMethod, String globalTransactionId) {
    }
}
