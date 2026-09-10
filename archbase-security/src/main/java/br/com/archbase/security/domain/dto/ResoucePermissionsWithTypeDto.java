package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.TipoRecurso;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.util.List;

/**
 * Um recurso e as capacidades dele, para a tela de concessão de permissões.
 *
 * <p><b>{@code resourceName} e {@code resourceType} existem porque a tela não conseguia fazer o
 * próprio trabalho sem eles.</b> Este DTO carregava apenas identificador e descrição, e o
 * {@code PermissionsSelectorModal} monta a árvore de escolha em cima dele — de modo que recurso de
 * tela e recurso de endpoint apareciam lado a lado, indistinguíveis, embora o banco saiba a
 * diferença desde sempre em {@code SEGURANCA_RECURSO.TIPO_RECURSO}. Não era decisão da interface:
 * era ausência de dado.
 *
 * <p>E a descrição não identifica. Ela é texto humano, passa por tradução no cliente e é o eixo de
 * agrupamento improvisado com {@code ->}; duas capacidades com descrição parecida ficam impossíveis
 * de distinguir, e quem administra não consegue ligar a linha ao {@code @HasPermission} que o
 * desenvolvedor escreveu. O nome é o identificador estável — {@code tms.ordemservico} — e agora
 * chega junto.
 *
 * <p>Campos acrescentados, nada removido: cliente que não os conhece continua funcionando.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ResoucePermissionsWithTypeDto {

	private String resourceId;

	/** O identificador estável do recurso — {@code tms.ordemservico}, não "Ordens de serviço". */
	private String resourceName;

	private String resourceDescription;

	/**
	 * {@code VIEW} para recurso registrado por uma tela, {@code API} para o que a varredura do
	 * {@code @HasPermission} cadastrou.
	 *
	 * <p>Pode vir {@code null}: recurso criado antes de a coluna existir, ou pelo
	 * {@code POST /api/v1/resource} sem tipo declarado. A tela deve tratar o nulo como uma terceira
	 * classificação visível — "sem classificação" — e não empurrá-lo para um dos dois baldes; o
	 * recurso passa a ter tipo assim que um dos coletores o encontrar de novo.
	 */
	private TipoRecurso resourceType;

	/**
	 * Se o recurso está ativo.
	 *
	 * <p><b>A lista NÃO filtra por isto</b>, e não deve passar a filtrar. Uma capacidade sobre ação
	 * ativa de recurso inativo é concedível hoje, funciona hoje no {@code @HasPermission} — que não
	 * consulta {@code active} — e <b>deixará de funcionar</b> no dia em que
	 * {@code archbase.security.permission.require-active} for ligado. Escondê-la da tela tiraria do
	 * admin a chance de arrumar isso antes; e escondê-la sem tirá-la da decisão seria a interface
	 * discordando do avaliador, que é o defeito que o core existe para eliminar.
	 *
	 * <p>Então ela aparece, marcada. {@code null} em backends que ainda não enviam o campo — a tela
	 * trata nulo como ativo, que é o comportamento de antes.
	 */
	private Boolean resourceActive;

	private List<PermissionWithTypesDto> permissions;
}
