package it.brunasti.mitire.backend.web.dto;

public record ProjectEntryStatusDto(Long id, Long projectId, String name, int sequence, boolean active,
                                      boolean startingStatus, String description) {
}
