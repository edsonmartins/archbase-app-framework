package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Action;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ActionDto {

	protected String id;
	protected String code;
	protected Long version;
	protected LocalDateTime createEntityDate;
	protected LocalDateTime updateEntityDate;
	protected String createdByUser;
	protected String lastModifiedByUser;
	protected String name;
	protected String description;
	protected ResourceDto resource;
	protected String category;
	protected Boolean active;
	protected String actionVersion;

	public static ActionDto fromDomain(Action action) {
		if (action == null) {
			return null;
		}

		return ActionDto.builder()
				.id(action.getId().toString())
				.code(action.getCode())
				.version(action.getVersion())
				.createEntityDate(action.getCreateEntityDate())
				.updateEntityDate(action.getUpdateEntityDate())
				.createdByUser(action.getCreatedByUser())
				.lastModifiedByUser(action.getLastModifiedByUser())
				.name(action.getName())
				.description(action.getDescription())
				.resource(ResourceDto.fromDomain(action.getResource()))
				.category(action.getCategory())
				.active(action.getActive())
				.actionVersion(action.getActionVersion())
				.build();
	}

	public Action toDomain() {
		return Action.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.lastModifiedByUser(this.lastModifiedByUser)
				.createdByUser(this.createdByUser)
				.name(this.name)
				.description(this.description)
				.resource(this.resource != null ? this.resource.toDomain() : null)
				.category(this.category)
				.active(this.active)
				.actionVersion(this.actionVersion)
				.build();
	}
}