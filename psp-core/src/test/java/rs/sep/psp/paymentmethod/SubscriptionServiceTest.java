package rs.sep.psp.paymentmethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.merchant.MerchantUrls;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    private static final String KC_USER = "kc-user-1";

    private final MerchantRepository merchants = mock(MerchantRepository.class);
    private final MerchantSubscriptionRepository subscriptions = mock(MerchantSubscriptionRepository.class);
    private final PaymentMethodRepository paymentMethods = mock(PaymentMethodRepository.class);
    private final SubscriptionService service = new SubscriptionService(merchants, subscriptions, paymentMethods);

    private Merchant merchant;
    private PaymentMethod card;

    @BeforeEach
    void setUp() {
        merchant = new Merchant("MRC-1", "Parking", "parking@test.rs", KC_USER, "hash",
                new MerchantUrls("https://p/s", "https://p/f", "https://p/e", "https://p/w"));
        card = new PaymentMethod("CARD", "Platna kartica");
        when(merchants.findByKeycloakUserIdForUpdate(KC_USER)).thenReturn(Optional.of(merchant));
        when(paymentMethods.findByCode("CARD")).thenReturn(Optional.of(card));
    }

    @Test
    void deactivatingLastActiveMethodIsRejected() {
        MerchantSubscription sub = new MerchantSubscription(merchant, card);
        when(subscriptions.findByMerchantIdAndPaymentMethodCode(merchant.getId(), "CARD")).thenReturn(Optional.of(sub));
        when(subscriptions.countByMerchantIdAndActiveTrue(merchant.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivate(KC_USER, "card"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(sub.isActive()).isTrue();
    }

    @Test
    void deactivatingWhenAnotherMethodIsActiveSucceeds() {
        MerchantSubscription sub = new MerchantSubscription(merchant, card);
        when(subscriptions.findByMerchantIdAndPaymentMethodCode(merchant.getId(), "CARD")).thenReturn(Optional.of(sub));
        when(subscriptions.countByMerchantIdAndActiveTrue(merchant.getId())).thenReturn(2L);

        service.deactivate(KC_USER, "CARD");

        assertThat(sub.isActive()).isFalse();
    }

    @Test
    void activatingNewMethodCreatesSubscription() {
        when(subscriptions.findByMerchantIdAndPaymentMethodCode(merchant.getId(), "CARD")).thenReturn(Optional.empty());

        service.activate(KC_USER, "CARD");

        verify(subscriptions).save(any(MerchantSubscription.class));
    }

    @Test
    void reactivatingExistingSubscriptionDoesNotCreateDuplicate() {
        MerchantSubscription sub = new MerchantSubscription(merchant, card);
        sub.deactivate();
        when(subscriptions.findByMerchantIdAndPaymentMethodCode(merchant.getId(), "CARD")).thenReturn(Optional.of(sub));

        service.activate(KC_USER, "CARD");

        assertThat(sub.isActive()).isTrue();
        verify(subscriptions, never()).save(any());
    }

    @Test
    void unknownMethodIsRejected() {
        when(paymentMethods.findByCode("PAYONEER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(KC_USER, "payoneer"))
                .isInstanceOf(BusinessException.class);
    }
}
