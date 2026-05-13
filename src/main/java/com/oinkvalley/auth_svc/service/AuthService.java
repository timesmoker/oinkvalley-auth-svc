package com.oinkvalley.auth_svc.service;

import com.oinkvalley.auth_svc.db.domain.User;
import com.oinkvalley.auth_svc.db.repository.UserRepository;
import com.oinkvalley.auth_svc.dto.LoginRequest;
import com.oinkvalley.auth_svc.dto.SignUpRequest;
import com.oinkvalley.auth_svc.dto.SignUpResponse;
import com.oinkvalley.auth_svc.config.JwtProperties;
import com.oinkvalley.auth_svc.security.AuthUserPrincipal;
import com.oinkvalley.auth_svc.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateUserException("Username already taken");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateUserException("Email already registered");
		}
		List<String> roles = new ArrayList<>();
		roles.add("USER");
		User user = User.builder()
				.email(request.email().trim())
				.username(request.username().trim())
				.passwordHash(passwordEncoder.encode(request.password()))
				.provider(null)
				.providerId(null)
				.roles(roles)
				.build();
		try {
			userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateUserException("User already exists");
		}
		return new SignUpResponse(user.getId(), user.getUsername(), user.getEmail());
	}

	public IssuedLogin login(LoginRequest request) {
		final Authentication authResult;
		try {
			authResult = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
		} catch (AuthenticationException e) {
			throw new BadCredentialsException("Invalid email or password", e);
		}
		if (!(authResult.getPrincipal() instanceof AuthUserPrincipal principal)) {
			throw new BadCredentialsException("Invalid email or password");
		}
		List<String> rolesForJwt = principal.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(AuthService::stripRolePrefixForJwtClaim)
				.filter(s -> !s.isEmpty())
				.toList();
		String token = jwtTokenService.issueAccessToken(principal.getUserId(), rolesForJwt);
		return new IssuedLogin(token, "Bearer", jwtProperties.getExpirationSeconds());
	}

	/** JWT {@code roles} 배열에는 {@code ROLE_} 접두사 없이 넣는 합의(board handoff 6.4). */
	private static String stripRolePrefixForJwtClaim(String authority) {
		if (authority != null && authority.startsWith("ROLE_")) {
			return authority.substring(5);
		}
		return authority == null ? "" : authority;
	}
}
