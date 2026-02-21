package com.kalabay.cloudstorage.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.kalabay.cloudstorage.security.jwt.JwtService;
import com.kalabay.cloudstorage.user.dto.*;

import java.util.Map;

/**
 * REST controller responsible for user authentication operations.
 *
 * Provides endpoints for:
 * - User registration
 * - User login (JWT issuance)
 *
 * Base path: {@code /api/users}
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final JwtService jwt;

    public UserController(UserService service, JwtService jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    /**
     * Registers a new user.
     *
     * Validates request payload and delegates registration to {@link UserService}.
     * Returns basic user information upon successful creation.
     *
     * @param req registration request containing username and password
     * @return map containing user ID, username and creation timestamp
     *
     * @throws ResponseStatusException with 409 CONFLICT
     *         if the username already exists
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        try {
            User u = service.register(req.username(), req.password());
            return Map.of("id", u.getId(), "username", u.getUsername(), "createdAt", u.getCreatedAt());
        } catch (UsernameAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
    }

    /**
     * Authenticates a user and issues a JWT access token.
     *
     * If credentials are valid, a signed JWT token is returned
     * along with token type and expiration time.
     *
     * @param req login request containing username and password
     * @return {@link TokenResponse} containing:
     *         - access token
     *         - token type (Bearer)
     *         - expiration time in seconds
     *
     * @throws ResponseStatusException with 401 UNAUTHORIZED
     *         if credentials are invalid
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        boolean ok = service.login(req.username(), req.password());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwt.generateToken(req.username());
        return new TokenResponse(token, "Bearer", jwt.getExpiresInSeconds());
    }
}