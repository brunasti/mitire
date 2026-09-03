package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.ProjectEntityStatus;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.TimeEntry;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.web.dto.CreateTimeEntryRequest;
import it.brunasti.mitire.backend.web.dto.TimeEntryDto;
import it.brunasti.mitire.backend.web.dto.UpdateTimeEntryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;
    private final ProjectService projectService;
    private final ProjectEntityStatusService projectEntityStatusService;

    public TimeEntryService(TimeEntryRepository timeEntryRepository, UserService userService,
                             ProjectService projectService, ProjectEntityStatusService projectEntityStatusService) {
        this.timeEntryRepository = timeEntryRepository;
        this.userService = userService;
        this.projectService = projectService;
        this.projectEntityStatusService = projectEntityStatusService;
    }

    public TimeEntryDto create(CreateTimeEntryRequest request) {
        User user = userService.getReference(request.userId());
        Project project = projectService.getReference(request.projectId());

        Role effective = Role.effectiveFor(user, project);
        if (effective == null || effective == Role.VIEWER) {
            throw new AccessDeniedException(
                    "User '" + user.getUsername() + "' does not have access to project '" + project.getCode() + "'");
        }

        validateProjectDateRange(project, request.workDate());
        validateDailyHours(user.getId(), request.workDate(), request.hours(), null);

        TimeEntry entry = new TimeEntry();
        entry.setUser(user);
        entry.setProject(project);
        entry.setWorkDate(request.workDate());
        entry.setHours(request.hours());
        entry.setDescription(request.description());
        entry.setStatus(projectEntityStatusService.getDefaultForProject(project.getId()));

        return toDto(timeEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public TimeEntryDto findByIdForUser(Long id, Long requestingUserId) {
        return toDto(getReferenceChecked(id, requestingUserId));
    }

    public TimeEntryDto update(Long id, Long requestingUserId, UpdateTimeEntryRequest request) {
        TimeEntry entry = getReferenceChecked(id, requestingUserId);
        validateDailyHours(entry.getUser().getId(), entry.getWorkDate(), request.hours(), entry.getId());
        entry.setHours(request.hours());
        entry.setDescription(request.description());
        if (request.statusId() != null) {
            User requester = userService.getReference(requestingUserId);
            if (Role.effectiveFor(requester, entry.getProject()) != Role.ADMIN) {
                throw new AccessDeniedException("Only an ADMIN can change the status of a time entry");
            }
            ProjectEntityStatus newStatus = projectEntityStatusService.getReference(request.statusId());
            if (!newStatus.getProject().getId().equals(entry.getProject().getId())) {
                throw new IllegalArgumentException("Status does not belong to this entry's project");
            }
            entry.setStatus(newStatus);
        }
        return toDto(timeEntryRepository.save(entry));
    }

    public void delete(Long id, Long requestingUserId) {
        timeEntryRepository.delete(getReferenceChecked(id, requestingUserId));
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

    private void validateProjectDateRange(Project project, LocalDate workDate) {
        if (project.getStartDate() != null && workDate.isBefore(project.getStartDate())) {
            throw new IllegalArgumentException("Work date can't be before the project's start date");
        }
        if (project.getEndDate() != null && workDate.isAfter(project.getEndDate())) {
            throw new IllegalArgumentException("Work date can't be after the project's end date");
        }
    }

    private void validateDailyHours(Long userId, LocalDate workDate, BigDecimal hours, Long excludeEntryId) {
        BigDecimal existingTotal = timeEntryRepository.findByUserIdAndWorkDate(userId, workDate).stream()
                .filter(entry -> excludeEntryId == null || !entry.getId().equals(excludeEntryId))
                .map(TimeEntry::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingTotal.add(hours).compareTo(new BigDecimal("24")) > 0) {
            throw new IllegalArgumentException("Total hours for " + workDate + " can't exceed 24");
        }
    }

    private TimeEntry getReferenceChecked(Long id, Long requestingUserId) {
        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Time entry " + id + " not found"));
        User requester = userService.getReference(requestingUserId);
        boolean isProjectAdmin = Role.effectiveFor(requester, entry.getProject()) == Role.ADMIN;
        if (!isProjectAdmin && !entry.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not have access to this time entry");
        }
        return entry;
    }

    private TimeEntryDto toDto(TimeEntry entry) {
        return new TimeEntryDto(
                entry.getId(),
                entry.getUser().getId(),
                entry.getUser().getUsername(),
                entry.getUser().getFullName(),
                entry.getProject().getId(),
                entry.getProject().getCode(),
                entry.getWorkDate(),
                entry.getHours(),
                entry.getDescription(),
                entry.getStatus().getId(),
                entry.getStatus().getName()
        );
    }
}
