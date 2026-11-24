package rs.sep.bank.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import rs.sep.bank.common.BankException;
import rs.sep.bank.common.HmacSigner;
import rs.sep.bank.config.BankProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API koji poziva PSP. Svaki zahtev mora biti potpisan HMAC-om zajednickom tajnom PSP-a i banke
 * (autentifikacija PSP-a iz koraka 3a specifikacije).
 */
@RestController
@RequestMapping("/api/acquirer/payments")
public class AcquirerApiController {

    private static final Logger log = LoggerFactory.getLogger(AcquirerApiController.class);

    private final PaymentSessionService service;
    private final HmacSigner signer;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Clock clock;

    public AcquirerApiController(PaymentSessionService service, BankProperties props, ObjectMapper objectMapper,
                                 Validator validator, Clock clock) {
        this.service = service;
        this.signer = new HmacSigner(props.psp().hmacSecret());
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.clock = clock;
    }

    @PostMapping
    public CreatePaymentResponse create(@RequestBody String body,
                                        @RequestHeader(value = HmacSigner.TIMESTAMP_HEADER, required = false) String timestamp,
                                        @RequestHeader(value = HmacSigner.SIGNATURE_HEADER, required = false) String signature)
            throws IOException {
        CreatePaymentRequest req = verifiedBody(body, timestamp, signature, CreatePaymentRequest.class);
        PaymentSession session = service.create(req.merchantId(), req.amount(), req.currency(), req.stan(),
                req.pspTimestamp());
        return new CreatePaymentResponse(session.getId().toString(), service.paymentUrl(session));
    }

    @PostMapping("/status")
    public StatusResponse status(@RequestBody String body,
                                 @RequestHeader(value = HmacSigner.TIMESTAMP_HEADER, required = false) String timestamp,
                                 @RequestHeader(value = HmacSigner.SIGNATURE_HEADER, required = false) String signature)
            throws IOException {
        StatusRequest req = verifiedBody(body, timestamp, signature, StatusRequest.class);
        PaymentSession s = service.status(req.merchantId(), req.stan(), req.pspTimestamp());
        return new StatusResponse(s.getId().toString(), s.getStatus().name(), s.getGlobalTransactionId(),
                s.getAcquirerTimestamp());
    }

    @ExceptionHandler(BankException.class)
    public ProblemDetail handle(BankException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    private <T> T verifiedBody(String body, String timestamp, String signature, Class<T> type) throws IOException {
        if (!signer.verify(timestamp, body, signature, clock.instant())) {
            log.warn("Odbijen zahtev sa neispravnim HMAC potpisom");
            throw new BankException(HttpStatus.UNAUTHORIZED, "Neispravan potpis zahteva");
        }
        T value = objectMapper.readValue(body, type);
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new BankException(HttpStatus.BAD_REQUEST, violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; ")));
        }
        return value;
    }

    /** Tabela 2 u specifikaciji. */
    record CreatePaymentRequest(
            @NotBlank String merchantId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Pattern(regexp = "\\d{6}") String stan,
            @NotNull Instant pspTimestamp) {
    }

    record CreatePaymentResponse(String paymentId, String paymentUrl) {
    }

    record StatusRequest(@NotBlank String merchantId, @NotBlank String stan, @NotNull Instant pspTimestamp) {
    }

    record StatusResponse(String paymentId, String status, String globalTransactionId, Instant acquirerTimestamp) {
    }
}
