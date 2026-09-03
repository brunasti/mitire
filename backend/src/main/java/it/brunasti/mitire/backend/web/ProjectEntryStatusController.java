package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.ProjectEntryStatusService;
import it.brunasti.mitire.backend.service.UserService;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntryStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectEntryStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntryStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/statuses")
public class ProjectEntryStatusController {

    private final ProjectEntryStatusService projectEntryStatusService;
    private final UserService userService;

    public ProjectEntryStatusController(ProjectEntryStatusService projectEntryStatusService, UserService userService) {
        this.projectEntryStatusService = projectEntryStatusService;
        this.userService = userService;
    }

    @GetMapping
    public List<ProjectEntryStatusDto> findByProject(@PathVariable Long projectId) {
        return projectEntryStatusService.findByProject(projectId);
    }

    @GetMapping("/{statusId}")
    public ProjectEntryStatusDto findById(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntryStatusService.findById(projectId, statusId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectEntryStatusDto create(@PathVariable Long projectId, @Valid @RequestBody CreateProjectEntryStatusRequest request,
                                          Authentication authentication) {
        return projectEntryStatusService.create(projectId, request, currentUserId(authentication));
    }

    @PutMapping("/{statusId}")
    public ProjectEntryStatusDto update(@PathVariable Long projectId, @PathVariable Long statusId,
                                    @Valid @RequestBody UpdateProjectEntryStatusRequest request,
                                    Authentication authentication) {
        return projectEntryStatusService.update(projectId, statusId, request, currentUserId(authentication));
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long statusId, Authentication authentication) {
        projectEntryStatusService.delete(projectId, statusId, currentUserId(authentication));
    }

    @PutMapping("/{statusId}/move-up")
    public List<ProjectEntryStatusDto> moveUp(@PathVariable Long projectId, @PathVariable Long statusId,
                                                Authentication authentication) {
        projectEntryStatusService.moveUp(projectId, statusId, currentUserId(authentication));
        return projectEntryStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/move-down")
    public List<ProjectEntryStatusDto> moveDown(@PathVariable Long projectId, @PathVariable Long statusId,
                                                  Authentication authentication) {
        projectEntryStatusService.moveDown(projectId, statusId, currentUserId(authentication));
        return projectEntryStatusService.findByProject(projectId);
    }

    @PutMapping("/{statusId}/set-starting")
    public ProjectEntryStatusDto setStarting(@PathVariable Long projectId, @PathVariable Long statusId,
                                               Authentication authentication) {
        return projectEntryStatusService.setStarting(projectId, statusId, currentUserId(authentication));
    }

    @GetMapping("/{statusId}/children")
    public List<ProjectEntryStatusDto> findChildren(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntryStatusService.findChildren(projectId, statusId);
    }

    @GetMapping("/{statusId}/parents")
    public List<ProjectEntryStatusDto> findParents(@PathVariable Long projectId, @PathVariable Long statusId) {
        return projectEntryStatusService.findParents(projectId, statusId);
    }

    @PutMapping("/{statusId}/children/{childStatusId}")
    public ProjectEntryStatusDto addChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                            @PathVariable Long childStatusId, Authentication authentication) {
        return projectEntryStatusService.addChild(projectId, statusId, childStatusId, currentUserId(authentication));
    }

    @DeleteMapping("/{statusId}/children/{childStatusId}")
    public ProjectEntryStatusDto removeChild(@PathVariable Long projectId, @PathVariable Long statusId,
                                               @PathVariable Long childStatusId, Authentication authentication) {
        return projectEntryStatusService.removeChild(projectId, statusId, childStatusId, currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return userService.getByUsername(authentication.getName()).id();
    }
}
