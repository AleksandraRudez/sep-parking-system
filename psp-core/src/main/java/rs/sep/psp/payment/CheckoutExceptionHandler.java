package rs.sep.psp.payment;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import rs.sep.psp.common.BusinessException;

/** Greske na stranici za kupca prikazuju se kao HTML stranica, a ne kao JSON. */
@ControllerAdvice(assignableTypes = CheckoutController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CheckoutExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handle(BusinessException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "checkout-message";
    }
}
