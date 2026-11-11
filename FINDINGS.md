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

### 고치지 않고 기록만 한 것 (판단 필요)

번호는 테스트의 `@Disabled` 사유에 적힌 번호와 같다.

| # | 내용 | 재현 테스트 |
|---|---|---|
| 1 | **액세스 토큰을 리프레시 토큰으로 쓸 수 있다.** 두 토큰에 종류 구분 클레임이 없어 `/auth/refresh`가 액세스 토큰을 받아 7일짜리 리프레시 토큰을 새로 발급한다. 반대로 리프레시 토큰도 API 인증에 통과한다. | `AuthControllerTest.refreshRejectsAccessTokenUsedAsRefreshToken` |
| 2 | **정지된 사용자도 로그인된다.** `AuthService.login`/`refreshToken`이 `UserStatus`(INACTIVE/SUSPENDED)를 확인하지 않는다. JWT 필터도 사용자 상태·존재를 확인하지 않는다. | `AuthControllerTest.loginRejectsSuspendedUser` |
| 3 | **이메일 없는 회원가입이 500.** `SignupRequest.email`에 `@Email`만 있고 `@NotBlank`가 없어 null이 검증을 통과, DB NOT NULL 위반으로 터진다. `LoginRequest.email`도 동일(이쪽은 401로 끝남). | `AuthControllerTest.signupRejectsMissingEmail` |
| 4 | **MANAGER가 OWNER 역할을 부여할 수 있다.** `addMember`가 호출자 역할만 보고 부여하려는 역할은 보지 않는다. | `OrganizationControllerTest.managerCannotGrantOwnerRole` |
| 5 | **존재하지 않는 사용자를 멤버로 추가.** 대상 userId 존재 확인이 없다. H2 테스트 스키마에서는 201로 성공하고, PostgreSQL에서는 FK 위반으로 500이 될 것이다. | `OrganizationControllerTest.rejectsAddingUserThatDoesNotExist` |
| 6 | **존재하지 않는 타임존이 저장된다.** `[A-Za-z_/]+` 패턴만 검사해 `Mars/Olympus_Mons`가 통과하고, 이후 슬롯 조회·예약의 `ZoneId.of()`에서 500이 난다. 반대로 `Etc/GMT+9` 같은 유효한 ID는 거부된다. | `LocationControllerTest.rejectsTimezoneThatIsNotARealZoneId` |
| 7 | **수정(PUT) 요청에 검증이 없다.** `UpdateLocationRequest`, `UpdateResourceRequest`에 제약이 없어 빈 이름, 정원 0, 음수 가격을 PUT으로 쓸 수 있다(POST는 거부). | `LocationControllerTest.updateRejectsBlankName`, `ResourceControllerTest.updateRejectsInvalidValues` |
| 8 | **하위 데이터가 있는 지점·자원 삭제가 500.** FK 위반이 처리되지 않은 `DataIntegrityViolationException`으로 올라온다. 409로 막을지, 연쇄 삭제/소프트 삭제로 할지 결정이 필요하다. `ApiExceptionHandler`에 범용 핸들러도 없다. | `LocationControllerTest.deletingLocationWithResourcesIsRejectedCleanly`, `ResourceControllerTest.deletingResourceWithReservationsIsRejectedCleanly` |
| 9 | **최소 예약 시간 > 최대 예약 시간인 자원이 생성된다.** 교차 필드 검증이 없어 예약이 불가능한 자원이 만들어진다. | `ResourceControllerTest.rejectsMinBookingLongerThanMaxBooking` |

## 4. 사람이 확인할 항목

1. 3절의 수정 커밋(`1708f85`)이 의도한 동작인지.
2. #1, #2, #4는 보안 성격이라 우선순위를 높게 볼 만하다.
3. **테스트 스키마 ≠ 운영 스키마.** 테스트는 H2 + Hibernate `create-drop`으로 돌고 Flyway `V1__init_schema.sql`은 테스트에서 전혀 실행되지 않는다. 엔티티와 마이그레이션이 어긋나도(#5의 FK 차이가 그 예) 테스트로는 잡히지 않는다. Testcontainers는 의존성에 없다.
4. CLAUDE.md의 `./gradlew integrationTest`, `./gradlew spotlessCheck`는 `build.gradle.kts`에 정의돼 있지 않다. CI는 `./gradlew build`만 실행한다.
5. `application.yml`에 JWT 시크릿 기본값이 평문으로 들어 있다. 운영에서 환경 변수로 덮어쓰는지 확인.
6. 자기 조직 경로로 타 조직 자원을 실제 지점 ID와 함께 지정하면(`/organizations/{내 조직}/locations/{남의 지점}/resources/{남의 자원}`) 404가 아니라 403이 나온다. 차단은 되지만 존재 여부가 드러난다.
7. 401 응답(`ErrorResponse`)과 그 외 오류 응답(`ApiErrorResponse`)의 JSON 형태가 다르다.

## 5. 못 한 것

- pstack 플레이북 사용(0절 참고).
- `AvailabilityService`의 규칙/블랙아웃 수정·삭제 엔드포인트 정상 경로, `StaffNoteService`, `AddonService`, `DashboardService`의 추가 케이스는 기존 테스트 + 격리 테스트 수준에 머물렀다.
- 동시 예약(같은 슬롯 동시 요청) 경쟁 조건 테스트는 작성하지 않았다. `existsOverlap` 후 저장 사이에 락이 없어 보이므로 PostgreSQL 환경에서 따로 확인할 가치가 있다.
- 커버리지 수치 측정(JaCoCo)은 새 의존성/플러그인 추가를 피하려고 넣지 않았다.
