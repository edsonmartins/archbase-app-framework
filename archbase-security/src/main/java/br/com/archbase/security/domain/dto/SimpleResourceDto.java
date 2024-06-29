package br.com.archbase.security.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleResourceDto {
    private String resourceName;
    private String resourceDescription;
}
