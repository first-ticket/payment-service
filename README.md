# payment-service

First Ticket 프로젝트의 결제 서비스.  
토스페이먼츠 PG 연동을 통한 결제 생성, 승인, 환불 및 MSA 환경에서의 분산 트랜잭션 정합성을 보장한다.

---

## 📌 핵심 기능

티켓 예매 확정 후 결제 플로우를 처리하고 결제 상태에 따라 이벤트를 발행하여 Booking 서비스와 데이터 정합성을 유지한다.

### 주요 흐름

1. Booking 서비스가 예매 확정 후 `POST /api/v1/payments` 호출 → `orderId` 반환
2. 프론트엔드가 `GET /api/v1/payments/payment-page?orderId=xxx&amount=xxx` 호출 → 토스 결제창 HTML 반환
3. 사용자가 토스 결제창에서 카드 결제
4. 결제 성공 시 토스가 `confirm-redirect`로 리다이렉트 → 서버에서 토스 승인 API 호출
5. 결제 승인 완료 후 `payment.completed` 이벤트 발행 → Booking 서비스 예매 확정 처리
6. 결제 실패 시 토스가 `fail`로 리다이렉트 → 실패 사유 화면 반환

### 결제 상태 전이

```
PENDING     → 결제 요청 (선점 시간 내 재시도 가능)
FAILED      → 결제 실패 (선점 시간 내 재시도 가능)
FINAL_FAILED → 최종 실패 (선점 시간 만료, 재시도 불가)
SUCCESS     → 결제 성공
REFUNDED    → 환불 완료
```

> FAILED와 FINAL_FAILED를 분리하여 사용자가 선점 시간(10분) 내에 다른 카드로 재시도할 수 있도록 설계했다.  
> `payment.failed` 이벤트는 FINAL_FAILED 시에만 발행된다.

### 안정성

- **아웃박스 패턴** — 결제 상태 저장과 Kafka 이벤트 발행 원자성 보장, 이벤트 유실 0%
- **인박스 패턴** — `@IdempotentConsumer`로 Kafka 메시지 중복 처리 방지
- **멱등성 처리** — 중복 결제 생성 및 중복 환불 요청 방지
- **비관적 락** — 결제 만료 스케줄러와 사용자 동시 접근 시 상태 불일치 방지
- **DLQ** — 이벤트 처리 실패 시 Dead Letter Topic으로 이동하여 유실 없이 별도 관리
- **보상 트랜잭션** — `booking.payment.compensation` 수신 시 자동 환불 처리

### 외부 이벤트 연동

| 토픽 | 방향 | 처리 |
|---|---|---|
| `payment.completed` | 발행 | 결제 승인 완료 → Booking 예매 확정 |
| `payment.failed` | 발행 | FINAL_FAILED → Booking 예매 취소 |
| `payment.refund.completed` | 발행 | 환불 완료 → Booking 알림 |
| `booking.expired` | 수신 | 좌석 선점 만료 → 환불 처리 |
| `booking.cancel.requested` | 수신 | 사용자 예매 취소 → 환불 처리 |
| `booking.payment.compensation` | 수신 | 보상 트랜잭션 → 결제 취소 |

---

## 🛠 기술 스택

| 항목 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.13 · Spring Cloud 2025.0.2 |
| Database | PostgreSQL 16 |
| Cache | Redis |
| Migration | Flyway |
| Message Broker | Apache Kafka |
| External API | Toss Payments |
| Monitoring | Prometheus · Grafana · Zipkin |
| Test | JUnit 5 · Mockito · JaCoCo · Spring REST Docs |
| CI/CD | GitHub Actions · AWS ECR · AWS ECS Fargate |

공통 기술 스택은 공통 README 참고.

---

## 📁 패키지 구조

레이어드 아키텍처 기반

```
com.firstticket.paymentservice
├── domain/                  # 도메인 모델, Enum, Exception, Repository 인터페이스
│   ├── Payment.java         # 결제 Aggregate Root
│   ├── PaymentHistory.java  # 결제 이력
│   ├── PaymentStatus.java   # 결제 상태 Enum
│   └── exception/           # 도메인 예외
├── application/             # 비즈니스 로직
│   ├── PaymentCommandService.java   # 결제 생성, 승인, 환불
│   ├── PaymentQueryService.java     # 결제 조회
│   ├── PaymentScheduler.java        # 결제 만료 스케줄러
│   └── dto/                         # Command / Result DTO
├── infrastructure/          # 외부 연동, Repository 구현체
│   ├── persistence/         # JPA Repository
│   ├── external/            # Toss Payments API 클라이언트
│   ├── messaging/           # Kafka Producer / Consumer
│   └── config/              # 인프라 설정 (JpaConfig 등)
└── presentation/            # REST Controller, Request/Response DTO
    ├── PaymentController.java         # 외부 API (게이트웨이 접근)
    └── PaymentInternalController.java # 내부 API (서비스 간 통신)
```

