package br.com.archbase.security.domain.dto;

import br.com.archbase.security.access.AccessLevel;

import br.com.archbase.security.domain.entity.Profile;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class ProfileDto extends SecurityDto {

	/**
	 * Nível que este perfil confere. Editável aqui — é a fonte padrão do portão LEVEL.
	 */
	protected AccessLevel accessLevel;

    public ProfileDto() {
    }

    @Builder
    public ProfileDto(String id, String code, Long version, LocalDateTime createEntityDate, LocalDateTime updateEntityDate, String createdByUser, String lastModifiedByUser, String name, String description, Set<ActionDto> actions, AccessLevel accessLevel) {
        super(id, code, version, createEntityDate, updateEntityDate, createdByUser, lastModifiedByUser, name, description, actions);
        this.accessLevel = accessLevel;
    }


    public static ProfileDto fromDomain(Profile profile) {
        if (profile == null) {
            return null;
        }

        return ProfileDto.builder()
                .id(profile.getId().toString())
                .code(profile.getCode())
                .version(profile.getVersion())
                .createEntityDate(profile.getCreateEntityDate())
                .updateEntityDate(profile.getUpdateEntityDate())
                .createdByUser(profile.getCreatedByUser())
                .lastModifiedByUser(profile.getLastModifiedByUser())
                .name(profile.getName())
                .description(profile.getDescription())
				.accessLevel(profile.getAccessLevel())
                .build();
    }

    public Profile toDomain() {
        return Profile.builder()
                .id(this.id)
                .code(this.code)
                .version(this.version)
                .createEntityDate(this.createEntityDate)
                .updateEntityDate(this.updateEntityDate)
                .createdByUser(this.createdByUser)
                .lastModifiedByUser(this.lastModifiedByUser)
                .name(this.name)
                .description(this.description)
				.accessLevel(this.accessLevel)
                .build();
    }
}
