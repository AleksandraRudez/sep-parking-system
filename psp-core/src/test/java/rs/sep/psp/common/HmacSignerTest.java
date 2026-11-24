package rs.sep.psp.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner("tajna");
    private final Instant now = Instant.parse("2026-11-24T12:00:00Z");
    private final String ts = String.valueOf(now.getEpochSecond());

    @Test
    void validSignatureIsAccepted() {
        assertThat(signer.verify(ts, "{\"amount\":200}", signer.sign(ts, "{\"amount\":200}"), now)).isTrue();
    }

    @Test
    void modifiedBodyIsRejected() {
        String signature = signer.sign(ts, "{\"amount\":200}");
        assertThat(signer.verify(ts, "{\"amount\":1}", signature, now)).isFalse();
    }

    @Test
    void wrongSecretIsRejected() {
        String signature = new HmacSigner("druga-tajna").sign(ts, "telo");
        assertThat(signer.verify(ts, "telo", signature, now)).isFalse();
    }

    @Test
    void oldMessageIsRejected() {
        String signature = signer.sign(ts, "telo");
        assertThat(signer.verify(ts, "telo", signature, now.plusSeconds(600))).isFalse();
    }
}
