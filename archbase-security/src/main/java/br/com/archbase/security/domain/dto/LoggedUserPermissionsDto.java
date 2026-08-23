package br.com.archbase.security.domain.dto;

import lombok.*;

import java.util.Map;
import java.util.Set;

/**
 * Tudo que o usuário autenticado pode, em uma resposta só.
 *
 * <p>Existe porque a pergunta "o que esta pessoa alcança?" não tinha resposta barata. O
 * {@code /permissions/{resourceName}} responde por um recurso, e quem monta um menu precisa da
 * resposta para dezenas deles — o cliente acabava disparando uma requisição por item, ou o produto
 * desistia e mostrava tudo para todo mundo. Foi o que aconteceu no admin do gestor-rq.
 *
 * <p>O filtro é <b>o mesmo</b> de {@link ResourcePermissionsDto}: fora as negadas, fora as de ação
 * inativa. Duas listagens da mesma coisa com critérios diferentes é como a tela passa a divergir da
 * decisão — e o menu habilitaria o que a tela recusa.
 *
 * @param administrator vem junto porque quem é administrador não depende do catálogo: o cliente
 *                      precisa saber disso para não interpretar um mapa vazio como "não pode nada"
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoggedUserPermissionsDto {

    protected Boolean administrator;

    /** Nome do recurso para os nomes das ações concedidas nele. */
    protected Map<String, Set<String>> permissions;

}
