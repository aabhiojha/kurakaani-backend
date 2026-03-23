package com.abhishekojha.kurakanimonolith.modules.user.service;

import com.abhishekojha.kurakanimonolith.common.exception.exceptions.ResourceNotFoundException;
import com.abhishekojha.kurakanimonolith.modules.auth.model.Role;
import com.abhishekojha.kurakanimonolith.modules.user.dtos.UpdateUserDto;
import com.abhishekojha.kurakanimonolith.modules.user.dtos.UserDto;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import com.abhishekojha.kurakanimonolith.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        User user = getAuthenticatedUser();
        log.debug("Fetching current user profile for userId={}", user.getId());
        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        log.debug("Fetching user by id: {}", userId);
        return toDto(findUserById(userId));
    }

    @Override
    @Transactional
    public UserDto updateCurrentUser(UpdateUserDto updateUserDto) {
        User user = getAuthenticatedUser();
        log.debug("Updating profile for userId={}", user.getId());

        if (updateUserDto.getUserName() != null && !updateUserDto.getUserName().isBlank()) {
            userRepository.findByUserName(updateUserDto.getUserName())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        log.debug("Username update rejected for userId={} because username already exists: {}", user.getId(), updateUserDto.getUserName());
                        throw new IllegalArgumentException("Username already exists.");
                    });
            user.setUserName(updateUserDto.getUserName());
            log.debug("Updated username for userId={} to {}", user.getId(), updateUserDto.getUserName());
        }

        if (updateUserDto.getEmail() != null && !updateUserDto.getEmail().isBlank()) {
            userRepository.findByEmailIgnoreCase(updateUserDto.getEmail())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        log.debug("Email update rejected for userId={} because email already exists: {}", user.getId(), updateUserDto.getEmail());
                        throw new IllegalArgumentException("Email already exists.");
                    });
            user.setEmail(updateUserDto.getEmail());
            log.debug("Updated email for userId={} to {}", user.getId(), updateUserDto.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.debug("User profile updated for userId={}", savedUser.getId());
        return toDto(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
        log.debug("Deleted user with id={}", userId);
    }

    private User getAuthenticatedUser() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        log.debug("Authenticated user resolved with id={}", user.getId());
        return user;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.debug("User lookup failed for id={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
