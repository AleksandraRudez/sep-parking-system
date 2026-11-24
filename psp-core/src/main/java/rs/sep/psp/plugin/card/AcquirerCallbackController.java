package rs.sep.psp.plugin.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.sep.psp.common.HmacSigner;
import rs.sep.psp.payment.PaymentService;
import rs.sep.psp.plugin.PaymentOutcome;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

/** Banka asinhrono javlja ishod placanja karticom (korak 6 u specifikaciji). */
@RestController
@RequestMapping("/api/acquirer")
public class AcquirerCallbackController {

    private static final Logger log = LoggerFactory.getLogger(AcquirerCallbackController.class);

    private final PaymentService paymentService;
    private final HmacSigner signer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AcquirerCallbackController(PaymentService paymentService, AcquirerProperties props,
                                      ObjectMapper objectMapper, Clock clock) {
        this.paymentService = paymentService;
        this.signer = new HmacSigner(props.hmacSecret());
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody String body,
                                         @RequestHeader(value = HmacSigner.TIMESTAMP_HEADER, required = false) String timestamp,
                                         @RequestHeader(value = HmacSigner.SIGNATURE_HEADER, required = false) String signature)
            throws IOException {
        if (!signer.verify(timestamp, body, signature, clock.instant())) {
            log.warn("Odbijen callback banke sa neispravnim potpisom");
            return ResponseEntity.status(401).build();
        }
        Callback callback = objectMapper.readValue(body, Callback.class);
        var outcome = CardPaymentPlugin.toOutcome(callback.status());
        if (outcome.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        paymentService.complete(CardPaymentPlugin.CODE, callback.paymentId(), callback.stan(),
                new PaymentOutcome(outcome.get(), callback.globalTransactionId(), callback.acquirerTimestamp()));
        return ResponseEntity.ok().build();
    }

    record Callback(String paymentId, String merchantId, String stan, String status,
                    String globalTransactionId, Instant acquirerTimestamp) {
    }
}
