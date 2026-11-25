package br.com.archbase.error.handling;

import br.com.archbase.error.handling.handler.BeanValidationArchbaseApiExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConstraintViolationException vira 400 com erro por campo")
class BeanValidationArchbaseApiExceptionHandlerTest {

    static class Pedido {
        @NotBlank
        String cliente;
        @Min(1)
        Integer quantidade;

        Pedido(String cliente, Integer quantidade) {
            this.cliente = cliente;
            this.quantidade = quantidade;
        }
    }

    private final Validator validator = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();

    private ConstraintViolationException violacao() {
        return new ConstraintViolationException(validator.validate(new Pedido("", 0)));
    }

    @Test
    @DisplayName("exceção direta: 400, um erro por campo, código da restrição")
    void direta() {
        var handler = new BeanValidationArchbaseApiExceptionHandler(new ArchbaseErrorHandlingProperties());
        ConstraintViolationException e = violacao();

        assertThat(handler.canHandle(e)).isTrue();
        ArchbaseApiErrorResponse resposta = handler.handle(e);
        assertThat(resposta.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getFieldErrors())
                .extracting(ArchbaseApiFieldError::getProperty, ArchbaseApiFieldError::getCode,
                        ArchbaseApiFieldError::getRejectedValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("cliente", "NotBlank", ""),
                        org.assertj.core.groups.Tuple.tuple("quantidade", "Min", 0));
    }

    @Test
    @DisplayName("embrulhada no commit (como o Hibernate entrega): também 400")
    void embrulhada() {
        var handler = new BeanValidationArchbaseApiExceptionHandler(new ArchbaseErrorHandlingProperties());
        var e = new TransactionSystemException("Could not commit", new RuntimeException("rollback", violacao()));

        assertThat(handler.canHandle(e)).isTrue();
        assertThat(handler.handle(e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("outras exceções continuam com os demais tratadores")
    void outras() {
        var handler = new BeanValidationArchbaseApiExceptionHandler(new ArchbaseErrorHandlingProperties());
        assertThat(handler.canHandle(new IllegalStateException("x"))).isFalse();
        assertThat(handler.canHandle(new TransactionSystemException("x"))).isFalse();
    }
}
