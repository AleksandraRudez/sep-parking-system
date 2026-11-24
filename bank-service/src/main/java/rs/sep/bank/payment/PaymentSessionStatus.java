package rs.sep.bank.payment;

public enum PaymentSessionStatus {
    /** Forma je kreirana i ceka kupca. */
    CREATED,
    /** Kupac je poslao podatke; forma vise ne moze da se koristi. */
    PROCESSING,
    SUCCESS,
    FAILED,
    ERROR,
    /** Kupac nije platio dok je forma vazila. */
    EXPIRED
}
