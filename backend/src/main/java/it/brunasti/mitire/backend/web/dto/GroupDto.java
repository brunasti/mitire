package it.brunasti.mitire.backend.web.dto;

import java.util.List;

public record GroupDto(Long id, String name, List<ProjectDto> projects) {
}
