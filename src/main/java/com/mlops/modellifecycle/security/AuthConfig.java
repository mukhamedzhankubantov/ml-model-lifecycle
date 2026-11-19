package com.mlops.modellifecycle.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Checks the name and the BCrypt hash at login. Prod ships no users in this lab,
     * so without a UserDetailsService every login is the same 401.
     */
    @Bean
    public AuthenticationManager authenticationManager(ObjectProvider<UserDetailsService> users,
                                                       PasswordEncoder encoder) {
        UserDetailsService service = users.getIfAvailable();
        if (service == null) {
            return authentication -> {
                throw new BadCredentialsException("no users configured");
            };
        }
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
