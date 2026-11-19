package com.mlops.modellifecycle.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Two classroom users for the laptop. Passwords are stored as BCrypt hashes
 * (the delegating encoder writes the {bcrypt} prefix); this profile is not active in prod.
 */
@Configuration
@Profile("dev")
public class DevUsersConfig {
    public static final String SCIENTIST_USER = "scientist";
    public static final String LEAD_USER = "lead";

    private static final String SCIENTIST_PASSWORD = "scientist-dev";
    private static final String LEAD_PASSWORD = "lead-dev";

    @Bean
    public UserDetailsService devUsers(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(SCIENTIST_USER)
                        .password(encoder.encode(SCIENTIST_PASSWORD))
                        .roles(Roles.SCIENTIST)
                        .build(),
                User.withUsername(LEAD_USER)
                        .password(encoder.encode(LEAD_PASSWORD))
                        .roles(Roles.LEAD)
                        .build());
    }
}
