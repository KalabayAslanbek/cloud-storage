package com.kalabay.cloudstorage.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user registration and authentication logic.
 *
 * Responsibilities:
 * - Register new users with encoded passwords
 * - Validate user credentials during login
 *
 * Ensures username uniqueness and secure password hashing
 * using {@link PasswordEncoder}.
 */
@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    /**
     * Creates a new user service.
     *
     * @param repo user repository used for persistence
     * @param encoder password encoder used for hashing passwords
     */
    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /**
     * Registers a new user.
     *
     * Steps:
     * - Checks if username already exists
     * - Encodes raw password using {@link PasswordEncoder}
     * - Saves user entity to database
     *
     * Handles potential race conditions by catching
     * {@link DataIntegrityViolationException} and converting it
     * to {@link UsernameAlreadyExistsException}.
     *
     * @param username desired username
     * @param rawPassword plain text password
     * @return persisted {@link User} entity
     *
     * @throws UsernameAlreadyExistsException if username is already taken
     */
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

    /**
     * Validates user credentials.
     *
     * Compares provided raw password with stored password hash.
     *
     * @param username username to authenticate
     * @param rawPassword plain text password
     * @return true if credentials are valid, false otherwise
     */
    public boolean login(String username, String rawPassword) {
        return repo.findByUsername(username)
                .map(u -> encoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }
}