package br.com.archbase.security.domain.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleResourceDto {
    private String resourceName;
    private String resourceDescription;
}
