package br.com.archbase.security.domain.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleActionDto {
    private String actionName;
    private String actionDescription;
}
