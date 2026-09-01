package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.GroupService;
import it.brunasti.mitire.backend.web.dto.CreateGroupRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupDto> findAll() {
        return groupService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GroupDto create(@Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupDto update(@PathVariable Long id, @Valid @RequestBody UpdateGroupRequest request) {
        return groupService.update(id, request);
    }
}
