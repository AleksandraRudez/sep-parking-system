package rs.sep.psp.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Poziva Keycloak Admin REST API preko servisnog naloga psp-core klijenta.
 * Koristi se za kreiranje naloga novih klijenata PSP-a.
 */
@Component
public class KeycloakAdminClient {

    private final KeycloakProperties props;
    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakProperties props, RestClient.Builder builder) {
        this.props = props;
        this.restClient = builder.baseUrl(props.serverUrl()).build();
    }

    /**
     * Kreira korisnika sa privremenom lozinkom i obaveznom promenom lozinke pri prvoj prijavi.
     *
     * @return Keycloak ID kreiranog korisnika
     */
    public String createUserWithTemporaryPassword(String email, String displayName,
                                                  String temporaryPassword, String realmRole) {
        String token = serviceAccountToken();

        Map<String, Object> user = Map.of(
                "username", email,
                "email", email,
                "firstName", displayName,
                // Keycloak user profile zahteva i prezime; bez njega trazi dopunu profila pri prijavi
                "lastName", "Klijent",
                "enabled", true,
                "emailVerified", true,
                "requiredActions", List.of("UPDATE_PASSWORD"),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", temporaryPassword,
                        "temporary", true)));

        URI location = restClient.post()
                .uri("/admin/realms/{realm}/users", props.realm())
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak nije vratio lokaciju kreiranog korisnika");
        }
        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        assignRealmRole(token, userId, realmRole);
        return userId;
    }

    public void deleteUser(String userId) {
        restClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", props.realm(), userId)
                .headers(h -> h.setBearerAuth(serviceAccountToken()))
                .retrieve()
                .toBodilessEntity();
    }

    private void assignRealmRole(String token, String userId, String roleName) {
        JsonNode role = restClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", props.realm(), roleName)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class);

        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.realm(), userId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private String serviceAccountToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        JsonNode response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        return response.get("access_token").asText();
    }
}
