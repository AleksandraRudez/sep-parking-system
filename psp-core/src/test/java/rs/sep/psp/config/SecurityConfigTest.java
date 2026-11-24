package rs.sep.psp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.sep.psp.common.SecretGenerator;
import rs.sep.psp.merchant.AdminMerchantController;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.merchant.MerchantPortalController;
import rs.sep.psp.merchant.MerchantService;
import rs.sep.psp.paymentmethod.PaymentMethodRepository;
import rs.sep.psp.paymentmethod.SubscriptionService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminMerchantController.class, MerchantPortalController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MerchantService merchantService;
    @MockitoBean
    SubscriptionService subscriptionService;
    @MockitoBean
    PaymentMethodRepository paymentMethodRepository;
    @MockitoBean
    MerchantRepository merchantRepository;
    @MockitoBean
    SecretGenerator secretGenerator;

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/admin/merchants")).andExpect(status().isUnauthorized());
    }

    @Test
    void merchantCannotAccessAdminPortal() throws Exception {
        mvc.perform(get("/api/admin/merchants").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MERCHANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCanAccessAdminPortal() throws Exception {
        mvc.perform(get("/api/admin/merchants").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminCannotAccessMerchantPortal() throws Exception {
        mvc.perform(get("/api/merchant/me/payment-methods")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void merchantApiRequiresApiKey() throws Exception {
        mvc.perform(get("/api/v1/payments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void merchantApiRejectsKeycloakTokenWithoutApiKey() throws Exception {
        mvc.perform(get("/api/v1/payments/00000000-0000-0000-0000-000000000000")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void keycloakRealmRolesAreMappedToSpringRoles() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("MERCHANT")))
                .build();

        assertThat(SecurityConfig.realmRoles(token))
                .extracting(Object::toString)
                .containsExactly("ROLE_MERCHANT");
    }
}