---

## 🌐 API 엔드포인트

### 외부 API (게이트웨이 접근)

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/v1/payments` | 결제 생성 |
| GET | `/api/v1/payments/payment-page` | 토스 결제창 |
| POST | `/api/v1/payments/confirm` | 결제 승인 |
| GET | `/api/v1/payments/confirm-redirect` | 결제 승인 리다이렉트 |
| GET | `/api/v1/payments/fail` | 결제 실패 화면 |
| GET | `/api/v1/payments/me` | 내 결제 목록 조회 |
| GET | `/api/v1/payments/{paymentId}` | 결제 상세 조회 |
| POST | `/api/v1/payments/{paymentId}/refund` | 환불 |
| GET | `/api/v1/payments/admin` | 전체 결제 목록 (ADMIN) |

### 내부 API (서비스 간 통신)

| Method | Path | 설명 |
|---|---|---|
| GET | `/internal/v1/payments/payment-page` | 토스 결제창 (내부용) |

상세한 요청 / 응답 예시는 REST Docs 참조: `http://localhost:8084/docs/index.html`

---

## 🌐 포트

| 환경 | 포트 |
|---|---|
| local | 8084 |
| prod | 8080 (컨테이너 내부) |

---

## 🚀 로컬 실행

### 사전 조건

다음 인프라가 먼저 실행되어 있어야 한다.

- `infra` 레포의 docker-compose — PostgreSQL · Redis · Kafka · Zipkin
- `eureka-server` — 서비스 디스커버리
- `config-server` — 설정 관리 서버
- `booking-service` — Kafka 이벤트 발행

### 환경변수 설정

```bash
GITHUB_USER=
GITHUB_TOKEN=
CONFIG_SERVER_USERNAME=
CONFIG_SERVER_PASSWORD=
TOSS_SECRET_KEY=
DB_HOST=localhost
DB_USERNAME=postgres
DB_PASSWORD=
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
PAYMENT_SUCCESS_URL=http://localhost:8084/api/v1/payments/confirm-redirect
PAYMENT_FAIL_URL=http://localhost:8084/api/v1/payments/fail
```

`.env.example` 파일을 참고하여 `.env`를 작성한다.

### 실행

```bash
./gradlew bootRun
```

---

## ⚙️ 외부 설정

설정은 `config-repo`에서 관리한다.

### 공통 (`payment-service.yml`)

- PostgreSQL · Redis · Kafka 연결 정보
- Zipkin · Prometheus
- Kafka Producer / Consumer 기본값 (idempotence, retries 등)
- Outbox / Inbox 활성화: `messaging.outbox.enabled: true` / `messaging.inbox.enabled: true`
- 스키마: `payment_schema` (JPA + Flyway)

### payment-service 전용

- Toss Payments: `payment.success-url` · `payment.fail-url`
- 아웃박스 스케줄러: `messaging.outbox.scheduler.enabled` · `messaging.outbox.scheduler.delay`
- Kafka 토픽 매핑: `kafka.topics.*`

### 환경별 (`payment-service-{profile}.yml`)

- 포트, DB 호스트, Redis 호스트 등 환경 의존 설정
- 로컬 환경에서는 아웃박스 스케줄러 비활성화 (`messaging.outbox.scheduler.enabled: false`)

---

## 📊 성능 개선

### DB 인덱스 추가 (10만 건 기준)

| 인덱스 | Before | After | 개선율 |
|---|---|---|---|
| user_id | 15.957ms | 0.107ms | 99% 감소 (149배) |
| status + requested_at | 17.298ms | 0.076ms | 99.6% 감소 (227배) |

### N+1 문제 해결 (fetch join 적용)

| 항목 | Before | After | 개선율 |
|---|---|---|---|
| GET /api/v1/payments/me 응답 시간 | 6.66ms | 3.27ms | 51% 감소 |
| 쿼리 수 | N+1번 | 1번 | 91% 감소 |

### JaCoCo 테스트 커버리지

| 구분 | Before | After |
|---|---|---|
| 전체 | 35% | 95% |
| domain | 27% | 100% |
| application | 46% | 97% |

---

## 🔍 헬스체크

