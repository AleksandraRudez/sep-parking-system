package rs.sep.psp.merchant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Unapred dogovoreni URL-ovi klijenta. Cuvaju se na PSP-u da ne bi morali da se salju uz svaki zahtev.
 * webhookUrl je endpoint klijenta na koji PSP asinhrono javlja status transakcije.
 */
@Embeddable
public record MerchantUrls(
        @Column(name = "success_url", nullable = false) String successUrl,
        @Column(name = "failed_url", nullable = false) String failedUrl,
        @Column(name = "error_url", nullable = false) String errorUrl,
        @Column(name = "webhook_url", nullable = false) String webhookUrl) {
}
