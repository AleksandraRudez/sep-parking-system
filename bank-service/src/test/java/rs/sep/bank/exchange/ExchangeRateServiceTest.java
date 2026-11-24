package rs.sep.bank.exchange;

import org.junit.jupiter.api.Test;
import rs.sep.bank.common.BankException;
import rs.sep.bank.config.BankProperties;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateServiceTest {

    private final ExchangeRateService service = new ExchangeRateService(new BankProperties(null, null, null, null,
            Map.of("RSD", BigDecimal.ONE, "EUR", new BigDecimal("117.20"), "USD", new BigDecimal("108.50"))));

    @Test
    void sameCurrencyIsNotConverted() {
        assertThat(service.convert(new BigDecimal("200.00"), "RSD", "RSD")).isEqualByComparingTo("200.00");
    }

    @Test
    void euroIsConvertedToDinars() {
        assertThat(service.convert(new BigDecimal("10.00"), "EUR", "RSD")).isEqualByComparingTo("1172.00");
    }

    @Test
    void crossRateGoesThroughDinars() {
        assertThat(service.convert(new BigDecimal("10.00"), "EUR", "USD")).isEqualByComparingTo("10.80");
    }

    @Test
    void unknownCurrencyIsRejected() {
        assertThatThrownBy(() -> service.convert(BigDecimal.TEN, "JPY", "RSD")).isInstanceOf(BankException.class);
    }
}
