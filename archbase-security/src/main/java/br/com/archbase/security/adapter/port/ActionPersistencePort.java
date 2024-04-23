package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.ActionDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ActionPersistencePort {
    public List<ActionDto> findAllActions();

    public List<ActionDto> findAllActionsByResource(String resourceId) ;

    public Optional<ActionDto> findActionById(String id);

    public Optional<ActionDto> findActionByName(String name, String resourceId) ;

    public ActionDto createAction(ActionDto actionDto) ;

    public Optional<ActionDto> updateAction(String id, ActionDto actionDto) ;

    public void deleteAction(String id) ;

}