package rs.sep.bank.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import rs.sep.bank.common.HmacSigner;
import rs.sep.bank.config.BankProperties;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Javlja PSP-u ishod placanja (korak 6). Ako PSP nije dostupan, ishod ostaje u banci, a PSP ga
 * proverava sam preko MERCHANT_ID + STAN + PSP_TIMESTAMP.
 */
@Component
public class PspNotifier {

    private static final Logger log = LoggerFactory.getLogger(PspNotifier.class);

    private final RestClient restClient;
    private final HmacSigner signer;
    private final ObjectMapper objectMapper;
    private final BankProperties props;
    private final Clock clock;

    public PspNotifier(RestClient.Builder builder, ObjectMapper objectMapper, BankProperties props, Clock clock) {
        this.restClient = builder.build();
        this.signer = new HmacSigner(props.psp().hmacSecret());
        this.objectMapper = objectMapper;
        this.props = props;
        this.clock = clock;
    }

    public void notifyOutcome(PaymentSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", session.getId().toString());
        payload.put("merchantId", session.getMerchant().getMerchantId());
        payload.put("stan", session.getStan());
        payload.put("status", session.getStatus().name());
        payload.put("globalTransactionId", session.getGlobalTransactionId());
        payload.put("acquirerTimestamp", session.getAcquirerTimestamp() == null ? null
                : session.getAcquirerTimestamp().toString());
        String body = toJson(payload);
        String timestamp = String.valueOf(clock.instant().getEpochSecond());
        try {
            restClient.post()
                    .uri(props.psp().callbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSigner.TIMESTAMP_HEADER, timestamp)
                    .header(HmacSigner.SIGNATURE_HEADER, signer.sign(timestamp, body))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("PSP nije primio ishod placanja {}: {}", session.getId(), e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
