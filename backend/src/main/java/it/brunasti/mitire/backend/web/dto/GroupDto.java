package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;

import java.util.List;

public record GroupDto(Long id, String name, Role role, List<ProjectDto> projects) {
}
