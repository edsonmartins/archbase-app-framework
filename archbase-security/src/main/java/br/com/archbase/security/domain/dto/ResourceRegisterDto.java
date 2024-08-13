package br.com.archbase.security.domain.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceRegisterDto {
    private SimpleResourceDto resource;
    private List<SimpleActionDto> actions;
}
