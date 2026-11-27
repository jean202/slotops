# FINDINGS — 테스트 보강 + CI (브랜치 `test/coverage-and-ci`)

작성일: 2026-11-11 · 무인 실행 결과 보고

## 0. 먼저 알아둘 것

- 요청은 `/pstack:poteto-mode` 플레이북으로 진행하는 것이었으나, 이 스킬은 `disable-model-invocation`으로 **모델이 직접 호출할 수 없게 막혀 있다**(사용자가 직접 입력해야 함). 제한을 우회하지 않았고, pstack 없이 일반 방식으로 같은 목표를 수행했다.
- 코드에 `@RequireRole`은 존재하지 않는다(CLAUDE.md에만 언급). 역할 검사는 `OrganizationService.addMember` 한 곳에 인라인으로만 있다.

## 1. 테스트 수

`slotops-api`에서 `./gradlew clean test` 기준.

| | tests | skipped | failures | errors |
|---|---|---|---|---|
| 기준선 (main `14cbd4a`) | 61 | 0 | 0 | 0 |
| 최종 | 298 | 11 | 0 | 0 |

- 실행되어 통과하는 테스트: 61 → 287.
- skipped 11개는 전부 아래 3절의 버그 재현 테스트(`@Disabled` + 사유)다. `@Disabled`를 일시 제거해 11개 모두 실제로 실패함을 확인한 뒤 되돌렸다.
- 기존 테스트 파일은 수정하지 않았다.

## 2. 추가한 테스트

| 파일 | 수 | 검증 내용 |
|---|---|---|
| `global/security/JwtTokenProviderTest` | 6 | 토큰 생성/파싱, 만료·위조·다른 키·쓰레기 값 거부 |
| `auth/api/AuthControllerTest` | 17 (3 disabled) | 회원가입(해시 저장, 중복, 검증), 로그인(토큰으로 보호 API 호출, 이메일 존재 여부 비노출), 리프레시 |
| `global/security/JwtAuthenticationFilterTest` | 9 | 실제 필터 체인: 토큰 없음/형식 오류/만료/다른 키 → 401, 유효 토큰의 userId 전달, 공개 엔드포인트 허용 |
| `global/security/CrossOrgAccessTest` | 92 | 실제 JWT + `OrgAccessAspect`로 조직 격리 검증. 조직 범위 엔드포인트 33개 × (타 조직 멤버 403 / 미인증 401) + 자기 조직 경로로 타 조직 하위 ID 접근 차단. 매 요청 후 상대 조직 데이터 불변 확인 |
| `organization/api/OrganizationControllerTest` | 27 (2 disabled) | 생성 시 OWNER 부여, slug 검증·중복, 조회 권한, 멤버 추가 역할 규칙(OWNER/MANAGER 가능, STAFF·비멤버 불가) |
| `location/api/LocationControllerTest` | 17 (3 disabled) | CRUD, 부분 수정, 검증, 타 조직 지점 차단 |
| `resource/api/ResourceControllerTest` | 29 (3 disabled) | CRUD, 필드별 검증, 이미지 순서·삭제, 지점/조직 범위 |
| `reservation/api/PublicReservationRulesTest` | 21 | 영업시간, 리드타임, 당일 마감, 블랙아웃, 버퍼, 취소 후 슬롯 해제, 가격, 고객 재사용, 알림 채널(EMAIL/SMS) |
| `reservation/application/ReservationPolicyValidatorTest` | 9 | 인원·시간 범위·슬롯 단위·정원 규칙 |
| `reservation/application/ReservationCodeGeneratorTest` | 2 | 코드 형식, 무작위성 |
| `payment/domain/PaymentTest` | 8 | 결제 상태 전이와 금액, 환불 반올림 |

테스트 픽스처: `src/test/resources/sql/two-orgs.sql`(조직 2개 전체 시드) 추가.

## 3. 발견한 버그

