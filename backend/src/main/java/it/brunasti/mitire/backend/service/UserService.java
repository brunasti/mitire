package it.brunasti.mitire.backend.service;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.GroupRepository;
import it.brunasti.mitire.backend.repository.UserRepository;
import it.brunasti.mitire.backend.web.dto.CreateUserRequest;
import it.brunasti.mitire.backend.web.dto.UpdateUserRequest;
import it.brunasti.mitire.backend.web.dto.UserDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, GroupRepository groupRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("User '" + username + "' not found"));
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
        user.setGroup(resolveGroup(request.groupId()));

        return toDto(userRepository.save(user));
    }

    public UserDto update(Long id, UpdateUserRequest request) {
        User user = getReference(id);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        user.setGroup(resolveGroup(request.groupId()));

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

    private Group resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group " + groupId + " not found"));
    }

    private UserDto toDto(User user) {
        Group group = user.getGroup();
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                group != null ? group.getId() : null,
                group != null ? group.getName() : null
        );
    }
}
