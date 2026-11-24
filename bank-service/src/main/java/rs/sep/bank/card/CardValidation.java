package rs.sep.bank.card;

import java.time.YearMonth;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Provera podataka sa forme za placanje, pre nego sto se uopste pristupi racunu. */
public final class CardValidation {

    private static final Pattern EXPIRY = Pattern.compile("(0[1-9]|1[0-2])/(\\d{2})");

    private CardValidation() {
    }

    /** Uklanja razmake i crtice koje kupac unese radi citljivosti. */
    public static String normalizePan(String pan) {
        return pan == null ? "" : pan.replaceAll("[\\s-]", "");
    }

    /** Lunov algoritam (mod 10): svaka druga cifra s desna se duplira, zbir mora biti deljiv sa 10. */
    public static boolean isValidLuhn(String pan) {
        if (pan == null || !pan.matches("\\d{12,19}")) {
            return false;
        }
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int digit = pan.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    /** Datum isteka u formatu MM/YY. */
    public static Optional<YearMonth> parseExpiry(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher m = EXPIRY.matcher(value.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))));
    }

    /** Kartica vazi do kraja meseca isteka. */
    public static boolean isExpired(YearMonth expiry, YearMonth current) {
        return expiry.isBefore(current);
    }

    public static boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3}");
    }
}
