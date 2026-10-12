package com.oinkvalley.auth_svc.messaging.publisher;

import com.oinkvalley.auth_svc.config.NatsConfig;
import com.oinkvalley.auth_svc.messaging.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 계정 생성 사실 → {@code auth.user.created}.
 * 페이로드는 userId. 후속 작업은 여기 없음.
 */
@Component
public class UserEventPublisher {

	public static final String SUBJECT = "auth.user.created";

	private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

	private final NatsConfig natsConfig;

	public UserEventPublisher(NatsConfig natsConfig) {
		this.natsConfig = natsConfig;
	}

	public void publish(UserCreatedEvent event) {
		if (event == null || event.userId() <= 0) {
			return;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("userId", event.userId());
		try {
			natsConfig.publish(SUBJECT, body);
		} catch (Exception e) {
			log.warn("auth.user.created publish failed userId={}: {}", event.userId(), e.toString());
		}
	}
}
