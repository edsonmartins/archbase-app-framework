package br.com.archbase.security.controller;

import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/userProfile")
public class UserProfileController {

    private final UserProfileService profileService;

    @Autowired
    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@RequestBody ProfileDto profile)  {
        return ResponseEntity.ok(profileService.createProfile(profile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileDto> updateProfile(@PathVariable String id, @RequestBody ProfileDto profile)  {
        return ResponseEntity.ok(profileService.updateProfile(id, profile).get());
    }

    @DeleteMapping("/{id}")
    public void removeProfile(@PathVariable String id)  {
        profileService.deleteProfile(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileDto> getProfileById(@PathVariable String id) {
        try {
            ProfileDto user = profileService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProfileDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return profileService.findAll(page, size);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"page", "size", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProfileDto> findAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        return profileService.findAll(page, size, sort);
    }

    @GetMapping(
            value = {"/findAll"},
            params = {"ids"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ProfileDto> findAll(@RequestParam(required = true) List<String> ids) {
        return profileService.findAll(ids);
    }

    @GetMapping(
            value = {"/findWithFilter"},
            params = {"page", "size", "filter"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProfileDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size) {
        return profileService.findWithFilter(filter, page, size);
    }

    @GetMapping(
            value = {"/findWithFilterAndSort"},
            params = {"page", "size", "filter", "sort"}
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<ProfileDto> find(@RequestParam(value = "filter",required = true) String filter, @RequestParam(value = "page",required = true) int page, @RequestParam(value = "size",required = true) int size, @RequestParam(value = "sort",required = true) String[] sort) {
        return profileService.findWithFilter(filter, page, size, sort);
    }
}