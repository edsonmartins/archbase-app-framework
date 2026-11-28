package br.com.archbase.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Troca de senha recusada por um motivo do pedido: senha atual errada, confirmação diferente, senha nova fora da
 * política, ou conta cuja senha é de outro provedor. Responde 400 com a mensagem.
 *
 * <p>Até a 3.5.0 esses casos lançavam {@link IllegalStateException} (ou a exceção de validação da política), sem
 * status, e o {@code PATCH /api/v1/user} respondia 500. Estende {@link IllegalStateException} para não quebrar quem
 * já a captura.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ArchbasePasswordChangeRejectedException extends IllegalStateException {

    public ArchbasePasswordChangeRejectedException(String message) {
        super(message);
    }

    public ArchbasePasswordChangeRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
