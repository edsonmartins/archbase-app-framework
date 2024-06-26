package br.com.archbase.security.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class GrantPermissionDto {
	private String securityId;
	private String actionId;
	private SecurityType type;
}