package rs.sep.bank.card;

import java.util.Optional;

/** Kartice koje banka prihvata. */
public enum CardBrand {
    VISA,
    MASTERCARD;

    public static Optional<CardBrand> detect(String pan) {
        if (pan.startsWith("4")) {
            return Optional.of(VISA);
        }
        if (pan.length() >= 4) {
            int prefix2 = Integer.parseInt(pan.substring(0, 2));
            int prefix4 = Integer.parseInt(pan.substring(0, 4));
            if ((prefix2 >= 51 && prefix2 <= 55) || (prefix4 >= 2221 && prefix4 <= 2720)) {
                return Optional.of(MASTERCARD);
            }
        }
        return Optional.empty();
    }
}
