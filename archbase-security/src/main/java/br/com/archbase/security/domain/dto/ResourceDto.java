package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Resource;
import br.com.archbase.security.domain.entity.TipoRecurso;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
	protected Boolean active;
	protected TipoRecurso type;

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
				.type(resource.getType())
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
				.type(this.type)
				.build();
	}
}
