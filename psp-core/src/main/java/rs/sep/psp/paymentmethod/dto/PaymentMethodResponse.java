package rs.sep.psp.paymentmethod.dto;

import rs.sep.psp.paymentmethod.PaymentMethod;

public record PaymentMethodResponse(String code, String name) {

    public static PaymentMethodResponse of(PaymentMethod pm) {
        return new PaymentMethodResponse(pm.getCode(), pm.getName());
    }
}
