package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation
 * Loads user-specific data for Spring Security authentication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[USER_LOAD] Loading user by username - username={}", username);

        UserDetails user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[USER_LOAD] User not found - username={}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.debug("[USER_LOAD] User loaded successfully - username={}, authorities={}",
                username, user.getAuthorities());

        return user;
    }
}
