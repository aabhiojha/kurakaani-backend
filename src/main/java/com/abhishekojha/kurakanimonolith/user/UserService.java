package com.abhishekojha.kurakanimonolith.user;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser loadOrCreateOAuth2User(String email, String name) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (name != null && !name.isBlank() && !name.equals(existingUser.getName())) {
                        existingUser.setName(name);
                    }
                    return existingUser;
                })
                .orElseGet(() -> createUser(email, name));
    }

    private AppUser createUser(String email, String name) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name == null || name.isBlank() ? email : name);
        user.setRoles(Set.of(Role.ROLE_USER));
        return userRepository.save(user);
    }
}
