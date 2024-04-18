package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Group;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class GroupDto extends SecurityDto {

	protected List<UserDto> members;

	@Builder
	public GroupDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Set<ActionDto> actions, String email, List<UserDto> members) {
		super(id, code, version, createEntityDate, updateEntityDate, createdByUser, lastModifiedByUser, name, description, actions, email);
		this.members = members;
	}

	public static GroupDto fromDomain(Group group) {
		if (group == null) {
			return null;
		}

		return GroupDto.builder()
				.id(group.getId().toString())
				.code(group.getCode())
				.version(group.getVersion())
				.createEntityDate(group.getCreateEntityDate())
				.updateEntityDate(group.getUpdateEntityDate())
				.createdByUser(group.getCreatedByUser())
				.lastModifiedByUser(group.getLastModifiedByUser())
				.name(group.getName())
				.description(group.getDescription())
				.email(group.getEmail())
				.members(group.getMembers().stream()
						.map(UserDto::fromDomain)
						.collect(Collectors.toList()))
				.build();
	}

	public Group toDomain() {
		return Group.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.createdByUser(this.createdByUser)
				.lastModifiedByUser(this.lastModifiedByUser)
				.name(this.name)
				.description(this.description)
				.email(this.email)
				.members(this.members.stream()
						.map(UserDto::toDomain)
						.collect(Collectors.toList()))
				.build();
	}
}
