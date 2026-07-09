package com.oinkvalley.auth_svc.service;

import com.oinkvalley.auth_svc.db.domain.User;
import com.oinkvalley.auth_svc.db.repository.UserRepository;
import com.oinkvalley.auth_svc.dto.LoginRequest;
import com.oinkvalley.auth_svc.dto.SignUpRequest;
import com.oinkvalley.auth_svc.config.GuestProperties;
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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

	private static final String GUEST_ROLE = "GUEST";
	private static final int GUEST_CREATE_MAX_ATTEMPTS = 20;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final UserProfileClient userProfileClient;
	private final PasswordEncoder passwordEncoder;
	private final JwtProperties jwtProperties;
	private final GuestProperties guestProperties;
	private final TransactionTemplate transactionTemplate;

	public AuthService(
			AuthenticationManager authenticationManager,
			JwtTokenService jwtTokenService,
			UserRepository userRepository,
			UserProfileClient userProfileClient,
			PasswordEncoder passwordEncoder,
			JwtProperties jwtProperties,
			GuestProperties guestProperties,
			PlatformTransactionManager transactionManager) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.userProfileClient = userProfileClient;
		this.passwordEncoder = passwordEncoder;
		this.jwtProperties = jwtProperties;
		this.guestProperties = guestProperties;
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
		return persistUser(email, password, "TEMP_USER");
	}

	private User persistUser(String email, String password, String role) {
		List<String> roles = new ArrayList<>();
		roles.add(role);
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

	/**
	 * 게스트 계정 생성 + JWT 발급. 비활성이거나 생성에 실패하면 {@code Optional.empty()} — 호출부는 조용히 넘어간다.
	 */
	public Optional<IssuedLogin> guestLogin() {
		if (!guestProperties.isEnabled() || guestProperties.emailDomainTrimmed().isEmpty()) {
			return Optional.empty();
		}
		try {
			User guest = createGuestUser();
			String token = jwtTokenService.issueAccessToken(guest.getId(), List.of(GUEST_ROLE));
			return Optional.of(new IssuedLogin(token, jwtProperties.getExpirationSeconds()));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private User createGuestUser() {
		for (int attempt = 0; attempt < GUEST_CREATE_MAX_ATTEMPTS; attempt++) {
			String code = String.format("%05d", SECURE_RANDOM.nextInt(100_000));
			String email = "guest_" + code + "@" + guestProperties.emailDomainTrimmed();
			String nickname = "손님_" + code;
			if (userRepository.existsByEmail(email) || userProfileClient.existsNickname(nickname)) {
				continue;
			}
			final User guest;
			try {
				// 비밀번호는 스키마 제약용 일회성 난수 — 발급 즉시 폐기, 게스트는 비밀번호 로그인 불가
				guest = transactionTemplate.execute(status ->
						persistUser(email, UUID.randomUUID().toString(), GUEST_ROLE));
			} catch (DuplicateUserException e) {
				continue;
			}
			try {
				userProfileClient.createProfile(guest.getId(), nickname);
			} catch (Exception e) {
				transactionTemplate.executeWithoutResult(status -> userRepository.deleteById(guest.getId()));
				throw new IllegalStateException("Failed to create guest profile", e);
			}
			return guest;
		}
		throw new IllegalStateException("Failed to allocate guest identifier");
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
		// 게스트 계정은 이메일/비밀번호 로그인 불가 — 존재 여부 노출 없이 동일 오류
		boolean isGuest = principal.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.map(AuthService::stripRolePrefixForJwtClaim)
				.anyMatch(GUEST_ROLE::equals);
		if (isGuest) {
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
