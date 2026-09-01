package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.repository.GroupRepository;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.web.dto.CreateGroupRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateGroupRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final ProjectRepository projectRepository;

    public GroupService(GroupRepository groupRepository, ProjectRepository projectRepository) {
        this.groupRepository = groupRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupDto> findAll() {
        return groupRepository.findAll().stream().map(this::toDto).toList();
    }

    public GroupDto create(CreateGroupRequest request) {
        if (groupRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("A group named '" + request.name() + "' already exists");
        }
        Group group = new Group();
        group.setName(request.name());
        group.setProjects(resolveProjects(request.projectIds()));
        return toDto(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public GroupDto findById(Long id) {
        return toDto(getReference(id));
    }

    public GroupDto update(Long id, UpdateGroupRequest request) {
        Group group = getReference(id);
        group.setName(request.name());
        group.setProjects(resolveProjects(request.projectIds()));
        return toDto(groupRepository.save(group));
    }

    public GroupDto addProject(Long groupId, Long projectId) {
        Group group = getReference(groupId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project " + projectId + " not found"));
        group.getProjects().add(project);
        return toDto(groupRepository.save(group));
    }

    public GroupDto removeProject(Long groupId, Long projectId) {
        Group group = getReference(groupId);
        group.getProjects().removeIf(project -> project.getId().equals(projectId));
        return toDto(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<GroupDto> findByProject(Long projectId) {
        return groupRepository.findAll().stream()
                .filter(group -> group.getProjects().stream().anyMatch(project -> project.getId().equals(projectId)))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findProjectsForGroup(Long groupId) {
        if (groupId == null) {
            return List.of();
        }
        return getReference(groupId).getProjects().stream()
                .map(this::toProjectDto)
                .sorted(Comparator.comparing(ProjectDto::code))
                .toList();
    }

    Group getReference(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group " + id + " not found"));
    }

    private Set<Project> resolveProjects(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(projectRepository.findAllById(projectIds));
    }

    GroupDto toDto(Group group) {
        List<ProjectDto> projects = group.getProjects().stream()
                .map(this::toProjectDto)
                .sorted(Comparator.comparing(ProjectDto::code))
                .toList();
        return new GroupDto(group.getId(), group.getName(), projects);
    }

    private ProjectDto toProjectDto(Project project) {
        return new ProjectDto(project.getId(), project.getCode(), project.getName(), project.isActive());
    }
}
