package com.kalabay.cloudstorage.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.kalabay.cloudstorage.security.jwt.JwtService;
import com.kalabay.cloudstorage.user.dto.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final JwtService jwt;

    public UserController(UserService service, JwtService jwt) {
        this.service = service;
        this.jwt = jwt;
    }

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