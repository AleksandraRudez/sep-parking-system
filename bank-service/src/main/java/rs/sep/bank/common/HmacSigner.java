package rs.sep.bank.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * HMAC-SHA256 potpis poruka izmedju servisa. Potpisuje se "timestamp.telo", a primalac odbija
 * poruke sa pogresnim potpisom ili previse starim timestamp-om (zastita od izmene i ponavljanja).
 */
public final class HmacSigner {

    public static final String SIGNATURE_HEADER = "X-Signature";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final Duration MAX_SKEW = Duration.ofMinutes(5);

    private final byte[] key;

    public HmacSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("HMAC tajna nije podesena");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verify(String timestamp, String body, String signature, Instant now) {
        if (timestamp == null || signature == null) {
            return false;
        }
        try {
            Instant sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (Duration.between(sentAt, now).abs().compareTo(MAX_SKEW) > 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] expected = sign(timestamp, body).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
