# 의존성

`build.gradle` 과 같다. Starter 모듈 버전은 Spring Boot가 끌고 오는 **의존성 관리(BOM)** 에 맞춰진다.

**implementation**

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework:spring-tx`
- `com.fasterxml.jackson.core:jackson-databind`
- `org.jspecify:jspecify:1.0.0`

**runtimeOnly**

- `org.postgresql:postgresql`

**compileOnly**

- `org.projectlombok:lombok`

**annotationProcessor**

- `org.projectlombok:lombok`
- `org.springframework.boot:spring-boot-configuration-processor`

JDK 21. 플러그인: Spring Boot 4.0.5, dependency-management 1.1.7. Gradle 9.4.1은 `gradle/wrapper/gradle-wrapper.properties` 에 적혀 있다.
