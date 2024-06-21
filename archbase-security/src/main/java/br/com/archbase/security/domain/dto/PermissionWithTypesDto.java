package br.com.archbase.security.domain.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;


@Getter
@Setter
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class PermissionWithTypesDto {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String permissionId;
	private String actionId;
	private String actionDescription;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Set<SecurityType> types;
}