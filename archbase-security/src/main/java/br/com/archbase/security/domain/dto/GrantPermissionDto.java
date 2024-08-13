package br.com.archbase.security.domain.dto;

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
}