package com.oinkvalley.auth_svc.config;

import com.oinkvalley.auth_svc.security.JwtPrincipalConverter;
import com.oinkvalley.auth_svc.security.SecurityJsonHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtPrincipalConverter jwtPrincipalConverter;
	private final SecurityJsonHandlers securityJsonHandlers;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) ->
								securityJsonHandlers.writeUnauthorized(response))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								securityJsonHandlers.writeForbidden(response))
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/health").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/guest").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
						.requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
						.anyRequest().denyAll()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtPrincipalConverter))
						.authenticationEntryPoint((request, response, authException) ->
								securityJsonHandlers.writeInvalidToken(response))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								securityJsonHandlers.writeForbidden(response))
				)
				.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
			throws Exception {
		return configuration.getAuthenticationManager();
	}
}