### 고친 것 (별도 커밋 `1708f85`)

**Location·Resource 엔드포인트에 조직 멤버십 검사가 없었다.** `LocationController`, `ResourceController`에 `@RequireOrgAccess`가 빠져 있어, 로그인한 사용자라면 누구든 다른 조직의 지점·자원을 조회·수정·삭제할 수 있었다(10개 엔드포인트). `CrossOrgAccessTest`로 재현 후 수정.

- 두 컨트롤러에 `@RequireOrgAccess` 추가.
- 두 컨트롤러는 경로 변수명이 `orgId`가 아니라 `organizationId`라서, `OrgAccessAspect`가 둘 다 읽도록 변경.
- **확인 필요**: 프로덕션 동작이 바뀌는 수정이다(비멤버 200 → 403). 의도와 맞는지 리뷰해 달라.

### 기록했던 버그 9건 — 수정 완료 (브랜치 `fix/findings-bugs`, 2026-11-27)

번호는 원래 `@Disabled` 사유의 번호와 같다. `@Disabled`는 모두 제거됐고, `./gradlew clean test` 기준 309 tests / skipped 0 / failures 0 / errors 0.

| # | 내용 | 수정 | 커밋 |
|---|---|---|---|
| 1 | 액세스 토큰을 리프레시 토큰으로(반대도) 쓸 수 있었다 | 토큰에 `typ`(access/refresh) 클레임 추가. JWT 필터는 access만, `/auth/refresh`는 refresh만 통과 | `ed4bcd7` |
| 2 | 정지·비활성 사용자도 로그인·리프레시됐다 | 로그인(비밀번호 확인 후)·리프레시에서 ACTIVE 아니면 403 `ACCOUNT_NOT_ACTIVE`. JWT 필터는 DB 조회 안 함 | `56ddb91` |
| 3 | 이메일 없는 회원가입이 500 | `SignupRequest`/`LoginRequest.email`에 `@NotBlank` | `6dd1257` |
| 4 | MANAGER가 OWNER 역할을 부여할 수 있었다 | OWNER 부여는 호출자가 OWNER일 때만(아니면 403) | `33d6c76` |
| 5 | 존재하지 않는 사용자를 멤버로 추가 | 대상 사용자 존재 확인, 없으면 404 `USER_NOT_FOUND` | `6a6f821` |
| 6 | 존재하지 않는 타임존이 저장됐다 | 정규식 대신 `@ValidZoneId`(`ZoneId.of`) 검증. `Etc/GMT+9` 같은 유효 ID도 이제 허용 | `97d301a` |
| 7 | PUT 요청에 검증이 없었다 | `UpdateLocationRequest`/`UpdateResourceRequest`에 제약 + 컨트롤러 `@Valid`. null은 "변경 안 함" 유지 | `a1bb6f7` |
| 8 | 하위 데이터가 있는 지점·자원 삭제가 500 | 삭제 전 확인 후 409 `LOCATION_IN_USE`/`RESOURCE_IN_USE`(지점: 자원 존재 / 자원: 예약·운영 규칙·블랙아웃 존재) | `f4961c7` |
| 9 | 최소 예약 시간 > 최대 예약 시간인 자원이 생성됐다 | `CreateResourceRequest`에 클래스 수준 검증, 오류는 `maxBookingMinutes` 필드로 보고 | `c30b6ce` |

수정하며 남은 것(판단 필요):

