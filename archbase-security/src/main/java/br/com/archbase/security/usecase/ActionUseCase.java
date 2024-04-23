package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.ActionDto;


import java.util.List;
import java.util.Optional;

public interface ActionUseCase {

    public List<ActionDto> findAllActions();

    public List<ActionDto> getAllActionsByResource(String resourceId);

    public Optional<ActionDto> findActionById(String id);

    public Optional<ActionDto> findActionByName(String name, String resourceId);

    public ActionDto createAction(ActionDto actionDto);

    public Optional<ActionDto> updateAction(String id, ActionDto actionDto);

    public void deleteAction(String id);
}
