package br.com.archbase.security.controller;


import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityController {

    @Autowired
    private ActionService actionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ResourceService resourceService;
    @Autowired
    private UserService userService;


    /**
     * ACTIONS
     */

    @GetMapping("/actions")
    public ResponseEntity<List<ActionDto>> getAllActions() {
        return ResponseEntity.ok(actionService.findAllActions());
    }

    @GetMapping("/actions/resource/{resourceId}")
    public ResponseEntity<List<ActionDto>> getActionsByResource(@PathVariable String resourceId) {
        return ResponseEntity.ok(actionService.getAllActionsByResource(resourceId));
    }

    @GetMapping("/actions/{id}")
    public ResponseEntity<ActionDto> getActionById(@PathVariable String id) {
        Optional<ActionDto> action = actionService.findActionById(id);
        return action.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/actions")
    public ResponseEntity<ActionDto> createAction(@RequestBody ActionDto actionDto) {
        ActionDto createdAction = actionService.createAction(actionDto);
        return ResponseEntity.ok(createdAction);
    }

    @PutMapping("/actions/{id}")
    public ResponseEntity<ActionDto> updateAction(@PathVariable String id, @RequestBody ActionDto actionDto) {
        Optional<ActionDto> updatedAction = actionService.updateAction(id, actionDto);
        return updatedAction.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/actions/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable String id) {
        actionService.deleteAction(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PROFILES
     */

    @GetMapping("/profiles")
    public ResponseEntity<List<ProfileDto>> getAllProfiles() {
        return ResponseEntity.ok(profileService.findAllProfiles());
    }

    @GetMapping("/profiles/{id}")
    public ResponseEntity<ProfileDto> getProfileById(@PathVariable String id) {
        Optional<ProfileDto> profile = profileService.findProfileById(id);
        return profile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/profiles")
    public ResponseEntity<ProfileDto> createProfile(@RequestBody ProfileDto profileDto) {
        ProfileDto createdProfile = profileService.createProfile(profileDto);
        return ResponseEntity.ok(createdProfile);
    }

    @PutMapping("/profiles/{id}")
    public ResponseEntity<ProfileDto> updateProfile(@PathVariable String id, @RequestBody ProfileDto profileDto) {
        Optional<ProfileDto> updatedProfile = profileService.updateProfile(id, profileDto);
        return updatedProfile.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GROUPS
     */

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.findAllGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDto> getGroupById(@PathVariable String id) {
        Optional<GroupDto> group = groupService.findGroupById(id);
        return group.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupDto> createGroup(@RequestBody GroupDto groupDto) {
        GroupDto createdGroup = groupService.createGroup(groupDto);
        return ResponseEntity.ok(createdGroup);
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<GroupDto> updateGroup(@PathVariable String id, @RequestBody GroupDto groupDto) {
        Optional<GroupDto> updatedGroup = groupService.updateGroup(id, groupDto);
        return updatedGroup.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * RESOURCES
     */
    @GetMapping("/resources")
    public ResponseEntity<List<ResourceDto>> getAllResources() {
        return ResponseEntity.ok(resourceService.findAllResources());
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ResourceDto> getResourceById(@PathVariable String id) {
        Optional<ResourceDto> resource = resourceService.findResourceById(id);
        return resource.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/resources")
    public ResponseEntity<ResourceDto> createResource(@RequestBody ResourceDto resourceDto) {
        ResourceDto createdResource = resourceService.createResource(resourceDto);
        return ResponseEntity.ok(createdResource);
    }

    @PutMapping("/resources/{id}")
    public ResponseEntity<ResourceDto> updateResource(@PathVariable String id, @RequestBody ResourceDto resourceDto) {
        Optional<ResourceDto> updatedResource = resourceService.updateResource(id, resourceDto);
        return updatedResource.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable String id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
