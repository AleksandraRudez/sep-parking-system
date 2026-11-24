package rs.sep.bank.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.sep.bank.account.Account;
import rs.sep.bank.config.SecurityConfig;
import rs.sep.bank.merchant.AcquirerMerchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentFormController.class)
@Import(SecurityConfig.class)
class PaymentFormControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaymentSessionService service;

    private final PaymentSession session = new PaymentSession(
            new AcquirerMerchant("ACQ-1", "Parking Servis d.o.o.", new Account("1", "P", "RSD", BigDecimal.ZERO)),
            new BigDecimal("250.00"), "RSD", "000001", Instant.now(), Instant.now(), Instant.now().plusSeconds(900));

    @Test
    void formShowsMerchantAmountAndCardLogos() throws Exception {
        when(service.usableSession(session.getId())).thenReturn(Optional.of(session));

        mvc.perform(get("/pay/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parking Servis d.o.o.")))
                .andExpect(content().string(containsString("250,00 RSD")))
                .andExpect(content().string(containsString("aria-label=\"Visa\"")))
                .andExpect(content().string(containsString("aria-label=\"Mastercard\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void invalidCardDataIsShownWithoutEchoingPan() throws Exception {
        when(service.usableSession(session.getId())).thenReturn(Optional.of(session));
        when(service.pay(any(), any())).thenReturn(new PaymentResult.Invalid(Map.of("pan", "Broj kartice nije ispravan")));

        mvc.perform(post("/pay/" + session.getId()).with(csrf())
                        .param("pan", "4242424242424241").param("holderName", "Petar")
                        .param("expiry", "12/29").param("cvv", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Broj kartice nije ispravan")))
                .andExpect(content().string(not(containsString("4242424242424241"))));
    }

    @Test
    void usedLinkShowsMessage() throws Exception {
        when(service.usableSession(session.getId())).thenReturn(Optional.empty());

        mvc.perform(get("/pay/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("istekao ili je vec iskoriscen")));
    }

    @Test
    void processedPaymentLinksBackToPsp() throws Exception {
        when(service.pay(any(), any())).thenReturn(new PaymentResult.Processed(PaymentSessionStatus.SUCCESS,
                "Placanje je uspesno izvrseno.", "http://psp/checkout/return/card?paymentId=1"));

        mvc.perform(post("/pay/" + session.getId()).with(csrf())
                        .param("pan", "4242424242424242").param("holderName", "Petar Petrovic")
                        .param("expiry", "12/29").param("cvv", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Placanje je uspesno izvrseno.")))
                .andExpect(content().string(containsString("http://psp/checkout/return/card?paymentId=1")));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/pay/" + session.getId()).param("pan", "4242424242424242"))
                .andExpect(status().isForbidden());
    }
}
