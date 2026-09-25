package com.dmg.movieticketing.identity.config;

import com.dmg.movieticketing.identity.security.JwtToAccountAuthenticationConverter;
import com.dmg.movieticketing.identity.security.PasswordAuthenticationProvider;
import com.dmg.movieticketing.identity.security.SecurityProblemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(PasswordAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtToAccountAuthenticationConverter jwtConverter,
            SecurityProblemWriter problemWriter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/customers/signup",
                                "/api/v1/auth/admins/signup",
                                "/api/v1/auth/signin"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/cities",
                                "/api/v1/cities/**",
                                "/api/v1/movies",
                                "/api/v1/movies/**",
                                "/api/v1/shows",
                                "/api/v1/shows/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/movies")
                        .hasRole("THEATRE_ADMIN")
                        .requestMatchers("/api/v1/theatres", "/api/v1/theatres/**")
                        .hasRole("THEATRE_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(problemWriter::writeUnauthorized)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemWriter::writeUnauthorized)
                        .accessDeniedHandler((request, response, exception) ->
                                problemWriter.writeForbidden(request, response))
                );

        return http.build();
    }
}
