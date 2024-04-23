package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ResourceDto;

import java.util.List;
import java.util.Optional;

public interface GroupUseCase {
    public List<GroupDto> findAllGroups();

    public Optional<GroupDto> findGroupById(String id);

    public GroupDto createGroup(GroupDto groupDto);

    public Optional<GroupDto> updateGroup(String id, GroupDto groupDto);

    public void deleteGroup(String id);

    public void addPermission(String groupId, String actionId);

    public void removePermission(String permissionId);
}
