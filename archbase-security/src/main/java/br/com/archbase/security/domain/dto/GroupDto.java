package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.Group;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class GroupDto extends SecurityDto {
    @Builder
	public GroupDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Set<ActionDto> actions) {
		super(id, code, version, createEntityDate, updateEntityDate, createdByUser, lastModifiedByUser, name, description, actions);
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
				.build();
	}
}
