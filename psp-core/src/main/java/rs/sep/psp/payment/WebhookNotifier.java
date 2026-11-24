package rs.sep.psp.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import rs.sep.psp.common.HmacSigner;
import rs.sep.psp.payment.dto.PaymentStatusResponse;

import java.time.Clock;

/**
 * Asinhrono obavestava prodavca o ishodu transakcije (POST na njegov webhook URL).
 * Telo je potpisano HMAC-om; kljuc je SHA-256 hash API kljuca prodavca (hex), koji prodavac moze sam da izracuna.
 */
@Component
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);
    private static final long[] RETRY_DELAYS_MS = {0, 2_000, 10_000};

    private final PaymentTransactionRepository transactions;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WebhookNotifier(PaymentTransactionRepository transactions, RestClient.Builder builder,
                           ObjectMapper objectMapper, Clock clock) {
        this.transactions = transactions;
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) throws InterruptedException {
        PaymentTransaction t = transactions.findWithMerchant(event.transactionId()).orElseThrow();
        String body = toJson(PaymentStatusResponse.of(t));
        HmacSigner signer = new HmacSigner(t.getMerchant().getApiKeyHash());
        String url = t.getMerchant().getUrls().webhookUrl();

        for (long delay : RETRY_DELAYS_MS) {
            Thread.sleep(delay);
            String timestamp = String.valueOf(clock.instant().getEpochSecond());
            try {
                restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HmacSigner.TIMESTAMP_HEADER, timestamp)
                        .header(HmacSigner.SIGNATURE_HEADER, signer.sign(timestamp, body))
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Prodavac obavesten o transakciji {} ({})", t.getId(), t.getStatus());
                return;
            } catch (RuntimeException e) {
                log.warn("Webhook {} za transakciju {} nije uspeo: {}", url, t.getId(), e.getMessage());
            }
        }
        // Prodavac i dalje moze da proveri status preko GET /api/v1/payments/{id}
        log.error("Prodavac nije obavesten o transakciji {} posle {} pokusaja", t.getId(), RETRY_DELAYS_MS.length);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
