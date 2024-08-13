package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.AccessInterval;
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
public class AccessIntervalDto {

	protected String id;
	protected String code;
	protected Long version;
	protected LocalDateTime createEntityDate;
	protected LocalDateTime updateEntityDate;
	protected String createdByUser;
	protected String lastModifiedByUser;
	protected AccessScheduleDto accessSchedule;
	protected Long dayOfWeek;
	protected String startTime;
	protected String endTime;

	public static AccessIntervalDto fromDomain(AccessInterval accessInterval) {
		if (accessInterval == null) {
			return null;
		}

		return AccessIntervalDto.builder()
				.id(accessInterval.getId().toString())
				.code(accessInterval.getCode())
				.version(accessInterval.getVersion())
				.createEntityDate(accessInterval.getCreateEntityDate())
				.updateEntityDate(accessInterval.getUpdateEntityDate())
				.accessSchedule(AccessScheduleDto.fromDomain(accessInterval.getAccessSchedule()))
				.createdByUser(accessInterval.getCreatedByUser())
				.lastModifiedByUser(accessInterval.getLastModifiedByUser())
				.dayOfWeek(accessInterval.getDayOfWeek())
				.startTime(accessInterval.getStartTime())
				.endTime(accessInterval.getEndTime())
				.build();
	}

	public AccessInterval toDomain() {
		return AccessInterval.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.lastModifiedByUser(this.lastModifiedByUser)
				.createdByUser(this.createdByUser)
				.accessSchedule(this.accessSchedule != null ? this.accessSchedule.toDomain(): null)
				.dayOfWeek(this.dayOfWeek)
				.startTime(this.startTime)
				.endTime(this.endTime)
				.build();
	}
}
