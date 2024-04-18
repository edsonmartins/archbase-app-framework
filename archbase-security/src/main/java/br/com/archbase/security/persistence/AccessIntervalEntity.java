package br.com.archbase.security.persistence;

import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.security.domain.dto.AccessIntervalDto;
import br.com.archbase.security.domain.entity.AccessInterval;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="SEGURANCA_INTERVALO_ACESSO")
@Getter
@Setter
@AttributeOverrides({
        @AttributeOverride(name="id",
                column=@Column(name="ID_INTERVALO_ACESSO")),
        @AttributeOverride(name="code",
                column=@Column(name="CD_INTERVALO_ACESSO"))
})
public class AccessIntervalEntity extends TenantPersistenceEntityBase {

    @ManyToOne
    @JoinColumn(name = "ID_HORARIO_ACESSO")
    private AccessScheduleEntity accessSchedule;

    @Column(name = "DIA_DA_SEMANA")
    private Long dayOfWeek;

    @Column(name = "HORA_INICIAL")
    private String startTime;

    @Column(name = "HORA_FINAL")
    private String endTime;

    public AccessIntervalEntity() {
        // Default empty constructor
    }

    @Builder
    public AccessIntervalEntity(String id, String code, Long version, LocalDateTime createEntityDate, String createdByUser, LocalDateTime updateEntityDate, String lastModifiedByUser, String tenantId, AccessScheduleEntity accessSchedule, Long dayOfWeek, String startTime, String endTime) {
        super(id, code, version, createEntityDate, createdByUser, updateEntityDate, lastModifiedByUser, tenantId);
        this.accessSchedule = accessSchedule;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AccessIntervalEntity fromDomain(AccessInterval accessInterval) {
        if (accessInterval == null) {
            return null;
        }

        AccessScheduleEntity accessScheduleEntity = AccessScheduleEntity.fromDomain(accessInterval.getAccessSchedule());

        AccessIntervalEntity accessIntervalEntity = new AccessIntervalEntity();
        accessIntervalEntity.setId(accessInterval.getId().toString());
        accessIntervalEntity.setCode(accessInterval.getCode());
        accessIntervalEntity.setCreateEntityDate(accessInterval.getCreateEntityDate());
        accessIntervalEntity.setUpdateEntityDate(accessInterval.getUpdateEntityDate());
        accessIntervalEntity.setCreatedByUser(accessInterval.getCreatedByUser());
        accessIntervalEntity.setTenantId(accessIntervalEntity.getTenantId());
        accessIntervalEntity.setLastModifiedByUser(accessInterval.getLastModifiedByUser());
        accessIntervalEntity.setAccessSchedule(accessScheduleEntity);
        accessIntervalEntity.setDayOfWeek(accessInterval.getDayOfWeek());
        accessIntervalEntity.setStartTime(accessInterval.getStartTime());
        accessIntervalEntity.setEndTime(accessInterval.getEndTime());

        return accessIntervalEntity;
    }


    public AccessInterval toDomain() {
        return AccessInterval.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .accessSchedule(this.getAccessSchedule() != null ? this.getAccessSchedule().toDomain() : null)
                .dayOfWeek(this.getDayOfWeek())
                .startTime(this.getStartTime())
                .endTime(this.getEndTime())
                .build();
    }

    public AccessIntervalDto toDto() {
        return AccessIntervalDto.builder()
                .id(this.getId())
                .code(this.getCode())
                .version(this.getVersion())
                .updateEntityDate(this.getUpdateEntityDate())
                .createEntityDate(this.getCreateEntityDate())
                .createdByUser(this.getCreatedByUser())
                .lastModifiedByUser(this.getLastModifiedByUser())
                .accessSchedule(this.getAccessSchedule() != null ? this.getAccessSchedule().toDto() : null)
                .dayOfWeek(this.getDayOfWeek())
                .startTime(this.getStartTime())
                .endTime(this.getEndTime())
                .build();
    }
}
