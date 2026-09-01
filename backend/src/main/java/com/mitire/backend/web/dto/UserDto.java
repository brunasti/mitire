package com.mitire.backend.web.dto;

import com.mitire.backend.domain.Role;

public record UserDto(Long id, String username, String fullName, String email, Role role, boolean enabled) {
}
