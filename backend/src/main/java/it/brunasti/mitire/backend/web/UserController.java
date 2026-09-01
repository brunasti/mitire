package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.ChangeOwnPasswordRequest;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.UpdateOwnProfileRequest;
import it.brunasti.mitire.backend.web.dto.UpdatePasswordRequest;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @GetMapping("/me")
    public UserDto getCurrentUser(Authentication authentication) {
        return userService.getByUsername(authentication.getName());
    }

    @PutMapping("/me")
    public UserDto updateOwnProfile(Authentication authentication, @Valid @RequestBody UpdateOwnProfileRequest request) {
        return userService.updateOwnProfile(authentication.getName(), request);
    }

    @PutMapping("/me/password")
    public UserDto changeOwnPassword(Authentication authentication, @Valid @RequestBody ChangeOwnPasswordRequest request) {
        return userService.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
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

    @DeleteMapping("/{id}/groups/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto removeFromGroup(@PathVariable Long id, @PathVariable Long groupId) {
        return userService.removeFromGroup(id, groupId);
    }
}
