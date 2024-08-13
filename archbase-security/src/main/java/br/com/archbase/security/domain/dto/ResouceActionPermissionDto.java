package br.com.archbase.security.domain.dto;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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