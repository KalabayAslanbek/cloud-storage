package com.kalabay.cloudstorage.security;

import com.kalabay.cloudstorage.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation.
 *
 * Loads user credentials from the database and adapts them into {@link UserDetails}
 * required by Spring Security authentication.
 *
 * All authenticated users receive {@code ROLE_USER}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    /**
     * Creates a new service instance.
     *
     * @param repo user repository used to load users by username
     */
    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    /**
     * Loads a user by username for authentication.
     *
     * @param username username to search in the database
     * @return {@link UserDetails} containing username, password hash, and {@code ROLE_USER}
     * @throws UsernameNotFoundException if user does not exist
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(user.getUsername()).password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}