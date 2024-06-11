package br.com.archbase.security.persistence;

import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.entity.Profile;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("SEGURANCA_PERFIL")
@Getter
public class ProfileEntity extends SecurityEntity {

    public ProfileEntity() {
        // Default empty constructor
    }

    @Builder
    public ProfileEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, Set<ActionEntity> actions) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId, name, description);
    }



    public static ProfileEntity fromDomain(Profile profile) {
        if (profile == null) {
            return null;
        }


        ProfileEntity profileEntity = new ProfileEntity();
        profileEntity.setId(profile.getId().toString());
        profileEntity.setCode(profile.getCode());
        profileEntity.setVersion(profile.getVersion());
        profileEntity.setUpdateEntityDate(profile.getUpdateEntityDate());
        profileEntity.setCreateEntityDate(profile.getCreateEntityDate());
        profileEntity.setCreatedByUser(profile.getCreatedByUser());
        profileEntity.setLastModifiedByUser(profile.getLastModifiedByUser());
        profileEntity.setName(profile.getName());
        profileEntity.setDescription(profile.getDescription());

        return profileEntity;
    }

    public Profile toDomain() {
        return Profile.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .build();
    }

    public ProfileDto toDto() {
        return ProfileDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .name(this.getName())
                .description(this.getDescription())
                .build();
    }
}
