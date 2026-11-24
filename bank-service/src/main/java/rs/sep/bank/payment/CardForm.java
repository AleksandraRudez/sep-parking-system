package rs.sep.bank.payment;

/** Podaci koje kupac unosi na formi. Ne loguju se i ne cuvaju (osim poslednje 4 cifre). */
public record CardForm(String pan, String holderName, String expiry, String cvv) {
}
