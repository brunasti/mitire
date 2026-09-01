package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.web.dto.CreateProjectRequest;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        return projectRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProjectDto create(CreateProjectRequest request) {
        if (projectRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("A project with code '" + request.code() + "' already exists");
        }
        Project project = new Project();
        project.setCode(request.code());
        project.setName(request.name());
        return toDto(projectRepository.save(project));
    }

    Project getReference(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project " + id + " not found"));
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(project.getId(), project.getCode(), project.getName(), project.isActive());
    }
}
