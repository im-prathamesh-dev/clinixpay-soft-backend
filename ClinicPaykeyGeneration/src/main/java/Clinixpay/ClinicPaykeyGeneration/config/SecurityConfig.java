package Clinixpay.ClinicPaykeyGeneration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity // Optional, but good practice
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for non-browser API access
                .csrf(AbstractHttpConfigurer::disable)
                // 2. Configure Authorization
                .authorizeHttpRequests(auth -> auth
                        // Permit ALL access to the registration endpoint
                        .requestMatchers("/api/register/user").permitAll()
                        // Require authentication for any other path (default)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}