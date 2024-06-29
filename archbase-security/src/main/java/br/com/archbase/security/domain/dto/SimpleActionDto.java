package br.com.archbase.security.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleActionDto {
    private String actionName;
    private String actionDescription;
}
