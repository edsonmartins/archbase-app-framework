package br.com.archbase.security.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class ResouceActionPermissionDto {

	private String resourceId;
	private String resourceDescription;
	private String permissionId;
	private String actionId;
	private String actionDescription;

	public static ResouceActionPermissionDto fromPermissionDto(PermissionDto permissionDto) {
		return ResouceActionPermissionDto.builder()
				.resourceId(permissionDto.getAction().getResource().getId())
				.resourceDescription(permissionDto.getAction().getResource().getDescription())
				.permissionId(permissionDto.getId())
				.actionId(permissionDto.getAction().getId())
				.actionDescription(permissionDto.getAction().getDescription())
				.build();
	}
}