package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.GroupRepository;
import it.brunasti.mitire.backend.repository.UserRepository;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.GroupDto;
import it.brunasti.mitire.backend.web.dto.ProjectDto;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final ProjectService projectService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, GroupRepository groupRepository, GroupService groupService,
                        ProjectService projectService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.projectService = projectService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(getReference(id));
    }

    @Transactional(readOnly = true)
    public UserDto getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("User '" + username + "' not found"));
    }

    @Transactional(readOnly = true)
    public List<UserDto> findByProjectAccess(Long projectId) {
        return userRepository.findAll().stream()
                .filter(user -> hasAccess(user, projectId))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAccessibleProjects(Long userId) {
        User user = getReference(userId);
        if (user.getRole() == Role.ADMIN) {
            return projectService.findAll();
        }
        return user.getGroups().stream()
                .flatMap(group -> group.getProjects().stream())
                .distinct()
                .map(projectService::toDto)
                .sorted(Comparator.comparing(ProjectDto::code))
                .toList();
    }

    private boolean hasAccess(User user, Long projectId) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getGroups().stream()
                .anyMatch(group -> group.getProjects().stream().anyMatch(project -> project.getId().equals(projectId)));
    }

    @Transactional(readOnly = true)
    public List<UserDto> findByGroup(Long groupId) {
        return userRepository.findAll().stream()
                .filter(user -> user.getGroups().stream().anyMatch(group -> group.getId().equals(groupId)))
                .map(this::toDto)
                .toList();
    }

    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already in use");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setGroups(resolveGroups(request.groupIds()));

        return toDto(userRepository.save(user));
    }

    public UserDto update(Long id, UpdateUserRequest request) {
        User user = getReference(id);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        user.setGroups(resolveGroups(request.groupIds()));

        return toDto(userRepository.save(user));
    }

    public UserDto updatePassword(Long id, String newPassword) {
        User user = getReference(id);
        if (user.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Cannot change the password of an ADMIN user");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return toDto(userRepository.save(user));
    }

    User getReference(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User " + id + " not found"));
    }

    private Set<Group> resolveGroups(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(groupRepository.findAllById(groupIds));
    }

    private UserDto toDto(User user) {
        List<GroupDto> groups = user.getGroups().stream()
                .map(groupService::toDto)
                .sorted(Comparator.comparing(GroupDto::name))
                .toList();
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                groups
        );
    }
}
