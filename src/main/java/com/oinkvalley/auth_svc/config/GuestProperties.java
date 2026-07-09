package com.oinkvalley.auth_svc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 게스트 로그인 설정. 키는 {@code guest.*}, env: GUEST_LOGIN_ENABLED, GUEST_EMAIL_DOMAIN.
 * SOT는 infra 폴더 values.
 */
@Component
@ConfigurationProperties(prefix = "guest")
@Getter
@Setter
public class GuestProperties {

	private boolean enabled;
	private String emailDomain;

	public String emailDomainTrimmed() {
		return emailDomain == null ? "" : emailDomain.trim();
	}
}
