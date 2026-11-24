package rs.sep.psp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.sep.psp.common.SecretGenerator;
import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Autentifikacija sistema prodavca: zaglavlja X-Merchant-Id i X-Api-Key.
 * API kljuc se poredi kao hash, u konstantnom vremenu.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String MERCHANT_ID_HEADER = "X-Merchant-Id";
    public static final String API_KEY_HEADER = "X-Api-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final MerchantRepository merchants;
    private final SecretGenerator secrets;

    public ApiKeyAuthenticationFilter(MerchantRepository merchants, SecretGenerator secrets) {
        this.merchants = merchants;
        this.secrets = secrets;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String merchantId = request.getHeader(MERCHANT_ID_HEADER);
        String apiKey = request.getHeader(API_KEY_HEADER);
        Optional<Merchant> merchant = merchantId == null || apiKey == null
                ? Optional.empty()
                : merchants.findByMerchantId(merchantId).filter(Merchant::isActive)
                        .filter(m -> matches(m.getApiKeyHash(), secrets.sha256(apiKey)));

        if (merchant.isEmpty()) {
            log.warn("Neuspesna autentifikacija prodavca {} sa {}", merchantId, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"status\":401,\"detail\":\"Neispravan MERCHANT_ID ili API kljuc\"}");
            return;
        }
        var principal = new MerchantApiPrincipal(merchant.get().getId(), merchant.get().getMerchantId());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                AuthorityUtils.createAuthorityList("ROLE_MERCHANT_API"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private static boolean matches(String storedHash, String providedHash) {
        return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                providedHash.getBytes(StandardCharsets.UTF_8));
    }
}
