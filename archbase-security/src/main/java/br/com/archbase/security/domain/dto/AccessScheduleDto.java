package br.com.archbase.security.domain.dto;

import br.com.archbase.security.domain.entity.AccessSchedule;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class, property = "@id")
public class AccessScheduleDto {
	protected String id;
	protected String code;
	protected Long version;
	protected LocalDateTime createEntityDate;
	protected LocalDateTime updateEntityDate;
	protected String createdByUser;
	protected String lastModifiedByUser;
	protected String description;
	protected List<AccessIntervalDto> intervals = new ArrayList<>();

	public static AccessScheduleDto fromDomain(AccessSchedule accessSchedule) {
		if (accessSchedule == null) {
			return null;
		}

		return AccessScheduleDto.builder()
				.id(accessSchedule.getId().toString())
				.code(accessSchedule.getCode())
				.version(accessSchedule.getVersion())
				.createEntityDate(accessSchedule.getCreateEntityDate())
				.updateEntityDate(accessSchedule.getUpdateEntityDate())
				.createdByUser(accessSchedule.getCreatedByUser())
				.lastModifiedByUser(accessSchedule.getLastModifiedByUser())
				.description(accessSchedule.getDescription())
				.intervals(accessSchedule.getIntervals().stream()
						.map(AccessIntervalDto::fromDomain)
						.collect(Collectors.toList()))
				.build();
	}

	public AccessSchedule toDomain() {
		return AccessSchedule.builder()
				.id(this.id)
				.code(this.code)
				.version(this.version)
				.createEntityDate(this.createEntityDate)
				.updateEntityDate(this.updateEntityDate)
				.lastModifiedByUser(this.lastModifiedByUser)
				.createdByUser(this.createdByUser)
				.description(this.description)
				.intervals(this.intervals.stream()
						.map(AccessIntervalDto::toDomain)
						.collect(Collectors.toList()))
				.build();
	}
}
