package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.repository.UserRepository;
import it.brunasti.mitire.backend.web.dto.CreateProjectRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectEntityStatusService projectEntityStatusService;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                           ProjectEntityStatusService projectEntityStatusService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectEntityStatusService = projectEntityStatusService;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findByApprover(Long approverId) {
        return projectRepository.findByApproverId(approverId).stream().map(this::toDto).toList();
    }

    public ProjectDto create(CreateProjectRequest request) {
        if (projectRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("A project with code '" + request.code() + "' already exists");
        }
        validateDateRange(request.startDate(), request.endDate());
        Project project = new Project();
        project.setCode(request.code());
        project.setName(request.name());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        Project saved = projectRepository.save(project);
        projectEntityStatusService.seedDefaultStatuses(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(Long id) {
        return toDto(getReference(id));
    }

    public ProjectDto update(Long id, UpdateProjectRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        Project project = getReference(id);
        project.setName(request.name());
        project.setActive(request.active());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setApprover(resolveApprover(id, request.approverId()));
        return toDto(projectRepository.save(project));
    }

    Project getReference(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project " + id + " not found"));
    }

    ProjectDto toDto(Project project) {
        User approver = project.getApprover();
        return new ProjectDto(project.getId(), project.getCode(), project.getName(), project.isActive(),
                project.getStartDate(), project.getEndDate(),
                approver != null ? approver.getId() : null,
                approver != null ? approver.getFullName() : null);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date can't be before start date");
        }
    }

    private User resolveApprover(Long projectId, Long approverId) {
        if (approverId == null) {
            return null;
        }
        User user = userRepository.findById(approverId)
                .orElseThrow(() -> new NoSuchElementException("User " + approverId + " not found"));
        if (!userHasAccessToProject(user, projectId)) {
            throw new IllegalArgumentException("The approver must be a user with access to this project");
        }
        return user;
    }

    private boolean userHasAccessToProject(User user, Long projectId) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getGroups().stream()
                .flatMap(group -> group.getProjects().stream())
                .anyMatch(project -> project.getId().equals(projectId));
    }
}
