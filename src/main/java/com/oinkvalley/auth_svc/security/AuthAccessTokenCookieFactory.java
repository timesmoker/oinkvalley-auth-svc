package com.oinkvalley.auth_svc.security;

import com.oinkvalley.auth_svc.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public final class AuthAccessTokenCookieFactory {

	private final JwtProperties jwt;

	public AuthAccessTokenCookieFactory(JwtProperties jwt) {
		this.jwt = jwt;
	}

	public ResponseCookie issue(String jwt, long maxAgeSeconds) {
		return baseBuilder(jwt, maxAgeSeconds).build();
	}

	public ResponseCookie clear() {
		return baseBuilder("", 0).build();
	}

	private ResponseCookie.ResponseCookieBuilder baseBuilder(String value, long maxAgeSeconds) {
		var c = jwt.getCookie();
		ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(c.getName(), value)
				.httpOnly(true)
				.secure(c.isSecure())
				.path(c.getPath())
				.maxAge(maxAgeSeconds)
				.sameSite(c.getSameSite());
		String domain = jwt.cookieDomainTrimmed();
		if (!domain.isEmpty()) {
			b = b.domain(domain);
		}
		return b;
	}
}
