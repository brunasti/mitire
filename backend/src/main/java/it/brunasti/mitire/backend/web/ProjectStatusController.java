package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.ProjectStatusService;
import it.brunasti.mitire.backend.web.dto.CreateProjectStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/statuses")
public class ProjectStatusController {

    private final ProjectStatusService projectStatusService;

    public ProjectStatusController(ProjectStatusService projectStatusService) {
        this.projectStatusService = projectStatusService;
    }

    @GetMapping
    public List<ProjectStatusDto> findByProject(@PathVariable Long projectId) {
        return projectStatusService.findByProject(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectStatusDto create(@PathVariable Long projectId, @Valid @RequestBody CreateProjectStatusRequest request) {
        return projectStatusService.create(projectId, request);
    }

    @PutMapping("/{statusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectStatusDto update(@PathVariable Long projectId, @PathVariable Long statusId,
                                    @Valid @RequestBody UpdateProjectStatusRequest request) {
        return projectStatusService.update(projectId, statusId, request);
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectStatusService.delete(projectId, statusId);
    }

    @PutMapping("/{statusId}/move-up")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectStatusDto> moveUp(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectStatusService.moveUp(projectId, statusId);
        return projectStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/move-down")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectStatusDto> moveDown(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectStatusService.moveDown(projectId, statusId);
        return projectStatusService.findByProject(projectId);
    }
}
