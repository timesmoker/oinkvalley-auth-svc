package com.oinkvalley.auth_svc.controller;

import com.oinkvalley.auth_svc.dto.LoginRequest;
import com.oinkvalley.auth_svc.dto.MeResponse;
import com.oinkvalley.auth_svc.dto.SignUpRequest;
import com.oinkvalley.auth_svc.security.AuthAccessTokenCookieFactory;
import com.oinkvalley.auth_svc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthAccessTokenCookieFactory accessTokenCookieFactory;

	@PostMapping("/signup")
	public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest body) {
		authService.signUp(body);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/login")
	public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest body) {
		var issued = authService.login(body);
		var cookie = accessTokenCookieFactory.issue(
				issued.jwt(),
				issued.expiresInSeconds()
		);
	
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		var clear = accessTokenCookieFactory.clear();
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, clear.toString())
				.build();
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal Long userId) {
		return new MeResponse(userId);
	}
}
