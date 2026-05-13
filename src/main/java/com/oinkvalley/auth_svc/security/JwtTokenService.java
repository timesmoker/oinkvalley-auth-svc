package com.oinkvalley.auth_svc.security;

import com.oinkvalley.auth_svc.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * board-svc {@code JwtUtil}이 검증하는 HS256 JWT를 발급합니다 (서명 입력은 header.payload의 US_ASCII 바이트).
 * {@code roles}는 JSON 문자열 배열(선택). JWT에는 {@code ROLE_} 없이 {@code USER} 형태로 넣어도 됨.
 */
@Component
public class JwtTokenService {

	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

	private final byte[] secret;
	private final long expirationSeconds;

	public JwtTokenService(JwtProperties jwtProperties) {
		byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalArgumentException("jwt.secret must be at least 32 bytes (UTF-8)");
		}
		this.secret = bytes;
		this.expirationSeconds = jwtProperties.getExpirationSeconds();
	}

	public String issueAccessToken(long userId, List<String> rolesForJwt) {
		long now = Instant.now().getEpochSecond();
		long exp = now + expirationSeconds;
		String rolesJson = rolesForJwt.stream()
				.map(String::trim)
				.filter(r -> !r.isEmpty())
				.map(JwtTokenService::jsonStringLiteral)
				.collect(Collectors.joining(","));
		String payloadJson = "{\"sub\":\"" + userId + "\",\"iat\":" + now + ",\"exp\":" + exp
				+ ",\"roles\":[" + rolesJson + "]}";
		String headerPart = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
		String payloadPart = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
		String signingInput = headerPart + "." + payloadPart;
		byte[] signature = sign(signingInput);
		return signingInput + "." + base64Url(signature);
	}

	private static String jsonStringLiteral(String raw) {
		String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"");
		return "\"" + escaped + "\"";
	}

	private byte[] sign(String data) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
		} catch (Exception e) {
			throw new IllegalStateException("Could not sign JWT", e);
		}
	}

	private static String base64Url(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}
}
