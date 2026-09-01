package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.UpdatePasswordRequest;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> findAll(@RequestParam(required = false) Long projectId,
                                  @RequestParam(required = false) Long groupId) {
        if (projectId != null) {
            return userService.findByProjectAccess(projectId);
        }
        if (groupId != null) {
            return userService.findByGroup(groupId);
        }
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updatePassword(@PathVariable Long id, @Valid @RequestBody UpdatePasswordRequest request) {
        return userService.updatePassword(id, request.password());
    }

    @PutMapping("/{id}/groups/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto addToGroup(@PathVariable Long id, @PathVariable Long groupId) {
        return userService.addToGroup(id, groupId);
    }
}
