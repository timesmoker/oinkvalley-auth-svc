package com.oinkvalley.auth_svc.dto;

/**
 * 로그인 HTTP 본문. 액세스 JWT는 {@code Set-Cookie} HttpOnly로만 내려간다(본문에 토큰 문자열 없음).
 * API 검증은 {@code Authorization: Bearer}만 사용한다.
 */
public record LoginResponse(
		String tokenType,
		long expiresInSeconds
) {
}
