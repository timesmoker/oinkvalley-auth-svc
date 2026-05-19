# auth-svc

Spring Boot **인증 REST API**다. 사용자를 PostgreSQL에 두고, **이메일 + 비밀번호**로 로그인한 뒤 **HS256 JWT**를 발급한다. 로그인 응답에서는 **`Set-Cookie` HttpOnly** 로 JWT를 내려주고, JSON 본문에는 **`tokenType`**, **`expiresInSeconds`** 만 온다(토큰 문자열은 본문에 없음). **`GET /auth/me`** 등 보호 API는 **`Authorization: Bearer <token>`** 만 검증한다(쿠키에서 JWT를 읽지 않는다). 가입 시 **`username`** 은 닉네임, 로그인 식별자는 **`email`** 이다.

의존성·JDK·Gradle 버전 요약은 [DEPENDENCIES.md](DEPENDENCIES.md) 를 본다.

**단일 진실 소스(SOT):** 배포·운영에서 쓰는 값의 기준은 무조건 **infra 폴더**(Helm values, 매니페스트, 환경 변수 정의 등)에 있다. 이 저장소의 `application.properties` 와 여기 문서는 편의·개발용 설명이며, 충돌하면 **infra 쪽이 정답**이다.

## 목차

- [빠른 시작](#빠른-시작)
- [설정](#설정)
- [프로젝트 구조](#프로젝트-구조)
- [HTTP API](#http-api)
- [쿠키와 Bearer (앞단 전제)](#쿠키와-bearer-앞단-전제)

## 빠른 시작

- JDK **21**, 저장소에 포함된 Gradle Wrapper (`./gradlew`) 를 쓴다.
- 빌드: `./gradlew bootJar`
- 로컬 실행: `./gradlew bootRun` — DB·JWT 등은 아래 설정을 맞춘다.

## 설정

런타임은 `src/main/resources/application.properties` 를 따른다. DB 연결은 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` 를 반드시 준다(기본값 없음). 로컬 `bootRun` 도 동일하게 환경 변수를 맞춘다. JWT·쿠키·JPA DDL 등은 프로퍼티에 `${변수:기본값}` 이 있다.

**SOT 재확인:** 클러스터·배포에 실제로 쓰는 키·값은 **infra 폴더**를 따른다(이 절의 표는 이름·역할 참고용).


| 환경 변수                           | 바인딩(요지)                                  | 설명                                                                                                                                      |
| ------------------------------- | ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`         | `spring.datasource.url`                  | JDBC URL (필수)                                                                                                                           |
| `SPRING_DATASOURCE_USERNAME`    | `spring.datasource.username`             | DB 사용자 (필수)                                                                                                                             |
| `SPRING_DATASOURCE_PASSWORD`    | `spring.datasource.password`             | DB 비밀번호 (필수)                                                                                                                            |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto`          | 예: `validate`, `update` (기본값 `validate`)                                                                                                |
| `JWT_SECRET`                    | `jwt.secret`                             | HS256 서명·검증 비밀(UTF-8 32바이트 이상 권장). 토큰을 검증하는 다른 서비스(예: board-svc)와 **같은 값**이어야 한다.                                                       |
| `JWT_EXPIRATION_SECONDS` 등    | `jwt.expiration-seconds`, `jwt.cookie.*` | TTL·쿠키 이름·Secure·SameSite·path·domain 등 ([relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html)) |


**k3s·Helm**에서 Pod에 넣은 환경 변수는 같은 프로퍼티에 대해 패키징된 기본값보다 **우선**한다.

**JWT:** 이 서비스는 토큰을 **발급**하고 `/auth/me` 에서 **Bearer만** 검증한다. 쿠키는 로그인·로그아웃 **응답 헤더**용(브라우저 저장 등). 클레임은 소비자 서비스와 맞춘다. 규칙은 아래 [보안](#보안-구현-요약)을 본다.

## 프로젝트 구조


| 경로               | 역할                                                             |
| ---------------- | -------------------------------------------------------------- |
| `controller/`    | `AuthController`, `HealthController`, `AuthExceptionHandler`   |
| `service/`       | `AuthService`, `DatabaseUserDetailsService`, `HealthService` 등 |
| `dto/`           | 요청·응답 레코드                                                      |
| `db/domain/`     | JPA 엔티티 `User`                                                 |
| `db/repository/` | `UserRepository`                                               |
| `security/`      | JWT 발급·검증, `JwtAuthenticationFilter`, 쿠키 발급, `AuthUserPrincipal`  |
| `config/`        | `SecurityConfig`, `JwtProperties`                              |


## HTTP API

베이스 URL·리버스 프록시 접두 경로는 이 저장소에서 고정하지 않는다.

### 쿠키와 Bearer (앞단 전제)

로그인 응답의 **`Set-Cookie` HttpOnly** 는 브라우저·같은 사이트 흐름에서 토큰을 저장하기 위한 것이다. 이 서비스는 보호 API에서 **쿠키를 읽지 않고** **`Authorization: Bearer`** 만 검증하므로, **앞단(BFF, Ingress/게이트웨이, 풀스택 서버 등)** 이 들어오는 요청에 대해 **쿠키에 실린 JWT를 읽어 `Authorization: Bearer …` 로 바꿔 붙이는 것**을 **전제로 한다**(또는 동등하게, 앞단이 사용자 인증 상태에 맞는 Bearer를 항상 붙인다).

그 전제가 지켜지면 **auth-svc 쪽 역할(발급·쿠키 내려주기·Bearer로 검증)은 그대로 성립**한다.

### 보안 (구현 요약)

- Spring Security, **무상태**(세션 미사용), CSRF 비활성화. CORS는 이 애플리케이션에 두지 않았고, 필요하면 게이트웨이·프록시에서 맞춘다.
- 로그인 응답에는 **`Set-Cookie` HttpOnly** 로 JWT가 오고, JSON에는 `tokenType`, `expiresInSeconds` 만 있다.
- **`GET /auth/me`** 등: **`Authorization: Bearer <JWT>`** 만 검증한다(요청 쿠키의 토큰은 읽지 않는다).
- `JwtAuthenticationFilter` 가 Bearer 토큰을 검증하고 `SecurityContext` 를 채운다.
- **규칙 순서** (`SecurityConfig`): 위에서 아래로 먼저 매칭된다.
  1. `OPTIONS /**` → 허용
  2. `GET /health` → 허용
  3. `POST /auth/login`, `POST /auth/logout`, `POST /auth/signup` → 허용
  4. `GET /auth/me` → **인증 필요**(유효 JWT)
  5. 나머지 → **거부** (`denyAll`)

로그아웃 시 **`Set-Cookie`** 로 쿠키를 지운다. API는 상태 없음이라 **Bearer 토큰 폐기는 클라이언트**가 한다.

**다른 서비스가 검증할 때 맞출 클레임:** HS256, `sub` 는 숫자 문자열(사용자 ID), `roles` 문자열 배열(클레임에는 `ROLE_` 접두사 없음, 필터에서 스프링 규약에 맞게 붙임), `exp` 등.

### 엔드포인트


| 메서드  | 경로             | 인증                     | 비고                                      |
| ---- | -------------- | ---------------------- | --------------------------------------- |
| GET  | `/health`      | 불필요                    | 문자열 본문 `UP`                             |
| POST | `/auth/signup` | 불필요                    | 201 `SignUpResponse`, 중복 409            |
| POST | `/auth/login`  | 불필요                    | 200 `LoginResponse` + `Set-Cookie`(JWT), 401 |
| POST | `/auth/logout` | 불필요                    | 204 + `Set-Cookie` 쿠키 제거                   |
| GET  | `/auth/me`     | 필요 (`Authorization: Bearer`) | 200 `MeResponse`, 401                   |


JSON은 **camelCase**다. 오류 응답은 현재 **본문 없이** 상태 코드 위주다.

### 응답·요청 필드 요약

**SignUpRequest:** `email`, `password`, `username`(닉네임), `roles`(선택, 저장·권한에 반영 안 함 — 서버가 `TEMP_USER`만 부여)

**SignUpResponse:** `id`, `username`, `email`

**LoginRequest:** `email`, `password`

**LoginResponse:** `tokenType`, `expiresInSeconds` (JWT는 `Set-Cookie` 로만)

**MeResponse:** `userId`
