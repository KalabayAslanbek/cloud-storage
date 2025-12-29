package com.kalabay.cloudstorage.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public User register(String username, String rawPassword) {
        if (repo.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        User user = User.builder()
                .username(username)
                .passwordHash(encoder.encode(rawPassword))
                .build();
        try {
            return repo.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyExistsException();
        }
    }

    public boolean login(String username, String rawPassword) {
        return repo.findByUsername(username)
                .map(u -> encoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }
}