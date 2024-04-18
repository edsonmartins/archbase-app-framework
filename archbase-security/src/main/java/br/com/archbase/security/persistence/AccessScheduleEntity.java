package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.AccessScheduleDto;
import br.com.archbase.security.domain.entity.AccessSchedule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name="SEGURANCA_HORARIO_ACESSO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_HORARIO_ACESSO")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_HORARIO_ACESSO"))
})
public class AccessScheduleEntity extends TenantPersistenceEntityBase {

    @Column(name = "DESCRICAO")
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ID_HORARIO_ACESSO")
    private List<AccessIntervalEntity> intervals;

    public AccessScheduleEntity() {
        // Default empty constructor
    }

    public AccessScheduleEntity(String id, String code, String description, List<AccessIntervalEntity> intervals) {
        super(id, code);
        this.description = description;
        this.intervals = intervals;
    }

    public static AccessScheduleEntity fromDomain(AccessSchedule accessSchedule) {
        if (accessSchedule == null) {
            return null;
        }

        List<AccessIntervalEntity> intervals = accessSchedule.getIntervals().stream()
                .map(AccessIntervalEntity::fromDomain)
                .collect(Collectors.toList());

        AccessScheduleEntity accessScheduleEntity = new AccessScheduleEntity();
        accessScheduleEntity.setId(accessSchedule.getId().toString());
        accessScheduleEntity.setCode(accessSchedule.getCode());
        accessScheduleEntity.setDescription(accessSchedule.getDescription());
        accessScheduleEntity.setIntervals(intervals);

        return accessScheduleEntity;
    }

    public AccessSchedule toDomain() {
        return AccessSchedule.builder()
                .id(this.getId())
                .code(this.getCode())
                .description(this.getDescription())
                .intervals(this.getIntervals() != null ? this.getIntervals().stream().map(AccessIntervalEntity::toDomain).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public AccessScheduleDto toDto() {
        return AccessScheduleDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .description(this.getDescription())
                .intervals(this.getIntervals() != null ? this.getIntervals().stream().map(AccessIntervalEntity::toDto).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }
}
