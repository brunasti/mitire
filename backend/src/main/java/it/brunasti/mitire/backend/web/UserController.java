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
    public List<UserDto> findAll(@RequestParam(required = false) Long projectId) {
        return projectId != null ? userService.findByProjectAccess(projectId) : userService.findAll();
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
}
