package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        List<GroupDto> groups
) {
}
