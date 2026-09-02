package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/statuses")
public class ProjectEntityStatusController {

    private final ProjectEntityStatusService projectEntityStatusService;

    public ProjectEntityStatusController(ProjectEntityStatusService projectEntityStatusService) {
        this.projectEntityStatusService = projectEntityStatusService;
    }

    @GetMapping
    public List<ProjectEntityStatusDto> findByProject(@PathVariable Long projectId) {
        return projectEntityStatusService.findByProject(projectId);
    }

    @GetMapping("/{statusId}")
    public ProjectEntityStatusDto findById(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntityStatusService.findById(projectId, statusId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectEntityStatusDto create(@PathVariable Long projectId, @Valid @RequestBody CreateProjectEntityStatusRequest request) {
        return projectEntityStatusService.create(projectId, request);
    }

    @PutMapping("/{statusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectEntityStatusDto update(@PathVariable Long projectId, @PathVariable Long statusId,
                                    @Valid @RequestBody UpdateProjectEntityStatusRequest request) {
        return projectEntityStatusService.update(projectId, statusId, request);
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectEntityStatusService.delete(projectId, statusId);
    }

    @PutMapping("/{statusId}/move-up")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectEntityStatusDto> moveUp(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectEntityStatusService.moveUp(projectId, statusId);
        return projectEntityStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/move-down")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProjectEntityStatusDto> moveDown(@PathVariable Long projectId, @PathVariable Long statusId) {
        projectEntityStatusService.moveDown(projectId, statusId);
        return projectEntityStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/set-starting")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectEntityStatusDto setStarting(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntityStatusService.setStarting(projectId, statusId);
    }

    @GetMapping("/{statusId}/children")
    public List<ProjectEntityStatusDto> findChildren(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntityStatusService.findChildren(projectId, statusId);
    }

    @PutMapping("/{statusId}/children/{childStatusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectEntityStatusDto addChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                            @PathVariable Long childStatusId) {
        return projectEntityStatusService.addChild(projectId, statusId, childStatusId);
    }

    @DeleteMapping("/{statusId}/children/{childStatusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectEntityStatusDto removeChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                               @PathVariable Long childStatusId) {
        return projectEntityStatusService.removeChild(projectId, statusId, childStatusId);
    }
}