- **#1 배포 영향**: 배포 전에 발급된 토큰에는 `typ`가 없어 전부 무효가 된다. 사용자는 한 번 다시 로그인해야 한다.
- **#2 한계**: 정지 시점에 이미 발급된 액세스 토큰은 만료(1시간)까지 유효하다. 즉시 차단이 필요하면 토큰 블랙리스트나 필터 DB 조회가 필요한데, 정책상 넣지 않았다.
- **#8 경쟁 조건**: 확인 후 삭제 사이에 다른 요청이 예약을 만들면 PostgreSQL에서는 여전히 FK 위반 500이 날 수 있다. `ApiExceptionHandler`에 `DataIntegrityViolationException` 범용 핸들러는 넣지 않았다(NOT NULL 위반 등 다른 원인까지 409로 덮을 수 있어서).
- **#4 범위**: OWNER 부여만 막았다. MANAGER가 MANAGER를 부여하는 것은 여전히 허용된다(재현 테스트가 요구하지 않았고 기존 동작 유지).
- 운영 스키마 변경은 없어서 Flyway 마이그레이션은 추가하지 않았다.

## 4. 사람이 확인할 항목

2026-11-28 기준 상태(main `bf660df`).

| # | 항목 | 상태 | 현황 / 남은 일 |
|---|---|---|---|
| 1 | 3절 수정 커밋 `1708f85`(Location·Resource `@RequireOrgAccess`)이 의도한 동작인지 | 완료 | PR #1 머지로 main에 반영됨(비멤버 200 → 403) |
| 2 | #1, #2, #4 보안 버그 우선 처리 | 완료 | PR #2에서 수정(`ed4bcd7`, `56ddb91`, `33d6c76`). 3절 참고 |
| 3 | 테스트 스키마 ≠ 운영 스키마 | 미해결 | 테스트는 여전히 H2 + `create-drop`, `spring.flyway.enabled: false`. Testcontainers 의존성 없음. 엔티티와 `V1__init_schema.sql`이 어긋나도 테스트로 안 잡힌다. 해결하려면 Testcontainers(새 의존성) 도입 여부를 정해야 한다 |
| 4 | CLAUDE.md의 `./gradlew integrationTest`, `./gradlew spotlessCheck`가 실제로 없음 | 미해결 | `build.gradle.kts`에 두 태스크 모두 없음. CI는 `./gradlew build`만 실행. CLAUDE.md를 고칠지 태스크를 추가할지 결정 필요 |
| 5 | `application.yml`의 JWT 시크릿 기본값이 평문 | 미해결 | `jwt.secret`이 플레이스홀더 문자열 그대로다. 저장소에 배포 설정이 없어 운영에서 덮어쓰는지는 확인할 수 없다. Spring 바인딩상 환경 변수 `JWT_SECRET`으로 덮어쓸 수 있다. 기본값을 없애 미설정 시 기동 실패하게 할지 검토 |
| 6 | 자기 조직 경로로 타 조직 자원을 실제 지점 ID와 함께 지정하면 404 대신 403 | 미해결 | `ResourceService`의 조직 불일치 검사가 여전히 403 `FORBIDDEN`. 차단은 되지만 존재 여부가 드러난다. `CrossOrgAccessTest`는 403/404 둘 다 허용 |
| 7 | 401 응답(`ErrorResponse`)과 그 외 오류(`ApiErrorResponse`)의 JSON 형태가 다름 | 미해결 | 401은 `{status, error, message, path}`, 나머지는 `{code, message, fieldErrors}`. 클라이언트 오류 처리 통일이 필요하면 엔트리 포인트를 `ApiErrorResponse`로 맞출 것 |

## 5. 못 한 것

- pstack 플레이북 사용(0절 참고).
- `AvailabilityService`의 규칙/블랙아웃 수정·삭제 엔드포인트 정상 경로, `StaffNoteService`, `AddonService`, `DashboardService`의 추가 케이스는 기존 테스트 + 격리 테스트 수준에 머물렀다.
- 동시 예약(같은 슬롯 동시 요청) 경쟁 조건 테스트는 작성하지 않았다. `existsOverlap` 후 저장 사이에 락이 없어 보이므로 PostgreSQL 환경에서 따로 확인할 가치가 있다.
- 커버리지 수치 측정(JaCoCo)은 새 의존성/플러그인 추가를 피하려고 넣지 않았다.
