package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.TimeEntry;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;
    private final ProjectService projectService;

    public TimeEntryService(TimeEntryRepository timeEntryRepository, UserService userService, ProjectService projectService) {
        this.timeEntryRepository = timeEntryRepository;
        this.userService = userService;
        this.projectService = projectService;
    }

    public TimeEntryDto create(CreateTimeEntryRequest request) {
        User user = userService.getReference(request.userId());
        Project project = projectService.getReference(request.projectId());

        if (!canAccess(user, project)) {
            throw new AccessDeniedException(
                    "User '" + user.getUsername() + "' does not have access to project '" + project.getCode() + "'");
        }

        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setProject(project);
        entry.setWorkDate(request.workDate());
        entry.setHours(request.hours());
        entry.setDescription(request.description());

        return toDto(timeEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDto> search(Long userId, Long projectId, LocalDate from, LocalDate to) {
        Specification<TimeEntry> spec = Specification.allOf();
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (projectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("project").get("id"), projectId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("workDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("workDate"), to));
        }
        return timeEntryRepository.findAll(spec).stream().map(this::toDto).toList();
    }

    private boolean canAccess(User user, Project project) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getGroups().stream().anyMatch(group -> group.getProjects().contains(project));
    }

    private TimeEntryDto toDto(TimeEntry entry) {
        return new TimeEntryDto(
                entry.getId(),
                entry.getUser().getId(),
                entry.getUser().getUsername(),
                entry.getProject().getId(),
                entry.getProject().getCode(),
                entry.getWorkDate(),
                entry.getHours(),
                entry.getDescription(),
                entry.getStatus()
        );
    }
}
