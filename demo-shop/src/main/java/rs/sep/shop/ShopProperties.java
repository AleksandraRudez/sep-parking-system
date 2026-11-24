package rs.sep.shop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shop")
public record ShopProperties(String publicUrl, Psp psp) {

    public record Psp(String baseUrl, String merchantId, String apiKey) {
    }
}
