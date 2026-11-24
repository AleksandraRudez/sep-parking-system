package rs.sep.psp.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "psp.keycloak")
public record KeycloakProperties(String serverUrl, String realm, String clientId, String clientSecret) {
}
