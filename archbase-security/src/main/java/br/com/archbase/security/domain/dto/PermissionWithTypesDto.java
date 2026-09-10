package br.com.archbase.security.domain.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.util.List;
import java.util.Set;


/**
 * Uma capacidade do recurso, com as origens por onde ela já foi concedida.
 *
 * <p><b>{@code actionName} é o identificador; {@code actionDescription} é texto humano.</b> A tela
 * exibia apenas o segundo, e as descrições do catálogo são geradas em massa — o
 * {@code useArchbaseCrudSecurity} do archbase-react registra sempre "Criar X", "Editar X",
 * "Listar X" —, então centenas de linhas ficam quase idênticas e nenhuma pode ser ligada ao
 * {@code @HasPermission(action = "...")} que o desenvolvedor escreveu. O nome resolve isso e não
 * muda nada para quem não o lê.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class PermissionWithTypesDto {
	/**
	 * A concessão a remover — <b>presente apenas quando a linha pertence à entidade em edição</b>.
	 *
	 * <p>Nulo significa que a capacidade chega por herança: o usuário a tem pelo grupo ou pelo
	 * perfil, e não há concessão dele para apagar. A interface precisa <b>dizer isso</b>; hoje ela
	 * apenas desabilita o botão de remover, e quem administra clica sem entender por que nada
	 * acontece. O caminho para tirar é abrir o grupo ou o perfil, ou registrar uma negação.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String permissionId;

	private String actionId;

	/** O identificador estável da capacidade — {@code aprovar_custo}, não "Aprovar o custo da OS". */
	private String actionName;

	/**
	 * O rótulo curto — "Aprovar custo".
	 *
	 * <p>Ausente quando a capacidade não tem rótulo, que é o caso de todo catálogo existente. A tela
	 * mostra {@code actionLabel ?? actionDescription}: enquanto ninguém declarar rótulo, ela exibe
	 * exatamente o que exibe hoje.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String actionLabel;

	private String actionDescription;

	/**
	 * O agrupamento dentro do recurso — "Custos".
	 *
	 * <p>Ausente quando não há. Substitui o {@code ->} embutido na descrição, que o cliente quebrava
	 * na exibição para simular hierarquia — um campo que identificava, explicava e agrupava ao mesmo
	 * tempo.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String actionCategory;

	/** Por quais origens esta capacidade já foi concedida: {@code USER}, {@code GROUP}, {@code PROFILE}. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Set<SecurityType> types;

	/**
	 * As capacidades que esta declara precisar — <b>apenas as diretas</b>, em texto
	 * {@code recurso:acao}.
	 *
	 * <p>Viajam junto do catálogo porque a tela já o baixa inteiro e precisa delas no momento da
	 * concessão: é o que permite oferecer as dependências junto, e avisar ao revogar. O fecho
	 * transitivo <b>não</b> vem aqui — mandar o fecho de centenas de capacidades numa resposta só
	 * custaria mais do que a informação vale, e ele só é olhado quando alguém abre o detalhe de uma
	 * linha. Para isso existe {@code GET /api/v1/resource/permissions/dependencies/{actionId}}.
	 *
	 * <p>Omitido quando não há nenhuma, o que é o caso de todo catálogo que ainda não declarou
	 * dependências.
	 *
	 * <p><b>Informativo, nunca portão.</b> A decisão de acesso não lê estas arestas.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<String> requires;
}
