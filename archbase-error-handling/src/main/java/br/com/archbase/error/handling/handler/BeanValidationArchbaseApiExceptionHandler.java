package br.com.archbase.error.handling.handler;

import br.com.archbase.error.handling.ArchbaseApiErrorResponse;
import br.com.archbase.error.handling.ArchbaseApiFieldError;
import br.com.archbase.error.handling.ArchbaseErrorHandlingProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;

import java.time.temporal.Temporal;
import java.util.Comparator;

/**
 * {@link ConstraintViolationException} vira 400 com um erro por campo.
 *
 * <p>É a exceção dos services genéricos do {@code archbase-domain-driven-design} ao recusar um objeto inválido, da
 * validação de método ({@code @Validated}) e do Hibernate ao gravar uma entidade inválida — neste caso ela chega
 * embrulhada (na {@code TransactionSystemException} do commit, por exemplo), por isso a cadeia de causas é examinada.
 * Até a 3.5.0 todas caíam no tratamento genérico e respondiam 500.
 *
 * <p>O valor recusado só vai na resposta quando é simples (texto, número, booleano, enum, data); um objeto inteiro
 * — uma entidade com restrição de classe, por exemplo — não é devolvido.
 */
public class BeanValidationArchbaseApiExceptionHandler extends AbstractArchbaseApiExceptionHandler {

    public BeanValidationArchbaseApiExceptionHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return encontrar(exception) != null;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        ConstraintViolationException violacao = encontrar(exception);
        if (violacao == null) {
            throw new IllegalStateException("métodos canHandle() e handle() não estão em sincronia!");
        }
        int quantidade = violacao.getConstraintViolations() == null ? 0 : violacao.getConstraintViolations().size();
        ArchbaseApiErrorResponse response = new ArchbaseApiErrorResponse(HttpStatus.BAD_REQUEST,
                getErrorCode(violacao),
                "A validação falhou. Contagem de erros: " + quantidade);
        if (quantidade > 0) {
            violacao.getConstraintViolations().stream()
                    .sorted(Comparator.comparing(v -> String.valueOf(v.getPropertyPath())))
                    .map(this::erroDeCampo)
                    .forEach(response::addFieldError);
        }
        return response;
    }

    private ArchbaseApiFieldError erroDeCampo(ConstraintViolation<?> violacao) {
        String propriedade = String.valueOf(violacao.getPropertyPath());
        String restricao = violacao.getConstraintDescriptor() == null ? null
                : violacao.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
        String codigoDoCampo = propriedade + "." + restricao;
        String codigo = hasConfiguredOverrideForCode(codigoDoCampo)
                ? replaceCodeWithConfiguredOverrideIfPresent(codigoDoCampo)
                : replaceCodeWithConfiguredOverrideIfPresent(restricao);
        String mensagem = violacao.getMessage();
        if (hasConfiguredOverrideForMessage(codigoDoCampo)) {
            mensagem = getOverrideMessage(codigoDoCampo);
        } else if (restricao != null && hasConfiguredOverrideForMessage(restricao)) {
            mensagem = getOverrideMessage(restricao);
        }
        return new ArchbaseApiFieldError(codigo, propriedade, mensagem, valorSimples(violacao.getInvalidValue()));
    }

    private static Object valorSimples(Object valor) {
        if (valor instanceof CharSequence || valor instanceof Number || valor instanceof Boolean
                || valor instanceof Enum<?> || valor instanceof Temporal || valor instanceof Character) {
            return valor;
        }
        return null;
    }

    private static ConstraintViolationException encontrar(Throwable exception) {
        Throwable atual = exception;
        for (int profundidade = 0; atual != null && profundidade < 10; profundidade++) {
            if (atual instanceof ConstraintViolationException cve) {
                return cve;
            }
            if (atual.getCause() == atual) {
                return null;
            }
            atual = atual.getCause();
        }
        return null;
    }
}
