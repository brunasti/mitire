package it.brunasti.mitire.backend.web;

import it.brunasti.mitire.backend.service.ProjectService;
import it.brunasti.mitire.backend.web.dto.CreateProjectRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> findAll(@RequestParam(required = false) Long approverId) {
        if (approverId != null) {
            return projectService.findByApprover(approverId);
        }
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectDto findById(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectDto create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }
}
