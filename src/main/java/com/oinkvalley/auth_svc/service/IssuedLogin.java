package com.oinkvalley.auth_svc.service;

/**
 * 로그인 시 발급된 JWT(쿠키 헤더용)와 API 본문용 메타데이터.
 */
public record IssuedLogin(String jwt, String tokenType, long expiresInSeconds) {
}
