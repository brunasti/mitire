package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.ProjectEntityStatusService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/statuses")
public class ProjectEntityStatusController {

    private final ProjectEntityStatusService projectEntityStatusService;
    private final UserService userService;

    public ProjectEntityStatusController(ProjectEntityStatusService projectEntityStatusService, UserService userService) {
        this.projectEntityStatusService = projectEntityStatusService;
        this.userService = userService;
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
    public ProjectEntityStatusDto create(@PathVariable Long projectId, @Valid @RequestBody CreateProjectEntityStatusRequest request,
                                          Authentication authentication) {
        return projectEntityStatusService.create(projectId, request, currentUserId(authentication));
    }

    @PutMapping("/{statusId}")
    public ProjectEntityStatusDto update(@PathVariable Long projectId, @PathVariable Long statusId,
                                    @Valid @RequestBody UpdateProjectEntityStatusRequest request,
                                    Authentication authentication) {
        return projectEntityStatusService.update(projectId, statusId, request, currentUserId(authentication));
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long statusId, Authentication authentication) {
        projectEntityStatusService.delete(projectId, statusId, currentUserId(authentication));
    }

    @PutMapping("/{statusId}/move-up")
    public List<ProjectEntityStatusDto> moveUp(@PathVariable Long projectId, @PathVariable Long statusId,
                                                Authentication authentication) {
        projectEntityStatusService.moveUp(projectId, statusId, currentUserId(authentication));
        return projectEntityStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/move-down")
    public List<ProjectEntityStatusDto> moveDown(@PathVariable Long projectId, @PathVariable Long statusId,
                                                  Authentication authentication) {
        projectEntityStatusService.moveDown(projectId, statusId, currentUserId(authentication));
        return projectEntityStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/set-starting")
    public ProjectEntityStatusDto setStarting(@PathVariable Long projectId, @PathVariable Long statusId,
                                               Authentication authentication) {
        return projectEntityStatusService.setStarting(projectId, statusId, currentUserId(authentication));
    }

    @GetMapping("/{statusId}/children")
    public List<ProjectEntityStatusDto> findChildren(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntityStatusService.findChildren(projectId, statusId);
    }

    @GetMapping("/{statusId}/parents")
    public List<ProjectEntityStatusDto> findParents(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntityStatusService.findParents(projectId, statusId);
    }

    @PutMapping("/{statusId}/children/{childStatusId}")
    public ProjectEntityStatusDto addChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                            @PathVariable Long childStatusId, Authentication authentication) {
        return projectEntityStatusService.addChild(projectId, statusId, childStatusId, currentUserId(authentication));
    }

    @DeleteMapping("/{statusId}/children/{childStatusId}")
    public ProjectEntityStatusDto removeChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                               @PathVariable Long childStatusId, Authentication authentication) {
        return projectEntityStatusService.removeChild(projectId, statusId, childStatusId, currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return userService.getByUsername(authentication.getName()).id();
    }
}
