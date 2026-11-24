package rs.sep.bank.card;

import org.springframework.stereotype.Component;
import rs.sep.bank.config.BankProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC PAN-a i CVV-a sa tajnim kljucem banke. U bazi se cuva samo HMAC, pa ni neko ko procita bazu
 * ne moze da dobije brojeve kartica, niti da ih pogadja bez kljuca.
 */
@Component
public class CardSecrets {

    private final byte[] key;

    public CardSecrets(BankProperties props) {
        if (props.cardHmacKey() == null || props.cardHmacKey().isBlank()) {
            throw new IllegalStateException("BANK_CARD_HMAC_KEY nije podesen");
        }
        this.key = props.cardHmacKey().getBytes(StandardCharsets.UTF_8);
    }

    public String panHmac(String pan) {
        return hmac("pan:" + pan);
    }

    /** CVV se vezuje za PAN, da isti CVV na razlicitim karticama ne bi dao isti hash. */
    public String cvvHmac(String pan, String cvv) {
        return hmac("cvv:" + pan + ":" + cvv);
    }

    public boolean matches(String expectedHmac, String actualHmac) {
        return MessageDigest.isEqual(expectedHmac.getBytes(StandardCharsets.UTF_8),
                actualHmac.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
