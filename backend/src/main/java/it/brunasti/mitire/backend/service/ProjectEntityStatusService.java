package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.ProjectEntityStatus;
import it.brunasti.mitire.backend.domain.ProjectEntityStatusTransition;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.repository.ProjectEntityStatusRepository;
import it.brunasti.mitire.backend.repository.ProjectEntityStatusTransitionRepository;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.repository.UserRepository;
import it.brunasti.mitire.backend.web.dto.CreateProjectEntityStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectEntityStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectEntityStatusRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProjectEntityStatusService {

    private static final List<String> DEFAULT_STATUS_NAMES = List.of("SUBMITTED", "APPROVED", "REJECTED");

    private final ProjectEntityStatusRepository projectStatusRepository;
    private final ProjectEntityStatusTransitionRepository transitionRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;

    public ProjectEntityStatusService(ProjectEntityStatusRepository projectStatusRepository,
                                 ProjectEntityStatusTransitionRepository transitionRepository,
                                 ProjectRepository projectRepository,
                                 TimeEntryRepository timeEntryRepository,
                                 UserRepository userRepository) {
        this.projectStatusRepository = projectStatusRepository;
        this.transitionRepository = transitionRepository;
        this.projectRepository = projectRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
    }

    void seedDefaultStatuses(Project project) {
        for (int i = 0; i < DEFAULT_STATUS_NAMES.size(); i++) {
            ProjectEntityStatus status = new ProjectEntityStatus();
            status.setProject(project);
            status.setName(DEFAULT_STATUS_NAMES.get(i));
            status.setSequence(i + 1);
            status.setActive(true);
            status.setStartingStatus(i == 0);
            projectStatusRepository.save(status);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectEntityStatusDto> findByProject(Long projectId) {
        return projectStatusRepository.findByProjectIdOrderBySequence(projectId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProjectEntityStatusDto findDefaultForProject(Long projectId) {
        return toDto(getDefaultForProject(projectId));
    }

    @Transactional(readOnly = true)
    public ProjectEntityStatusDto findById(Long projectId, Long statusId) {
        return toDto(getReferenceForProject(projectId, statusId));
    }

    @Transactional(readOnly = true)
    public ProjectEntityStatusDto findById(Long statusId) {
        return toDto(getReference(statusId));
    }

    @Transactional(readOnly = true)
    public List<ProjectEntityStatusDto> findChildren(Long projectId, Long statusId) {
        getReferenceForProject(projectId, statusId);
        return transitionRepository.findByParentStatusId(statusId).stream()
                .map(ProjectEntityStatusTransition::getChildStatus)
                .sorted(Comparator.comparingInt(ProjectEntityStatus::getSequence))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectEntityStatusDto> findParents(Long projectId, Long statusId) {
        getReferenceForProject(projectId, statusId);
        return transitionRepository.findByChildStatusId(statusId).stream()
                .map(ProjectEntityStatusTransition::getParentStatus)
                .sorted(Comparator.comparingInt(ProjectEntityStatus::getSequence))
                .map(this::toDto)
                .toList();
    }

    public ProjectEntityStatusDto addChild(Long projectId, Long statusId, Long childStatusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        ProjectEntityStatus parent = getReferenceForProject(projectId, statusId);
        ProjectEntityStatus child = getReferenceForProject(projectId, childStatusId);
        if (parent.getId().equals(child.getId())) {
            throw new IllegalArgumentException("A status can't depend on itself");
        }
        if (transitionRepository.findByParentStatusIdAndChildStatusId(statusId, childStatusId).isEmpty()) {
            ProjectEntityStatusTransition transition = new ProjectEntityStatusTransition();
            transition.setParentStatus(parent);
            transition.setChildStatus(child);
            transitionRepository.save(transition);
        }
        return toDto(parent);
    }

    public ProjectEntityStatusDto removeChild(Long projectId, Long statusId, Long childStatusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        ProjectEntityStatus parent = getReferenceForProject(projectId, statusId);
        transitionRepository.findByParentStatusIdAndChildStatusId(statusId, childStatusId)
                .ifPresent(transitionRepository::delete);
        return toDto(parent);
    }

    @Transactional(readOnly = true)
    ProjectEntityStatus getDefaultForProject(Long projectId) {
        return projectStatusRepository.findByProjectIdAndStartingStatusTrue(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " has no starting status defined"));
    }

    public ProjectEntityStatusDto create(Long projectId, CreateProjectEntityStatusRequest request, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        if (projectStatusRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("A status named '" + request.name() + "' already exists for this project");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " not found"));
        int nextSequence = projectStatusRepository.findByProjectIdOrderBySequence(projectId).stream()
                .mapToInt(ProjectEntityStatus::getSequence).max().orElse(0) + 1;

        ProjectEntityStatus status = new ProjectEntityStatus();
        status.setProject(project);
        status.setName(request.name());
        status.setDescription(request.description());
        status.setSequence(nextSequence);
        status.setActive(true);
        status.setStartingStatus(false);
        return toDto(projectStatusRepository.save(status));
    }

    public ProjectEntityStatusDto update(Long projectId, Long statusId, UpdateProjectEntityStatusRequest request,
                                          Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        ProjectEntityStatus status = getReferenceForProject(projectId, statusId);
        if (!status.getName().equals(request.name())
                && projectStatusRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("A status named '" + request.name() + "' already exists for this project");
        }
        status.setName(request.name());
        status.setDescription(request.description());
        status.setActive(request.active());
        return toDto(projectStatusRepository.save(status));
    }

    public ProjectEntityStatusDto setStarting(Long projectId, Long statusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        ProjectEntityStatus status = getReferenceForProject(projectId, statusId);
        for (ProjectEntityStatus other : projectStatusRepository.findByProjectIdOrderBySequence(projectId)) {
            if (other.isStartingStatus() && !other.getId().equals(statusId)) {
                other.setStartingStatus(false);
                projectStatusRepository.save(other);
            }
        }
        status.setStartingStatus(true);
        return toDto(projectStatusRepository.save(status));
    }

    public void delete(Long projectId, Long statusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        ProjectEntityStatus status = getReferenceForProject(projectId, statusId);
        if (status.isStartingStatus()) {
            throw new IllegalArgumentException("Can't delete the starting status — set another status as starting first");
        }
        if (timeEntryRepository.existsByStatusId(statusId)) {
            throw new IllegalArgumentException("Can't delete a status that is used by existing time entries");
        }
        projectStatusRepository.delete(status);
    }

    public void moveUp(Long projectId, Long statusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        swapWithNeighbor(projectId, statusId, true);
    }

    public void moveDown(Long projectId, Long statusId, Long requestingUserId) {
        requireWorkflowEditAccess(projectId, requestingUserId);
        swapWithNeighbor(projectId, statusId, false);
    }

    private void swapWithNeighbor(Long projectId, Long statusId, boolean up) {
        List<ProjectEntityStatus> ordered = projectStatusRepository.findByProjectIdOrderBySequence(projectId);
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(statusId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new NoSuchElementException("Status " + statusId + " not found for project " + projectId);
        }
        int neighborIndex = up ? index - 1 : index + 1;
        if (neighborIndex < 0 || neighborIndex >= ordered.size()) {
            return;
        }
        ProjectEntityStatus current = ordered.get(index);
        ProjectEntityStatus neighbor = ordered.get(neighborIndex);
        int currentSequence = current.getSequence();
        current.setSequence(neighbor.getSequence());
        neighbor.setSequence(currentSequence);
        projectStatusRepository.save(current);
        projectStatusRepository.save(neighbor);
    }

    ProjectEntityStatus getReference(Long id) {
        return projectStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Status " + id + " not found"));
    }

    private ProjectEntityStatus getReferenceForProject(Long projectId, Long statusId) {
        ProjectEntityStatus status = getReference(statusId);
        if (!status.getProject().getId().equals(projectId)) {
            throw new NoSuchElementException("Status " + statusId + " not found for project " + projectId);
        }
        return status;
    }

    /**
     * Editing a project's workflow (its statuses and their dependencies) is restricted to
     * ADMIN or the project's own Owner — a plain member with access to the project can't.
     */
    private void requireWorkflowEditAccess(Long projectId, Long requestingUserId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new NoSuchElementException("User " + requestingUserId + " not found"));
        if (requester.getRole() == Role.ADMIN) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " not found"));
        User owner = project.getOwner();
        if (owner == null || !owner.getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Only the project owner or an ADMIN can edit the workflow");
        }
    }

    private ProjectEntityStatusDto toDto(ProjectEntityStatus status) {
        return new ProjectEntityStatusDto(status.getId(), status.getProject().getId(), status.getName(),
                status.getSequence(), status.isActive(), status.isStartingStatus(), status.getDescription());
    }
}
