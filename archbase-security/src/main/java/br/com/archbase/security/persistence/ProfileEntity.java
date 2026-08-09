package br.com.archbase.security.persistence;

import br.com.archbase.security.access.AccessLevelConverter;
import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.entity.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// Auditada junto com SecurityEntity: o Envers exige a anotação em cada subclasse.
@org.hibernate.envers.Audited
@Entity
@DiscriminatorValue("SEGURANCA_PERFIL")
@Getter
public class ProfileEntity extends SecurityEntity {

    /**
     * O nível que este perfil confere a quem o tem.
     *
     * <p>Mora aqui, e não no grupo, porque o perfil é <b>um por usuário</b> — e um piso ordinal
     * precisa de valor único. Um usuário pertence a vários grupos, e escolher entre o maior e o
     * menor nível deles seria arbitrário nos dois sentidos.
     *
     * <p>Nulo significa <b>sem nível</b>, e é como todo perfil existente nasce. O que acontece então
     * é decidido por {@code archbase.security.access-level.default}. Quem modela senioridade fora do
     * perfil implementa {@code ArchbaseAccessLevelResolver}.
     */
    @Setter
    @Convert(converter = AccessLevelConverter.class)
    @Column(name = "ACCESS_LEVEL", nullable = true, length = 30)
    private AccessLevel accessLevel;

    public ProfileEntity() {
        // Default empty constructor
    }

    @Builder
    public ProfileEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, String name, String description, Set<ActionEntity> actions, AccessLevel accessLevel) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId, name, description);
        this.accessLevel = accessLevel;
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
        profileEntity.setAccessLevel(profile.getAccessLevel());

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
                .accessLevel(this.getAccessLevel())
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
                .accessLevel(this.getAccessLevel())
                .build();
    }
}
