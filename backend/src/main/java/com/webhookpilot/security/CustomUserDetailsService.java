package com.webhookpilot.security;

import com.webhookpilot.model.User;
import com.webhookpilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security needs a UserDetailsService to load user data during authentication.
 * This bridges our User entity with Spring Security's UserDetails interface.
 *
 * We load by ID (from JWT) not email, because after login the JWT contains the user ID.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Called by Spring Security when it needs to verify a user exists.
     * In our JWT flow, this is called from JwtAuthFilter after extracting the user ID from the token.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return buildUserDetails(user);
    }

    /**
     * Load by user ID (used in JWT filter after extracting ID from token).
     */
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())  // username = user ID (not email) for consistency with JWT subject
                .password(user.getPasswordHash())
                .authorities(Collections.emptyList())  // no roles for now
                .build();
    }
}
