package rs.sep.psp.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretGeneratorTest {

    private final SecretGenerator generator = new SecretGenerator();

    @RepeatedTest(50)
    void temporaryPasswordSatisfiesRealmPolicy() {
        String password = generator.newTemporaryPassword();

        assertThat(password).hasSizeGreaterThanOrEqualTo(12);
        assertThat(password).containsPattern("[A-Z]");
        assertThat(password).containsPattern("[a-z]");
        assertThat(password).containsPattern("[0-9]");
        assertThat(password).containsPattern("[!@#$%^&*]");
    }

    @Test
    void apiKeysAreUniqueAndHashIsStable() {
        String a = generator.newApiKey();
        String b = generator.newApiKey();

        assertThat(a).isNotEqualTo(b).startsWith("psp_");
        assertThat(generator.sha256(a)).isEqualTo(generator.sha256(a)).hasSize(64);
    }
}
