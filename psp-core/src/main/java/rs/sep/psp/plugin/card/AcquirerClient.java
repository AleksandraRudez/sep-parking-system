package rs.sep.psp.plugin.card;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import rs.sep.psp.common.HmacSigner;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** Poziva API Acquirer banke. Svaki zahtev je potpisan HMAC-om da bi banka mogla da autentifikuje PSP. */
@Component
public class AcquirerClient {

    private final RestClient restClient;
    private final HmacSigner signer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AcquirerClient(AcquirerProperties props, RestClient.Builder builder, ObjectMapper objectMapper, Clock clock) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.signer = new HmacSigner(props.hmacSecret());
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Zahtev za dobijanje PAYMENT_URL i PAYMENT_ID (tabela 2 u specifikaciji). */
    public CreatePaymentResponse createPayment(String merchantId, BigDecimal amount, String currency,
                                               String stan, Instant pspTimestamp) {
        String body = toJson(Map.of(
                "merchantId", merchantId,
                "amount", amount,
                "currency", currency,
                "stan", stan,
                "pspTimestamp", pspTimestamp.toString()));
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        return restClient.post()
                .uri("/api/acquirer/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HmacSigner.TIMESTAMP_HEADER, timestamp)
                .header(HmacSigner.SIGNATURE_HEADER, signer.sign(timestamp, body))
                .body(body)
                .retrieve()
                .body(CreatePaymentResponse.class);
    }

    /** Provera statusa po MERCHANT_ID + STAN + PSP_TIMESTAMP, kada PSP nije dobio odgovor od banke. */
    public StatusResponse status(String merchantId, String stan, Instant pspTimestamp) {
        String body = toJson(Map.of(
                "merchantId", merchantId,
                "stan", stan,
                "pspTimestamp", pspTimestamp.toString()));
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        return restClient.post()
                .uri("/api/acquirer/payments/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HmacSigner.TIMESTAMP_HEADER, timestamp)
                .header(HmacSigner.SIGNATURE_HEADER, signer.sign(timestamp, body))
                .body(body)
                .retrieve()
                .body(StatusResponse.class);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public record CreatePaymentResponse(String paymentId, String paymentUrl) {
    }

    public record StatusResponse(String paymentId, String status, String globalTransactionId,
                                 Instant acquirerTimestamp) {
    }
}
