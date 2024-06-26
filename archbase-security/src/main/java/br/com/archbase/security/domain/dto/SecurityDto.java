package br.com.archbase.security.domain.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = ProfileDto.class, name = "profile"),
		@JsonSubTypes.Type(value = GroupDto.class, name = "group"),
		@JsonSubTypes.Type(value = UserDto.class, name = "user")
})
public abstract class SecurityDto {

	protected String id;
	protected String code;
	protected Long version;
	protected LocalDateTime createEntityDate;
	protected LocalDateTime updateEntityDate;
	protected String createdByUser;
	protected String lastModifiedByUser;
	protected String name;
	protected String description;
	protected Set<ActionDto> actions = new HashSet<>();

	public SecurityDto() {
	}

	public SecurityDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Set<ActionDto> actions) {
		this.id = id;
		this.code = code;
		this.version = version;
		this.createEntityDate = createEntityDate;
		this.updateEntityDate = updateEntityDate;
		this.createdByUser = createdByUser;
		this.lastModifiedByUser = lastModifiedByUser;
		this.name = name;
		this.description = description;
		this.actions = actions;
	}
}
