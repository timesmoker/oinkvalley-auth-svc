package com.oinkvalley.auth_svc.service;

import com.oinkvalley.auth_svc.db.domain.User;
import com.oinkvalley.auth_svc.db.repository.UserRepository;
import com.oinkvalley.auth_svc.dto.LoginRequest;
import com.oinkvalley.auth_svc.dto.SignUpRequest;
import com.oinkvalley.auth_svc.config.JwtProperties;
import com.oinkvalley.auth_svc.security.AuthUserPrincipal;
import com.oinkvalley.auth_svc.security.JwtTokenService;
import com.oinkvalley.auth_svc.client.UserProfileClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final UserProfileClient userProfileClient;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;
	private final TransactionTemplate transactionTemplate;

	public AuthService(
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			UserProfileClient userProfileClient,
			PasswordEncoder passwordEncoder,
			JwtProperties jwtProperties,
			PlatformTransactionManager transactionManager) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.userProfileClient = userProfileClient;
		this.passwordEncoder = passwordEncoder;
		this.jwtProperties = jwtProperties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public void signUp(SignUpRequest request) {
		String email = request.email().trim();
		String nickname = request.nickname().trim();
		if (nickname.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname is required");
		}
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateUserException("Email already registered");
		}
		if (userProfileClient.existsNickname(nickname)) {
			throw new DuplicateUserException("Nickname already taken");
		}
		User user = transactionTemplate.execute(status -> persistNewUser(email, request.password()));
		try {
			userProfileClient.createProfile(user.getId(), nickname);
		} catch (Exception e) {
			transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(user.getId()));
			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY, "Failed to create user profile", e);
		}
	}

	private User persistNewUser(String email, String password) {
		List<String> roles = new ArrayList<>();
		roles.add("TEMP_USER");
		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(password))
				.provider(null)
				.providerId(null)
				.roles(roles)
				.build();
		try {
			return userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateUserException("Email already registered");
		}
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
		return new IssuedLogin(token, jwtProperties.getExpirationSeconds());
	}

	/** JWT {@code roles} 배열에는 {@code ROLE_} 접두사 없이 넣는 합의(board handoff 6.4). */
	private static String stripRolePrefixForJwtClaim(String authority) {
		if (authority != null && authority.startsWith("ROLE_")) {
			return authority.substring(5);
		}
		return authority == null ? "" : authority;
	}
}
