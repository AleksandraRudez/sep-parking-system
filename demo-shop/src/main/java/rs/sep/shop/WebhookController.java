package rs.sep.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * PSP asinhrono javlja ishod placanja. Potpis se proverava kljucem SHA-256(API_KEY) u hex obliku,
 * pa niko osim PSP-a ne moze da lazno potvrdi placanje.
 */
@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final OrderStore orders;
    private final ObjectMapper objectMapper;
    private final HmacSigner signer;

    public WebhookController(OrderStore orders, ObjectMapper objectMapper, ShopProperties props)
            throws NoSuchAlgorithmException {
        this.orders = orders;
        this.objectMapper = objectMapper;
        byte[] keyHash = MessageDigest.getInstance("SHA-256").digest(props.psp().apiKey().getBytes(StandardCharsets.UTF_8));
        this.signer = new HmacSigner(HexFormat.of().formatHex(keyHash));
    }

    @PostMapping("/api/psp/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body,
                                        @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
                                        @RequestHeader(value = "X-Signature", required = false) String signature)
            throws IOException {
        if (!signer.verify(timestamp, body, signature, Instant.now())) {
            log.warn("Odbijen webhook sa neispravnim potpisom");
            return ResponseEntity.status(401).build();
        }
        PspClient.PaymentStatus status = objectMapper.readValue(body, PspClient.PaymentStatus.class);
        orders.find(status.merchantOrderId()).ifPresent(o -> o.confirmed(status.status(), status.globalTransactionId()));
        log.info("PSP potvrdio narudzbinu {}: {}", status.merchantOrderId(), status.status());
        return ResponseEntity.ok().build();
    }
}
