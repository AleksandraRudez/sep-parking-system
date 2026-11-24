package rs.sep.psp.plugin.card;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Veza sa Acquirer bankom.
 *
 * @param hmacSecret zajednicka tajna PSP-a i banke kojom se potpisuju poruke
 */
@ConfigurationProperties(prefix = "psp.acquirer")
public record AcquirerProperties(String baseUrl, String hmacSecret) {
}
