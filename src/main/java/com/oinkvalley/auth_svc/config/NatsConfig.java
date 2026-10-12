package com.oinkvalley.auth_svc.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

/**
 * NATS 연결·발행. Subject는 발행자가 넘긴다.
 * 브로커가 어떤 Subject를 담는지는 {@code nats-ensure-subjects.sh}. 수신자가 뭘 할지는 auth가 모른다.
 */
@Component
public class NatsConfig {

	private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);
	private static final JsonMapper JSON = JsonMapper.shared();

	private final Connection connection;

	public NatsConfig(@Value("${nats.url}") String natsUrl) {
		this.connection = connectOrNull(natsUrl);
	}

	private static Connection connectOrNull(String natsUrl) {
		if (natsUrl == null || natsUrl.isBlank()) {
			log.info("NATS_URL empty — auth event publish disabled");
			return null;
		}
		try {
			Options options = new Options.Builder()
					.server(natsUrl)
					.connectionName("auth-svc")
					.maxReconnects(-1)
					.reconnectWait(Duration.ofSeconds(2))
					.build();
			Connection nc = Nats.connect(options);
			log.info("NATS connected for auth events url={}", natsUrl);
			return nc;
		} catch (Exception e) {
			log.warn("NATS connect failed — auth event publish disabled: {}", e.toString());
			return null;
		}
	}

	/** subject 로 JSON 발행. */
	public void publish(String subject, Map<String, Object> body) {
		if (connection == null || subject == null || subject.isBlank() || body == null || body.isEmpty()) {
			return;
		}
		try {
			connection.publish(subject, JSON.writeValueAsBytes(body));
		} catch (Exception e) {
			log.warn("NATS publish failed subject={}: {}", subject, e.toString());
		}
	}

	@PreDestroy
	void close() {
		if (connection == null) {
			return;
		}
		try {
			connection.flush(Duration.ofSeconds(2));
			connection.close();
		} catch (Exception e) {
			log.debug("NATS close: {}", e.toString());
		}
	}
}
