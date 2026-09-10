package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.TipoRecurso;
import lombok.*;


/**
 * A concessão recém-gravada, devolvida ao cliente que a pediu.
 *
 * <p>Carrega os mesmos identificadores de {@link ResoucePermissionsWithTypeDto} e
 * {@link PermissionWithTypesDto} — nome do recurso, tipo do recurso e nome da ação — porque o
 * {@code PermissionsSelectorModal} monta a linha nova <b>a partir desta resposta</b>, sem recarregar
 * a lista. Sem eles, a capacidade acabada de conceder aparecia na árvore sem nome e sem
 * classificação até alguém reabrir o modal, e a tela contradizia a si mesma na própria ação que o
 * operador tinha acabado de fazer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResouceActionPermissionDto {

	private String resourceId;
	private String resourceName;
	private String resourceDescription;
	private TipoRecurso resourceType;
	private String permissionId;
	private String actionId;
	private String actionName;
	private String actionLabel;
	private String actionDescription;
	private String actionCategory;

	public static ResouceActionPermissionDto fromPermissionDto(PermissionDto permissionDto) {
		ActionDto acao = permissionDto.getAction();
		ResourceDto recurso = acao.getResource();
		return ResouceActionPermissionDto.builder()
				.resourceId(recurso.getId())
				.resourceName(recurso.getName())
				.resourceDescription(recurso.getDescription())
				.resourceType(recurso.getType())
				.permissionId(permissionDto.getId())
				.actionId(acao.getId())
				.actionName(acao.getName())
				.actionLabel(acao.getLabel())
				.actionDescription(acao.getDescription())
				.actionCategory(acao.getCategory())
				.build();
	}
}
