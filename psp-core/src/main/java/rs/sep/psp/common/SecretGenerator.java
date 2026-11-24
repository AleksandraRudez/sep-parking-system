package rs.sep.psp.common;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/** Generisanje identifikatora, API kljuceva i privremenih lozinki kriptografski sigurnim RNG-om. */
@Component
public class SecretGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*";

    private final SecureRandom random = new SecureRandom();

    public String newMerchantId() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return "MRC-" + HexFormat.of().withUpperCase().formatHex(bytes);
    }

    public String newApiKey() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return "psp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Privremena lozinka koja zadovoljava password policy iz Keycloak realm-a. */
    public String newTemporaryPassword() {
        String all = UPPER + LOWER + DIGITS + SPECIAL;
        List<Character> chars = new ArrayList<>();
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        while (chars.size() < 16) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, random);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }

    public String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
