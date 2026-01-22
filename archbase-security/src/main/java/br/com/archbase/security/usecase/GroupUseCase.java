package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.entity.Group;

import java.util.List;
import java.util.Optional;

public interface GroupUseCase {
    List<GroupDto> findAllGroups();

    Optional<GroupDto> findGroupById(String id);

    GroupDto createGroup(GroupDto groupDto);

    Optional<GroupDto> updateGroup(String id, GroupDto groupDto);

    void deleteGroup(String id);

    void addPermission(String groupId, String actionId);

    void removePermission(String permissionId);

    List<Group> findByNames(List<String> names);
}
