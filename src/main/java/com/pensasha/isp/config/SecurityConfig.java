package com.pensasha.isp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabling CSRF protection
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class) // Custom filter
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/public/**").permitAll() // Allow public access
                        .requestMatchers("/api/**").authenticated() // Secure API endpoints
                )
                .httpBasic(Customizer.withDefaults()); // Use HTTP Basic Authentication
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http.build();

    }

}
