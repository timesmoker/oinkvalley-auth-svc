package com.oinkvalley.auth_svc;

import com.oinkvalley.profile.v1.ProfileInternalServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(target = "profile", types = ProfileInternalServiceGrpc.ProfileInternalServiceBlockingStub.class)
public class AuthSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthSvcApplication.class, args);
	}
}
