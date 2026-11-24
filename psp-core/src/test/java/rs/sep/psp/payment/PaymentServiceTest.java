package rs.sep.psp.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.sep.psp.common.BusinessException;
import rs.sep.psp.merchant.Merchant;
import rs.sep.psp.merchant.MerchantRepository;
import rs.sep.psp.merchant.MerchantUrls;
import rs.sep.psp.payment.dto.InitPaymentRequest;
import rs.sep.psp.payment.dto.InitPaymentResponse;
import rs.sep.psp.paymentmethod.PaymentMethod;
import rs.sep.psp.paymentmethod.SubscriptionService;
import rs.sep.psp.plugin.PaymentOutcome;
import rs.sep.psp.plugin.PaymentPlugin;
import rs.sep.psp.plugin.PaymentStart;
import rs.sep.psp.plugin.PluginRegistry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final MerchantRepository merchants = mock(MerchantRepository.class);
    private final SubscriptionService subscriptions = mock(SubscriptionService.class);
    private final PaymentPlugin cardPlugin = mock(PaymentPlugin.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private PaymentService service;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        when(cardPlugin.code()).thenReturn("CARD");
        PaymentProperties props = new PaymentProperties("http://psp", Set.of("RSD", "EUR"),
                Duration.ofMinutes(30), Duration.ofMinutes(15));
        // Pravi TransactionTemplate sa laznim menadzerom transakcija: callback se zaista izvrsava
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new PaymentService(transactions, merchants, subscriptions, new PluginRegistry(List.of(cardPlugin)),
                props, events, tx, Clock.fixed(NOW, ZoneOffset.UTC));

        merchant = new Merchant("MRC-1", "Parking", "p@test.rs", "kc-1", "hash",
                new MerchantUrls("https://shop/success", "https://shop/failed", "https://shop/error", "https://shop/hook"));
        when(merchants.getReferenceById(merchant.getId())).thenReturn(merchant);
        when(transactions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private InitPaymentRequest request(String amount, String currency, Instant timestamp) {
        return new InitPaymentRequest(new BigDecimal(amount), currency, "ORDER-1", timestamp);
    }

    @Test
    void newOrderCreatesTransaction() {
        when(transactions.findByMerchantIdAndMerchantOrderId(merchant.getId(), "ORDER-1")).thenReturn(Optional.empty());

        InitPaymentResponse response = service.initialize(merchant.getId(), request("200.00", "RSD", NOW));

        assertThat(response.status()).isEqualTo(TransactionStatus.CREATED);
        assertThat(response.paymentUrl()).isEqualTo("http://psp/checkout/" + response.paymentId());
        verify(transactions).saveAndFlush(any());
    }

    @Test
    void repeatedOrderReturnsExistingTransaction() {
        PaymentTransaction existing = transaction();
        when(transactions.findByMerchantIdAndMerchantOrderId(merchant.getId(), "ORDER-1")).thenReturn(Optional.of(existing));

        InitPaymentResponse response = service.initialize(merchant.getId(), request("200", "RSD", NOW));

        assertThat(response.paymentId()).isEqualTo(existing.getId());
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void repeatedOrderWithDifferentAmountIsRejected() {
        when(transactions.findByMerchantIdAndMerchantOrderId(merchant.getId(), "ORDER-1"))
                .thenReturn(Optional.of(transaction()));

        assertThatThrownBy(() -> service.initialize(merchant.getId(), request("1.00", "RSD", NOW)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unsupportedCurrencyIsRejected() {
        assertThatThrownBy(() -> service.initialize(merchant.getId(), request("200", "JPY", NOW)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    void oldMerchantTimestampIsRejected() {
        assertThatThrownBy(() -> service.initialize(merchant.getId(), request("200", "RSD", NOW.minusSeconds(3600))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MERCHANT_TIMESTAMP");
    }

    @Test
    void choosingMethodTwiceStartsPaymentOnlyOnce() {
        PaymentTransaction t = transaction();
        when(transactions.findByIdForUpdate(t.getId())).thenReturn(Optional.of(t));
        when(transactions.findById(t.getId())).thenReturn(Optional.of(t));
        when(transactions.nextStan()).thenReturn("000001");
        when(subscriptions.activeMethods(merchant.getId())).thenReturn(List.of(new PaymentMethod("CARD", "Kartica")));
        when(cardPlugin.start(t)).thenReturn(new PaymentStart("BANK-1", "http://bank/pay/BANK-1"));

        URI redirect = service.startPayment(t.getId(), "CARD");

        assertThat(redirect).hasToString("http://bank/pay/BANK-1");
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(t.getStan()).isEqualTo("000001");
        assertThatThrownBy(() -> service.startPayment(t.getId(), "CARD"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vec zapoceto");
        verify(cardPlugin, times(1)).start(any());
    }

    @Test
    void methodNotActivatedByMerchantIsRejected() {
        PaymentTransaction t = transaction();
        when(transactions.findByIdForUpdate(t.getId())).thenReturn(Optional.of(t));
        when(subscriptions.activeMethods(merchant.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.startPayment(t.getId(), "CARD")).isInstanceOf(BusinessException.class);
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.CREATED);
    }

    @Test
    void duplicateOutcomeNotifiesMerchantOnlyOnce() {
        PaymentTransaction t = transaction();
        t.startPayment("CARD", "000001", NOW);
        when(transactions.findByExternalIdForUpdate("CARD", "BANK-1")).thenReturn(Optional.of(t));
        PaymentOutcome success = new PaymentOutcome(TransactionStatus.SUCCESS, "GT-1", NOW);

        service.complete("CARD", "BANK-1", "000001", success);
        service.complete("CARD", "BANK-1", "000001", success);

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(events, times(1)).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void outcomeWithWrongStanIsRejected() {
        PaymentTransaction t = transaction();
        t.startPayment("CARD", "000001", NOW);
        when(transactions.findByExternalIdForUpdate("CARD", "BANK-1")).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.complete("CARD", "BANK-1", "999999",
                new PaymentOutcome(TransactionStatus.SUCCESS, "GT-1", NOW))).isInstanceOf(BusinessException.class);
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void returningBuyerIsSentToMerchantSuccessPage() {
        PaymentTransaction t = transaction();
        t.startPayment("CARD", "000001", NOW);
        t.complete(TransactionStatus.SUCCESS, "GT-1", NOW);
        when(transactions.findByPaymentMethodAndExternalPaymentId("CARD", "BANK-1")).thenReturn(Optional.of(t));
        when(transactions.findWithMerchant(t.getId())).thenReturn(Optional.of(t));

        Optional<URI> url = service.handleReturn("CARD", "BANK-1");

        assertThat(url).hasValueSatisfying(u -> assertThat(u.toString())
                .startsWith("https://shop/success?merchantOrderId=ORDER-1&paymentId=" + t.getId())
                .endsWith("status=SUCCESS"));
        verify(cardPlugin, never()).checkStatus(any());
    }

    @Test
    void returningBuyerTriggersStatusCheckWhenCallbackIsMissing() {
        PaymentTransaction t = transaction();
        t.startPayment("CARD", "000001", NOW);
        when(transactions.findByPaymentMethodAndExternalPaymentId("CARD", "BANK-1")).thenReturn(Optional.of(t));
        when(transactions.findWithMerchant(t.getId())).thenReturn(Optional.of(t));
        when(transactions.findByExternalIdForUpdate("CARD", "BANK-1")).thenReturn(Optional.of(t));
        when(cardPlugin.checkStatus(t)).thenReturn(Optional.of(new PaymentOutcome(TransactionStatus.FAILED, null, NOW)));

        Optional<URI> url = service.handleReturn("CARD", "BANK-1");

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(url).hasValueSatisfying(u -> assertThat(u.toString()).startsWith("https://shop/failed"));
    }

    @Test
    void returningBuyerWaitsWhileBankIsStillProcessing() {
        PaymentTransaction t = transaction();
        t.startPayment("CARD", "000001", NOW);
        when(transactions.findByPaymentMethodAndExternalPaymentId("CARD", "BANK-1")).thenReturn(Optional.of(t));
        when(transactions.findWithMerchant(t.getId())).thenReturn(Optional.of(t));
        when(cardPlugin.checkStatus(t)).thenReturn(Optional.empty());

        assertThat(service.handleReturn("CARD", "BANK-1")).isEmpty();
    }

    private PaymentTransaction transaction() {
        return new PaymentTransaction(merchant, "ORDER-1", NOW, new BigDecimal("200.00"), "RSD", NOW.plusSeconds(1800));
    }
}
