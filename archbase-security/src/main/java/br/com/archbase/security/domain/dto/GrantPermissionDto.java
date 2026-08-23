package br.com.archbase.security.domain.dto;

import br.com.archbase.security.access.PermissionEffect;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionDto {
	private String securityId;
	private String actionId;
	private SecurityType type;

	/**
	 * {@code GRANT} (padrão, inclusive quando ausente) ou {@code DENY}.
	 *
	 * <p>Sem este campo a negação só existia por SQL direto — enquanto a documentação descrevia
	 * administradores criando negações pelo admin.
	 */
	private PermissionEffect effect;

	/** Estreitamento opcional. Vazio significa "vale em todo o tenant". */
	private String companyId;
	private String projectId;
}