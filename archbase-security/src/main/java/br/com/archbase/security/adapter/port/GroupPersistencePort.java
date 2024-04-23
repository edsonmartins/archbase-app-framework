package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.GroupDto;

import java.util.List;
import java.util.Optional;


public interface GroupPersistencePort {

    public List<GroupDto> findAllGroups();

    public Optional<GroupDto> findGroupById(String id);

    public GroupDto createGroup(GroupDto groupDto);

    public Optional<GroupDto> updateGroup(String id, GroupDto groupDto);

    public void deleteGroup(String id) ;

    public void addPermission(String groupId, String actionId);
    public void removePermission(String permissionId);
}