```bash
curl http://localhost:8084/actuator/health
# → {"status":"UP"}
```# payment-service

First Ticket 프로젝트의 결제 서비스.  
토스페이먼츠 PG 연동을 통한 결제 생성, 승인, 환불 및 MSA 환경에서의 분산 트랜잭션 정합성을 보장한다.

---

## 📌 핵심 기능

티켓 예매 확정 후 결제 플로우를 처리하고 결제 상태에 따라 이벤트를 발행하여 Booking 서비스와 데이터 정합성을 유지한다.

### 주요 흐름

1. Booking 서비스가 예매 확정 후 `POST /api/v1/payments` 호출 → `orderId` 반환
2. 프론트엔드가 `GET /api/v1/payments/payment-page?orderId=xxx&amount=xxx` 호출 → 토스 결제창 HTML 반환
3. 사용자가 토스 결제창에서 카드 결제
4. 결제 성공 시 토스가 `confirm-redirect`로 리다이렉트 → 서버에서 토스 승인 API 호출
5. 결제 승인 완료 후 `payment.completed` 이벤트 발행 → Booking 서비스 예매 확정 처리
6. 결제 실패 시 토스가 `fail`로 리다이렉트 → 실패 사유 화면 반환

### 결제 상태 전이

```
PENDING     → 결제 요청 (선점 시간 내 재시도 가능)
FAILED      → 결제 실패 (선점 시간 내 재시도 가능)
FINAL_FAILED → 최종 실패 (선점 시간 만료, 재시도 불가)
SUCCESS     → 결제 성공
REFUNDED    → 환불 완료
```

> FAILED와 FINAL_FAILED를 분리하여 사용자가 선점 시간(10분) 내에 다른 카드로 재시도할 수 있도록 설계했다.  
> `payment.failed` 이벤트는 FINAL_FAILED 시에만 발행된다.

### 안정성

- **아웃박스 패턴** — 결제 상태 저장과 Kafka 이벤트 발행 원자성 보장, 이벤트 유실 0%
- **인박스 패턴** — `@IdempotentConsumer`로 Kafka 메시지 중복 처리 방지
- **멱등성 처리** — 중복 결제 생성 및 중복 환불 요청 방지
- **비관적 락** — 결제 만료 스케줄러와 사용자 동시 접근 시 상태 불일치 방지
- **DLQ** — 이벤트 처리 실패 시 Dead Letter Topic으로 이동하여 유실 없이 별도 관리
- **보상 트랜잭션** — `booking.payment.compensation` 수신 시 자동 환불 처리

### 외부 이벤트 연동

| 토픽 | 방향 | 처리 |
|---|---|---|
| `payment.completed` | 발행 | 결제 승인 완료 → Booking 예매 확정 |
| `payment.failed` | 발행 | FINAL_FAILED → Booking 예매 취소 |
| `payment.refund.completed` | 발행 | 환불 완료 → Booking 알림 |
| `booking.expired` | 수신 | 좌석 선점 만료 → 환불 처리 |
| `booking.cancel.requested` | 수신 | 사용자 예매 취소 → 환불 처리 |
| `booking.payment.compensation` | 수신 | 보상 트랜잭션 → 결제 취소 |

---

## 🛠 기술 스택

| 항목 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.13 · Spring Cloud 2025.0.2 |
| Database | PostgreSQL 16 |
| Cache | Redis |
| Migration | Flyway |
| Message Broker | Apache Kafka |
| External API | Toss Payments |
| Monitoring | Prometheus · Grafana · Zipkin |
| Test | JUnit 5 · Mockito · JaCoCo · Spring REST Docs |
| CI/CD | GitHub Actions · AWS ECR · AWS ECS Fargate |

공통 기술 스택은 공통 README 참고.

---

## 📁 패키지 구조

레이어드 아키텍처 기반

```
com.firstticket.paymentservice
├── domain/                  # 도메인 모델, Enum, Exception, Repository 인터페이스
│   ├── Payment.java         # 결제 Aggregate Root
│   ├── PaymentHistory.java  # 결제 이력
│   ├── PaymentStatus.java   # 결제 상태 Enum
│   └── exception/           # 도메인 예외
├── application/             # 비즈니스 로직
│   ├── PaymentCommandService.java   # 결제 생성, 승인, 환불
│   ├── PaymentQueryService.java     # 결제 조회
│   ├── PaymentScheduler.java        # 결제 만료 스케줄러
│   └── dto/                         # Command / Result DTO
├── infrastructure/          # 외부 연동, Repository 구현체
│   ├── persistence/         # JPA Repository
│   ├── external/            # Toss Payments API 클라이언트
│   ├── messaging/           # Kafka Producer / Consumer
│   └── config/              # 인프라 설정 (JpaConfig 등)
└── presentation/            # REST Controller, Request/Response DTO
    ├── PaymentController.java         # 외부 API (게이트웨이 접근)
    └── PaymentInternalController.java # 내부 API (서비스 간 통신)
```

