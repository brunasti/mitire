package com.mitire.backend.service;

import com.mitire.backend.domain.User;
import com.mitire.backend.repository.UserRepository;
import com.mitire.backend.web.dto.UserDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    User getReference(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User " + id + " not found"));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}
