package com.oinkvalley.auth_svc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** {@code Authorization: Bearer} 만 검증한다. 쿠키에 실린 JWT는 내려주기만 하고 읽지 않는다. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtUtil jwtUtil;

	public JwtAuthenticationFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		String token = extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (token == null || token.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			JwtUtil.ParsedJwt parsed = jwtUtil.parseToken(token);
			List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
					.map(String::trim)
					.filter(r -> !r.isEmpty())
					.map(r -> new SimpleGrantedAuthority(toAuthority(r)))
					.toList();
			var auth = new UsernamePasswordAuthenticationToken(
					parsed.userId(),
					null,
					authorities
			);
			SecurityContextHolder.getContext().setAuthentication(auth);
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static String extractBearer(String header) {
		if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length()).trim();
	}

	/** JWT에 {@code ROLE_} 접두사 없이 오면 Spring {@code hasRole} 규약에 맞게 붙임. */
	private static String toAuthority(String raw) {
		if (raw.startsWith("ROLE_")) {
			return raw;
		}
		return "ROLE_" + raw;
	}
}
