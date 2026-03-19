package com.example.springboot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    @Value("${frontend.test.environment.url}")
    private String appTestEnvironmentUrl;
    @Value("${frontend.prod.environment.url}")
    private String appProdEnvironmentUrl;
    @Value("${frontend.docker.url}")
    private String appDockerEnvironmentUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(appTestEnvironmentUrl, appDockerEnvironmentUrl, appProdEnvironmentUrl)); // Angular dev server
            config.setAllowCredentials(true); // IMPORTANT for cookies
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/auth/authenticate", "/auth/register", "/booking/webhooks/stripe").permitAll()
                            .anyRequest().authenticated())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Plaintext passwords for testing
        return NoOpPasswordEncoder.getInstance();

        // For production, use:
        // return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
