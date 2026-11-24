package rs.sep.psp.payment;

public enum TransactionStatus {
    /** Prodavac je inicijalizovao placanje, kupac jos nije izabrao nacin placanja. */
    CREATED,
    /** Kupac je izabrao nacin placanja, ceka se ishod od servisa za placanje. */
    PENDING,
    SUCCESS,
    FAILED,
    ERROR,
    /** Kupac nije zapoceo placanje u predvidjenom roku. */
    EXPIRED;

    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == ERROR || this == EXPIRED;
    }
}
