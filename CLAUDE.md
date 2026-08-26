# Passmate-KMP

패스메이트(PassMate) 학생 앱 — Kotlin Multiplatform. 모듈: `shared`(도메인+데이터), `composeApp`(Android + Desktop/jvm UI), `iosApp`(SwiftUI 미러). 루트 패키지 `org.sesacteamproject.passmate`.

## 코드 규칙 (필수 준수)

이 리포의 모든 코드 작성·수정·리뷰는 아래 문서의 규칙을 따른다. 규칙과 충돌하는 요청을 받으면 먼저 규칙 문서를 근거로 확인한다.

@docs/Passmate_코드_패턴_규칙.md

아키텍처(모듈·레이어 경계, Koin DI 배선, MVI 기반 클래스)는 아래 설계 문서를 따른다. 두 문서 충돌 시 규칙 문서 우선.

@docs/Passmate_아키텍처_설계.md

## 작업 범위 (필수)

- **이 리포의 작업자(홍희표)는 KMP 학생 앱(Passmate-KMP)만 담당한다.** 백엔드(Passmate-Backend)는 전혜림, 웹(Passmate-Frontend)은 서승혁 담당 — 이 세션에서 백엔드·웹 코드를 작성·수정하지 않는다.
- 백엔드 API가 필요한데 아직 없으면: 구현을 대신하지 말고 **계약 문서(contracts/) 기준으로 클라이언트를 먼저 작성**하고, 필요 시 목킹으로 검증한다. 계약에 없는 것이 필요하면 계약 갱신을 제안해 팀에 전달한다.
- tasks.md에서 이 리포가 수행하는 것은 경로가 `mobile/`인 태스크뿐이다. `backend/`·`web/` 태스크는 참조만 한다.

## 프로젝트 컨텍스트

- 기능 명세·구현 계획·태스크: 상위 폴더 `../specs/001-passmate-mvp/` (spec.md · plan.md · tasks.md) — 로컬 모노 폴더 기준 경로
- REST·WebSocket 계약(단일 진실): `../specs/001-passmate-mvp/contracts/` (rest-api.md · websocket-events.md) — DTO/이벤트는 계약과 1:1, 어긋나면 계약을 먼저 갱신
- 이 앱의 담당 태스크는 tasks.md에서 경로가 `mobile/`로 표기된 항목이며, 실제 구현은 이 리포(Passmate-KMP)에 한다
- 팀: 새싹수들 (backend 전혜림 · web 서승혁 · mobile 홍희표) — GitHub 조직 `Sesac-TeamProject`

## 작업 관행

- 새 파일은 LF 개행으로 작성한다 (템플릿 기존 파일도 LF)
- 3타깃(Android/jvm/iOS) 공통 코드는 `shared/src/commonMain`에 두고, 플랫폼 분기는 expect/actual로 처리한다
- 커밋·push 전 최소 검증: `./gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid` (WSL에서 gradle EIO 발생 시 Windows `gradlew.bat` 경로 사용)