---

## 🌐 API 엔드포인트

### 외부 API (게이트웨이 접근)

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/v1/payments` | 결제 생성 |
| GET | `/api/v1/payments/payment-page` | 토스 결제창 |
| POST | `/api/v1/payments/confirm` | 결제 승인 |
| GET | `/api/v1/payments/confirm-redirect` | 결제 승인 리다이렉트 |
| GET | `/api/v1/payments/fail` | 결제 실패 화면 |
| GET | `/api/v1/payments/me` | 내 결제 목록 조회 |
| GET | `/api/v1/payments/{paymentId}` | 결제 상세 조회 |
| POST | `/api/v1/payments/{paymentId}/refund` | 환불 |
| GET | `/api/v1/payments/admin` | 전체 결제 목록 (ADMIN) |

### 내부 API (서비스 간 통신)

| Method | Path | 설명 |
|---|---|---|
| GET | `/internal/v1/payments/payment-page` | 토스 결제창 (내부용) |

상세한 요청 / 응답 예시는 REST Docs 참조: `http://localhost:8084/docs/index.html`

---

## 🌐 포트

| 환경 | 포트 |
|---|---|
| local | 8084 |
| prod | 8080 (컨테이너 내부) |

---

## 🚀 로컬 실행

### 사전 조건

다음 인프라가 먼저 실행되어 있어야 한다.

- `infra` 레포의 docker-compose — PostgreSQL · Redis · Kafka · Zipkin
- `eureka-server` — 서비스 디스커버리
- `config-server` — 설정 관리 서버
- `booking-service` — Kafka 이벤트 발행

### 환경변수 설정

```bash
GITHUB_USER=
GITHUB_TOKEN=
CONFIG_SERVER_USERNAME=
CONFIG_SERVER_PASSWORD=
TOSS_SECRET_KEY=
DB_HOST=localhost
DB_USERNAME=postgres
DB_PASSWORD=
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
PAYMENT_SUCCESS_URL=http://localhost:8084/api/v1/payments/confirm-redirect
PAYMENT_FAIL_URL=http://localhost:8084/api/v1/payments/fail
```

`.env.example` 파일을 참고하여 `.env`를 작성한다.

### 실행

```bash
./gradlew bootRun
```

---

## ⚙️ 외부 설정

설정은 `config-repo`에서 관리한다.

### 공통 (`payment-service.yml`)

- PostgreSQL · Redis · Kafka 연결 정보
- Zipkin · Prometheus
- Kafka Producer / Consumer 기본값 (idempotence, retries 등)
- Outbox / Inbox 활성화: `messaging.outbox.enabled: true` / `messaging.inbox.enabled: true`
- 스키마: `payment_schema` (JPA + Flyway)

### payment-service 전용

- Toss Payments: `payment.success-url` · `payment.fail-url`
- 아웃박스 스케줄러: `messaging.outbox.scheduler.enabled` · `messaging.outbox.scheduler.delay`
- Kafka 토픽 매핑: `kafka.topics.*`

### 환경별 (`payment-service-{profile}.yml`)

- 포트, DB 호스트, Redis 호스트 등 환경 의존 설정
- 로컬 환경에서는 아웃박스 스케줄러 비활성화 (`messaging.outbox.scheduler.enabled: false`)

---

## 📊 성능 개선

### DB 인덱스 추가 (10만 건 기준)

| 인덱스 | Before | After | 개선율 |
|---|---|---|---|
| user_id | 15.957ms | 0.107ms | 99% 감소 (149배) |
| status + requested_at | 17.298ms | 0.076ms | 99.6% 감소 (227배) |

### N+1 문제 해결 (fetch join 적용)

| 항목 | Before | After | 개선율 |
|---|---|---|---|
| GET /api/v1/payments/me 응답 시간 | 6.66ms | 3.27ms | 51% 감소 |
| 쿼리 수 | N+1번 | 1번 | 91% 감소 |

### JaCoCo 테스트 커버리지

| 구분 | Before | After |
|---|---|---|
| 전체 | 35% | 95% |
| domain | 27% | 100% |
| application | 46% | 97% |

---
