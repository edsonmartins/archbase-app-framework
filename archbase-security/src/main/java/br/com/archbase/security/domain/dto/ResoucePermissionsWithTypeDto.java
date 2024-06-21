package br.com.archbase.security.domain.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ResoucePermissionsWithTypeDto {

	private String resourceId;
	private String resourceDescription;
	private List<PermissionWithTypesDto> permissions;
}