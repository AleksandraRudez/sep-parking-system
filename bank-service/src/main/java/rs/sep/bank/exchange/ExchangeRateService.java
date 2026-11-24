package rs.sep.bank.exchange;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import rs.sep.bank.common.BankException;
import rs.sep.bank.config.BankProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Konverzija iznosa po kursu banke (kada se valuta placanja razlikuje od valute racuna). */
@Service
public class ExchangeRateService {

    private final Map<String, BigDecimal> ratesToRsd;

    public ExchangeRateService(BankProperties props) {
        this.ratesToRsd = props.exchangeRates();
    }

    public boolean supports(String currency) {
        return ratesToRsd.containsKey(currency);
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equals(to)) {
            return amount;
        }
        BigDecimal inRsd = amount.multiply(rate(from));
        return inRsd.divide(rate(to), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(String currency) {
        BigDecimal rate = ratesToRsd.get(currency);
        if (rate == null) {
            throw new BankException(HttpStatus.BAD_REQUEST, "Valuta " + currency + " nije podrzana");
        }
        return rate;
    }
}
