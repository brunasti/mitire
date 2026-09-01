package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;

public record UserDto(Long id, String username, String fullName, String email, Role role, boolean enabled) {
}
