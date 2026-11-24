package rs.sep.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /** API za PSP: autentifikacija je HMAC potpis, koji proverava kontroler. */
    @Bean
    @Order(1)
    SecurityFilterChain acquirerApiChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/acquirer/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /** Forma za placanje: javna, zasticena CSRF tokenom; skripte i stilovi samo sa sajta banke. */
    @Bean
    @Order(2)
    SecurityFilterChain paymentPageChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/pay/**", "/actuator/health", "/error").permitAll()
                        .anyRequest().denyAll())
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'")))
                .build();
    }
}
