package br.com.archbase.controlplane.access;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte {@link ControlPlaneAccessDeniedException} em 403 com {@code {"status","error","code","message"}} — o mesmo
 * formato das recusas do filtro.
 *
 * @since 3.4.6
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControlPlaneExceptionHandler {

    @ExceptionHandler(ControlPlaneAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handle(ControlPlaneAccessDeniedException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", e.getCode());
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
