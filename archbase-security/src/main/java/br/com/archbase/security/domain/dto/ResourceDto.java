package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Resource;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ResourceDto {

	protected String id;
	protected String code;
	protected Long version;
	protected LocalDateTime createEntityDate;
	protected LocalDateTime updateEntityDate;
	protected String createdByUser;
	protected String lastModifiedByUser;
	protected String name;
	protected String description;
	protected List<ActionDto> actions = new ArrayList<>();
	protected Boolean active;

	public static ResourceDto fromDomain(Resource resource) {
		if (resource == null) {
			return null;
		}

		return ResourceDto.builder()
				.id(resource.getId().toString())
				.code(resource.getCode())
				.version(resource.getVersion())
				.createEntityDate(resource.getCreateEntityDate())
				.updateEntityDate(resource.getUpdateEntityDate())
				.createdByUser(resource.getCreatedByUser())
				.lastModifiedByUser(resource.getLastModifiedByUser())
				.name(resource.getName())
				.description(resource.getDescription())
				.active(resource.getActive())
				.actions(resource.getActions().stream()
						.map(ActionDto::fromDomain)
						.collect(Collectors.toList()))
				.build();
	}

	public Resource toDomain() {
		return Resource.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.lastModifiedByUser(this.lastModifiedByUser)
				.createdByUser(this.createdByUser)
				.name(this.name)
				.active(this.active)
				.description(this.description)
				.actions(this.actions.stream()
						.map(ActionDto::toDomain)
						.collect(Collectors.toList()))
				.build();
	}
}
