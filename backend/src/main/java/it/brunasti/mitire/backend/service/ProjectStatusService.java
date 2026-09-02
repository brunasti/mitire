package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.ProjectStatus;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.repository.ProjectStatusRepository;
import it.brunasti.mitire.backend.repository.TimeEntryRepository;
import it.brunasti.mitire.backend.web.dto.CreateProjectStatusRequest;
import it.brunasti.mitire.backend.web.dto.ProjectStatusDto;
import it.brunasti.mitire.backend.web.dto.UpdateProjectStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProjectStatusService {

    private static final List<String> DEFAULT_STATUS_NAMES = List.of("SUBMITTED", "APPROVED", "REJECTED");

    private final ProjectStatusRepository projectStatusRepository;
    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;

    public ProjectStatusService(ProjectStatusRepository projectStatusRepository, ProjectRepository projectRepository,
                                 TimeEntryRepository timeEntryRepository) {
        this.projectStatusRepository = projectStatusRepository;
        this.projectRepository = projectRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    void seedDefaultStatuses(Project project) {
        int sequence = 1;
        for (String name : DEFAULT_STATUS_NAMES) {
            ProjectStatus status = new ProjectStatus();
            status.setProject(project);
            status.setName(name);
            status.setSequence(sequence++);
            projectStatusRepository.save(status);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectStatusDto> findByProject(Long projectId) {
        return projectStatusRepository.findByProjectIdOrderBySequence(projectId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProjectStatusDto findDefaultForProject(Long projectId) {
        return toDto(getDefaultForProject(projectId));
    }

    @Transactional(readOnly = true)
    ProjectStatus getDefaultForProject(Long projectId) {
        return projectStatusRepository.findFirstByProjectIdOrderBySequence(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " has no statuses defined"));
    }

    public ProjectStatusDto create(Long projectId, CreateProjectStatusRequest request) {
        if (projectStatusRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("A status named '" + request.name() + "' already exists for this project");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " not found"));
        int nextSequence = projectStatusRepository.findByProjectIdOrderBySequence(projectId).stream()
                .mapToInt(ProjectStatus::getSequence).max().orElse(0) + 1;

        ProjectStatus status = new ProjectStatus();
        status.setProject(project);
        status.setName(request.name());
        status.setSequence(nextSequence);
        return toDto(projectStatusRepository.save(status));
    }

    public ProjectStatusDto update(Long projectId, Long statusId, UpdateProjectStatusRequest request) {
        ProjectStatus status = getReferenceForProject(projectId, statusId);
        if (!status.getName().equals(request.name())
                && projectStatusRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("A status named '" + request.name() + "' already exists for this project");
        }
        status.setName(request.name());
        return toDto(projectStatusRepository.save(status));
    }

    public void delete(Long projectId, Long statusId) {
        ProjectStatus status = getReferenceForProject(projectId, statusId);
        if (timeEntryRepository.existsByStatusId(statusId)) {
            throw new IllegalArgumentException("Can't delete a status that is used by existing time entries");
        }
        projectStatusRepository.delete(status);
    }

    public void moveUp(Long projectId, Long statusId) {
        swapWithNeighbor(projectId, statusId, true);
    }

    public void moveDown(Long projectId, Long statusId) {
        swapWithNeighbor(projectId, statusId, false);
    }

    private void swapWithNeighbor(Long projectId, Long statusId, boolean up) {
        List<ProjectStatus> ordered = projectStatusRepository.findByProjectIdOrderBySequence(projectId);
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
        ProjectStatus current = ordered.get(index);
        ProjectStatus neighbor = ordered.get(neighborIndex);
        int currentSequence = current.getSequence();
        current.setSequence(neighbor.getSequence());
        neighbor.setSequence(currentSequence);
        projectStatusRepository.save(current);
        projectStatusRepository.save(neighbor);
    }

    ProjectStatus getReference(Long id) {
        return projectStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Status " + id + " not found"));
    }

    private ProjectStatus getReferenceForProject(Long projectId, Long statusId) {
        ProjectStatus status = getReference(statusId);
        if (!status.getProject().getId().equals(projectId)) {
            throw new NoSuchElementException("Status " + statusId + " not found for project " + projectId);
        }
        return status;
    }

    private ProjectStatusDto toDto(ProjectStatus status) {
        return new ProjectStatusDto(status.getId(), status.getProject().getId(), status.getName(), status.getSequence());
    }
}
