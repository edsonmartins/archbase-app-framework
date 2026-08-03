package br.com.archbase.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O recurso ao qual todas as capacidades desta classe pertencem.
 *
 * <p>Evita repetir {@code resource} em cada método:
 *
 * <pre>{@code
 * @RestController
 * @ArchbaseResource("tms.ordemservico")
 * public class OrdemServicoController {
 *
 *     @GetMapping
 *     @HasPermission(action = "view", description = "Listar ordens de serviço")
 *     public ResponseEntity<?> listar() { ... }
 *
 *     @PostMapping("/{id}/aprovar-custo")
 *     @HasPermission(action = "aprovar_custo", description = "Aprovar o custo da OS",
 *                    minimumLevel = AccessLevel.SUPERVISOR)
 *     public ResponseEntity<?> aprovarCusto(...) { ... }
 * }
 * }</pre>
 *
 * <p>Um {@code resource} declarado no próprio {@code @HasPermission} vence esta anotação.
 *
 * <p><b>Por que uma anotação separada, e não {@code @HasPermission} na classe.</b> Uma capacidade é
 * <i>recurso + ação</i>, e a ação é sempre por método. {@code @HasPermission} na classe teria de
 * carregar uma ação também, e todo método herdaria a mesma — um {@code DELETE} passaria a exigir
 * apenas {@code view} porque foi isso que a classe declarou. Leria como proteção e seria grosseria.
 * Aqui só o recurso é herdado, que é a parte que de fato se repete.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArchbaseResource {

    /** Nome do recurso no catálogo. Convenção sugerida: {@code dominio.recurso}. */
    String value();

    /** Descrição do recurso, usada no primeiro registro no catálogo. */
    String description() default "";
}
