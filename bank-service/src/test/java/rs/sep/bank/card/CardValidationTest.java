package rs.sep.bank.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CardValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"4242424242424242", "5555555555554444", "4000056655665556", "79927398713"})
    void validLuhnNumbersPass(String pan) {
        // 79927398713 je klasican primer iz opisa algoritma, ali je prekratak za karticu
        assertThat(CardValidation.isValidLuhn(pan)).isEqualTo(pan.length() >= 12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4242424242424241", "5555555555554445", "1234567812345678", "", "4242abcd42424242"})
    void invalidNumbersFail(String pan) {
        assertThat(CardValidation.isValidLuhn(pan)).isFalse();
    }

    @Test
    void spacesAndDashesAreRemoved() {
        assertThat(CardValidation.normalizePan("4242 4242-4242 4242")).isEqualTo("4242424242424242");
    }

    @Test
    void expiryMustBeMmYy() {
        assertThat(CardValidation.parseExpiry("12/29")).contains(YearMonth.of(2029, 12));
        assertThat(CardValidation.parseExpiry("01/30")).contains(YearMonth.of(2030, 1));
        assertThat(CardValidation.parseExpiry("13/29")).isEmpty();
        assertThat(CardValidation.parseExpiry("00/29")).isEmpty();
        assertThat(CardValidation.parseExpiry("1/29")).isEmpty();
        assertThat(CardValidation.parseExpiry("12/2029")).isEmpty();
        assertThat(CardValidation.parseExpiry("1229")).isEmpty();
    }

    @Test
    void cardIsValidUntilEndOfExpiryMonth() {
        YearMonth now = YearMonth.of(2026, 11);
        assertThat(CardValidation.isExpired(YearMonth.of(2026, 11), now)).isFalse();
        assertThat(CardValidation.isExpired(YearMonth.of(2026, 10), now)).isTrue();
    }

    @Test
    void cvvMustHaveThreeDigits() {
        assertThat(CardValidation.isValidCvv("123")).isTrue();
        assertThat(CardValidation.isValidCvv("12")).isFalse();
        assertThat(CardValidation.isValidCvv("1234")).isFalse();
        assertThat(CardValidation.isValidCvv("12a")).isFalse();
    }

    @Test
    void brandIsDetectedFromPrefix() {
        assertThat(CardBrand.detect("4242424242424242")).contains(CardBrand.VISA);
        assertThat(CardBrand.detect("5555555555554444")).contains(CardBrand.MASTERCARD);
        assertThat(CardBrand.detect("2221000000000009")).contains(CardBrand.MASTERCARD);
        assertThat(CardBrand.detect("6011111111111117")).isEmpty();
    }
}
