package com.oinkvalley.auth_svc.service;

/**
 * 로그인 시 발급된 JWT(쿠키 헤더용)와 API 본문용 메타데이터.
 * 외부로 안나감 내부에서 사용하는 객체
 */
public record IssuedLogin(String jwt, long expiresInSeconds) {
}
