package br.com.archbase.security.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ResourceRegisterDto {
    private SimpleResourceDto resource;
    private List<SimpleActionDto> actions;
}
