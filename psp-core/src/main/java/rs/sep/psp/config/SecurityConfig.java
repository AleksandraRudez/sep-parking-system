package rs.sep.psp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rs.sep.psp.common.SecretGenerator;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.security.ApiKeyAuthenticationFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    /** API za sisteme prodavaca: autentifikacija preko MERCHANT_ID + API_KEY. */
    @Bean
    @Order(1)
    SecurityFilterChain merchantApiChain(HttpSecurity http, MerchantRepository merchants, SecretGenerator secrets)
            throws Exception {
        return http
                .securityMatcher("/api/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ApiKeyAuthenticationFilter(merchants, secrets), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MERCHANT_API"))
                .build();
    }

    /** Obavestenja od banke: autentifikacija je HMAC potpis koji proverava kontroler. */
    @Bean
    @Order(2)
    SecurityFilterChain acquirerCallbackChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/acquirer/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /** Stranica za kupca: javna, ali forme su zasticene CSRF tokenom. */
    @Bean
    @Order(3)
    SecurityFilterChain checkoutChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/checkout/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /** Dashboard API: korisnici prijavljeni preko Keycloak-a. */
    @Bean
    @Order(4)
    SecurityFilterChain dashboardChain(HttpSecurity http) throws Exception {
        return http
                // API je stateless i koristi Bearer token, pa CSRF zastita nije potrebna
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Spring prosledjuje greske na /error; bez ovoga bi svaka greska izgledala kao 401
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/merchant/**").hasRole("MERCHANT")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .build();
    }

    /** Keycloak realm role (claim realm_access.roles) pretvara u Spring ROLE_* authority-je. */
    static Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmRoles);
        return converter;
    }

    static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${psp.dashboard.allowed-origin}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
