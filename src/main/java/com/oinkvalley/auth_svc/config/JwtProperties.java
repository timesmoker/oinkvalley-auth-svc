package com.oinkvalley.auth_svc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * 액세스 JWT 서명/TTL 및 로그인 응답 {@code Set-Cookie} 설정. 키는 {@code jwt.*}.
 * 기본값 여기 적지 말자 전부 resources/application.properties 에 적어두자...
 * 그리고 !!! SOT는 무 조 건 infra 폴더에 있는 값 !!!
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

	private String secret;
	private long expirationSeconds;

	@NestedConfigurationProperty
	private Cookie cookie = new Cookie();

	@Getter
	@Setter
	public static class Cookie {
		private String name;
		private boolean secure;
		private String sameSite;
		private String path;
		private String domain;
	}

	public String cookieDomainTrimmed() {
		String d = cookie.getDomain();
		if (d == null) {
			return "";
		}
		d = d.trim();
		while (d.startsWith(".")) {
			d = d.substring(1).trim();
		}
		return d;
	}
}