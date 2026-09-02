# Passmate 코드 패턴 규칙

이 문서는 Passmate-KMP(학생 앱) 구현 시 작성하는 코드의 고정 규칙이다.
범위: `shared`, `composeApp`(Android + Desktop/jvm), `iosApp`(SwiftUI 미러)
계약 문서: REST·WebSocket DTO/이벤트의 단일 진실은 `specs/001-passmate-mvp/contracts/`(rest-api.md·websocket-events.md)이다. 구현이 계약과 다르면 계약을 먼저 갱신한다.

## 1. 공통 원칙

- 기능보다 일관성을 우선한다.
- 한 파일/클래스는 하나의 책임만 가진다.
- 예외(`throw`)를 UI까지 전파하지 않고 `AppResult`/`AppError`로 변환한다.
- 로그인/권한/검증 로직은 화면이 아닌 도메인 정책 또는 라우트 가드에 둔다.
- 하드코딩 문자열/매직넘버를 최소화하고 상수로 분리한다.
- **서버 권위 원칙**: 점수 계산·타이머 만료·정오 판정은 전부 서버가 한다. 클라이언트는 서버가 준 값을 렌더링만 한다.

## 2. 모듈 경계 규칙

- `shared`는 도메인+데이터 계층만 담는다. UI 프레임워크 의존성을 갖지 않는다.
- `composeApp`/`iosApp`은 `shared`의 도메인 타입과 UseCase만 참조한다.
- ViewModel은 `shared`에 두지 않는다 — 플랫폼별 1:1로 작성한다(composeApp Kotlin ↔ iosApp Swift, 동일한 UiState/Action/Event 미러).
- 플랫폼별 구현(QR 스캔·오디오 재생·토큰 저장 등)은 `expect/actual` 또는 인터페이스 뒤로 숨기고 도메인 계약을 유지한다.

## 2-1. 플랫폼 네비게이션 규칙

- Android는 `Jetpack Navigation`(`NavHost`, `NavController`)을 사용한다.
- iOS는 `NavigationView`(stack 스타일) 위에 상태 배열 `[Route]` 기반 push(`RouteStackLevel`)를 사용한다.
- iOS 최소 배포 타깃은 15.0이다. iOS 16+ 전용 API(`NavigationStack`·`presentationDetents`·`Layout` 등)는 화면에서 직접 쓰지 않고 `#available` 분기를 가진 공통 컴포넌트(`component/`) 뒤로만 사용한다.
- Desktop은 상태 기반 라우트 상태머신(`currentRoute`, `routeStack`)을 사용한다.
- 라우트 이름과 로그인 가드 규칙은 3플랫폼에서 동일하게 유지한다.
- 상세 화면은 모달이 아닌 route push 방식으로 이동한다.
- 인증이 필요한 라우트 진입 실패 시 `SignIn` 라우트로 이동 후 성공 시 `pendingRoute/pendingAction`을 재실행한다.

### 2-1-1. 공통 라우트 규격

- 루트 라우트: `Home`, `SignIn`, `Join`, `Waiting`, `Play`, `Result`, `MyInfo`, `Payment`, `Settings`
- 하단 탭 루트(피그마 v6, 2026-08-30): `Home`(홈=입장 폼 인라인) · `HostedRooms`(내가 만든 방) · `JoinedRooms`(참여한 방) · `MyInfo`(마이). 탭 바는 이 4개 루트에서만 표시하고 push된 화면에서는 숨긴다. 게스트가 로그인 필수 탭(`HostedRooms`·`JoinedRooms`·`MyInfo`)을 누르면 화면을 열지 않고 `SignIn`으로 보낸다(판단은 셸 `AppShellViewModel`).
- `Join`은 `join?pin=`(QR·딥링크·방 목록 참여)일 때만 push 라우트로 쓴다. pin 없는 입장은 `Home` 탭이 담당한다.
- 라우트 인자: `join?pin=`, `waiting/{pin}`, `play/{pin}`, `result/{participationId}`, `payment/{pin}`
- `Home` = 입장 폼(인라인 PIN·QR·닉네임·캐릭터). 앱 시작 기본 진입은 항상 `Home`(게스트 포함).
- `Settings`는 `MyInfo`에서 진입하는 상세 push 라우트로 취급한다.

### 2-1-2. 세션 플로우 네비게이션 규칙

- 세션 플로우는 `Join → Waiting → Play → Result` 단방향이다. 화면 전환은 UI 임의 판단이 아니라 **서버 이벤트로만** 일어난다:
  - `GAME_STARTED` 수신 → `Waiting`에서 `Play`로 전환
  - `GAME_FINISHED` 수신 → `Play`에서 `Result`로 전환
- `Play` 진입 후 뒤로가기는 즉시 pop 하지 않고 퇴장 확인 다이얼로그를 거친다.
- `Result` 진입 시 세션 플로우 엔트리(`Join/Payment/Waiting/Play`)만 백스택에서 제거하고, 그 아래의 탭 루트(`Home`·`JoinedRooms` 등)는 유지한다.
- **재접속 복구**: 앱 재진입·네트워크 복구 시 `GET /rooms/{pin}/session/snapshot` 결과의 `status`로 라우트를 결정한다(WAITING→`Waiting`, RUNNING→`Play`, FINISHED→`Result`). 스냅샷 ts 이전의 STOMP 이벤트는 폐기한다.
- 종료된 방(410)·잘못된 PIN(404)은 안내 후 `Home`으로 보낸다.

## 3. 패키지/파일 규칙

- 루트 패키지는 `org.sesacteamproject.passmate`를 사용한다.
- 패키지 구조는 기능+레이어 기준으로 유지한다.
  - shared: `auth`, `room`, `session`, `question`, `feedback`, `report`, `rating`, `payment`, `user` + 공통 `network`, `model`, `common`
  - composeApp: `ui/{home,auth,join,waiting,play,result,mypage,payment}`, `component`, `navigation`, `theme`
- 파일명은 타입명과 1:1 매칭한다.
- 하나의 파일에 public 타입은 1개를 기본으로 한다.
- 확장 함수/매퍼는 `*Mapper.kt`, `*Extensions.kt`로 분리한다.

## 4. 네이밍 규칙

- `UseCase`: 동사+대상 (`JoinRoomUseCase`, `SubmitAnswerUseCase`, `GetMyReportsUseCase`)
- `Repository`: 도메인명+Repository (`RoomRepository`, `SessionRepository`)
- `DataSource`: 도메인명+Remote/LocalDataSource (`RoomRemoteDataSource`)
- `UiState`: 화면명+UiState (`PlayUiState`)
- `ViewModel`: 화면명+ViewModel (`WaitingViewModel`)
- `Action`: 화면명+Action (`JoinAction`)
- `Event`: 화면명+Event (`PlayEvent`)
- Boolean은 `is/has/can` 접두어를 사용한다.
- STOMP 이벤트 DTO는 계약의 `type` 이름을 그대로 딴다 (`QuestionStarted`, `RankingUpdated`, `VoiceHint`).

## 5. 도메인 계층 규칙 (`shared` 도메인)

- UseCase는 `operator fun invoke(...)`를 기본으로 한다.
- UseCase는 다른 UseCase를 직접 호출하지 않고 Repository/Policy 중심으로 구성한다.
- 도메인 모델은 불변(`data class` + `val`)을 기본으로 한다.
- 닉네임 형식·PIN 자릿수 같은 입력 검증은 Policy 클래스에서 수행한다. 단, 최종 판정(중복·마감·정오)은 서버 응답을 따른다.
- 남은 시간은 서버가 준 `endsAt`(서버 시각)과의 차로만 계산해 표시한다. 클라이언트 로컬 타이머로 마감·점수를 판정하지 않는다.

## 6. 데이터 계층 규칙 (`shared` 데이터)

- 외부 응답 DTO와 도메인 모델을 분리한다. DTO는 계약 문서와 1:1로 유지한다.
- DTO -> Domain 변환은 Mapper에서만 수행한다.
- **DataSource는 전송만, Repository는 매핑·신호**: Remote DataSource는 Ktor/STOMP 호출과 DTO 반환까지만 담당하고, `AppResult` 변환·도메인 매핑·스트림 노출은 Repository가 담당한다.
- ViewModel/UI에서 Ktor·STOMP를 직접 호출하지 않는다 — 항상 `UseCase → Repository → DataSource` 경로를 지킨다.
- 실패는 항상 `AppError`로 매핑해 반환한다.
- 화면 전용 집계 모델이 필요하면 `Repository -> UseCase -> ViewModel` 순서로 전달한다.
- 페이징이 필요한 목록(마이페이지 참여 이력 등)은 `PagedResult` 기준으로 `nextCursor/hasNext`를 사용한다.

## 7. 프레젠테이션 규칙 (`composeApp`/`iosApp`)

- 화면은 상태 렌더링만 담당하고 비즈니스 판단을 하지 않는다.
- MVI 패턴을 사용한다.
- 화면 상태 관리는 아래 2개 프로퍼티와 1개 메소드를 고정으로 사용한다.
  - `uiState`: `StateFlow<ScreenUiState>` (Swift는 `@Published`/미러 프로퍼티)
  - `event`: `SharedFlow<ScreenEvent>` (Swift는 `PassthroughSubject`)
  - `action`: `onAction(ScreenAction)`
- UI는 `action(...)`만 호출하고 상태 변화는 `uiState` 구독으로만 반영한다.
- 단발성 효과(토스트/네비게이션/로그인유도/힌트 재생)는 `event`로만 전달한다.
- `event`는 `MutableSharedFlow(replay = 0)`를 기본값으로 사용한다.
- `onAction`(또는 `action`) 내부에서만 호출되는 처리 메서드는 반드시 `private`로 캡슐화한다.
- 외부(UI/다른 클래스)에서 직접 호출하면 안 되는 액션 핸들러는 public으로 노출하지 않는다.
- 로그인 필요 액션은 `pendingRoute/pendingAction` 규칙으로 처리한다.

## 8. 인증/권한 패턴 규칙

- 계정은 단일 유형(선생님·학생 공용)이며, 앱에서는 **게스트 vs 회원** 구분만 존재한다.
- 게스트 허용: `Home`(목록·방 정보), 무료 방 `Join/Waiting/Play/Result`, 무료 방 평가(별점)
- 로그인 필수: `MyInfo`(마이페이지·누적 리포트), `Payment`(유료 방 결제·내역), 기록 연동(claim), 유료 방 입장
- 토큰 체계:
  - 회원: JWT access 30분 + refresh 14일. **401 응답 시 refresh 후 1회 재시도**를 ApiClient 공통 레이어에서 처리한다(백엔드는 토큰 만료를 401로 응답 — 403은 권한 거부로만 해석한다).
  - 게스트: `join` 응답의 게스트 토큰(participationId 바인딩)을 세션 스코프로 보관하고, WebSocket CONNECT·제출·평가 API에 사용한다.
- 서버 오류 코드 연동:
  - `LOGIN_REQUIRED`(401, 게스트→유료 방) → 로그인 유도 + `pendingRoute` 재실행
  - `PAYMENT_REQUIRED`(402) → 결제 플로우(`Payment`)로 유도
- 서버 검증이 최종 권위다. 클라이언트 가드는 UX 목적이며, 가드를 통과했더라도 서버 4xx를 항상 처리한다.
- 세션 변경 감지는 화면 재생성이나 임의 강제 이동으로 해결하지 않고 `observeCurrentUser()` 기반 스트림으로 처리한다. 플랫폼별 메인 ViewModel은 세션 변화를 구독해 탭/게스트 상태를 재계산한다.

## 9. 비동기/상태 처리 규칙

- suspend 함수는 취소 가능성을 고려한다.
- 로딩/성공/실패 상태를 명시적으로 분리한다.
- 재시도 가능한 실패(네트워크)와 불가능한 실패(410 Gone 등)를 분리한다.
- STOMP 이벤트 수신은 shared의 세션 스트림(`Flow`) 하나로 일원화하고, ViewModel은 자신의 화면에 필요한 이벤트만 필터링해 소비한다.
- WebSocket 재연결은 shared 레이어가 담당한다: 재구독 → 스냅샷 조회 → 증분 반영(§2-1-2). ViewModel마다 재연결 로직을 중복 구현하지 않는다.
- 제출(답안·평가·결제 확인)은 중복 호출 방지를 위해 in-flight 상태를 `uiState`에 두고 버튼을 비활성화한다. 최종 중복 차단은 서버(409)가 한다.

## 10. 오류 처리 규칙

- 에러 타입은 아래와 같다:
  - 기본: `Unauthorized`, `PermissionDenied`, `ValidationFailed`, `NetworkError`, `NotFound`, `Unknown`
  - 확장: `LoginRequired`(유료 방 게스트), `PaymentRequired`(미결제), `Conflict`(닉네임 중복·중복 제출·재평가), `Gone`(종료 방·마감 문항·파기된 기록)
- 서버 오류 응답 `{code, message}`의 `code`(예: `NICKNAME_TAKEN`, `ALREADY_RATED`, `RECORD_PURGED`)를 `AppError`에 보존해 화면 문구 분기에 사용한다.
- 사용자 액션 에러는 사용자 문구로 변환 가능해야 한다.
- 로그에는 내부 원인(cause)을 남기고 UI에는 안전한 메시지만 노출한다.
- AI 분석 실패(`AI_FEEDBACK_FAILED`)는 에러 화면이 아니라 "분석 불가" 상태 표시로 처리한다 — 정오·점수 확인을 막지 않는다.

## 11. UI 컴포넌트 규칙

- 공통 UI 컴포넌트(`PassmateCard`, `PassmateTopBar`, `PassmateTimerBar` 등)를 우선 재사용한다.
- 화면별 중복 컴포넌트는 공통 컴포넌트로 승격한다 (`component` 패키지).
- 빈 상태/에러 상태 컴포넌트를 항상 제공한다.
- 접근성(콘텐츠 설명, 클릭 영역, 색 대비)을 기본 준수한다.

## 11-1. UI 화면 작성 규칙

- 화면 파일은 컨테이너 뷰와 콘텐츠 뷰를 분리한다.
- 컨테이너 뷰는 `PlayScreen`/`PlayView`처럼 ViewModel을 소유하고 상태 변경 감지만 담당한다.
- 컨테이너 뷰에서 처리하는 범위는 `LaunchedEffect`, `onReceive`, `onAppear`, `onChange`, 타이머, 네비게이션 이벤트 수집 같은 effect로 제한한다.
- 콘텐츠 뷰는 `private` 보조 뷰로 분리한다. Swift는 `private struct PlayContentView`, Compose는 `PlayContentScreen` 같은 형태를 기본으로 한다.
- 콘텐츠 뷰는 ViewModel을 직접 참조하지 않는다.
- 콘텐츠 뷰 입력은 `uiState`와 `onAction` 콜백만 기본으로 받는다.
- 선택한 보기 인덱스, 배너 페이지 같은 순수 UI 상태가 필요하면 콘텐츠 뷰에 필요한 최소 상태만 추가로 전달한다.
- `BottomSheet`, `ModalBottomSheet`, `Sheet`, `Dialog`, 풀스크린 오버레이 같은 화면 외부 계층 UI는 콘텐츠 뷰에 두지 않는다.
- 오버레이/모달 UI(퇴장 확인·음성 힌트 배너·평가 시트)의 표시 여부와 생명주기는 반드시 상위 `Screen/View`가 소유한다.
- 콘텐츠 뷰(`ContentScreen`/`ContentView`)는 단일 화면 본문 렌더링만 담당한다.
- 미리보기/프리뷰는 콘텐츠 뷰 기준으로 작성한다. Compose Preview와 SwiftUI Preview에서 ViewModel 없이 렌더링 가능해야 한다.
- 화면 렌더링 로직, 섹션 배치, 스타일링은 콘텐츠 뷰에 두고, 이벤트 수집/라우팅 연결은 컨테이너 뷰에 둔다.
- 새 화면을 만들 때는 `Screen/View + ContentScreen/ContentView` 2단 구성을 우선 적용한다.

### 11-1-1. 기준 예시

- Compose 예시

```kotlin
@Composable
fun WaitingRoomScreen(
    viewModel: WaitingRoomViewModel,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is WaitingRoomEvent.GameStarted -> onNavigate(NavigationAction.NavigateToPlay(event.pin))
                is WaitingRoomEvent.RoomCancelled -> onNavigate(NavigationAction.NavigateToHome)
            }
        }
    }
    WaitingRoomContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun WaitingRoomContentScreen(
    uiState: WaitingRoomUiState,
    onAction: (WaitingRoomAction) -> Unit
) {
    LazyColumn {
        items(uiState.participants) { participant ->
            Text(
                text = participant.nickname,
                modifier = Modifier.clickable {
                    onAction(WaitingRoomAction.ClickParticipant(participant.id))
                }
            )
        }
    }
}
```

- SwiftUI 예시

```swift
struct WaitingRoomView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: WaitingRoomViewModel

    var body: some View {
        WaitingRoomContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .gameStarted(let pin):
                onNavigationAction(.navigateToPlay(pin: pin))
            case .roomCancelled:
                onNavigationAction(.navigateToHome)
            }
        }
    }
}

private struct WaitingRoomContentView: View {
    let uiState: WaitingRoomUiState

    let onAction: (WaitingRoomAction) -> Void

    var body: some View {
        ScrollView {
            VStack {
                ForEach(uiState.participants, id: \.id) { participant in
                    Text(participant.nickname)
                        .onTapGesture {
                            onAction(.participantTapped(id: participant.id))
                        }
                }
            }
        }
    }
}
```

- 핵심 패턴: 상위 화면은 상태/effect 처리, 하위 콘텐츠 뷰는 `UiState + Action` 기반 렌더링 전용

## 11-2. 색상 토큰 규칙

- 화면 코드에 hex 색상 하드코딩을 금지한다. `PassmateColors` 시맨틱 토큰만 사용한다.
- 토큰 이름과 라이트/다크 값은 Compose(`PassmateColors.kt`)와 iOS(`PassmateColors.swift`)에서 1:1 동일하게 유지한다.
- 서드파티 브랜드 색상(Google/Kakao 로그인 버튼 등)만 hex 직접 사용을 허용한다.
- 상세 토큰 표는 디자인 시스템 문서(`docs/Passmate_디자인_시스템.md`, 추후 작성)에서 관리한다.

## 11-3. 아이콘 리소스 규칙

- 아이콘을 화면 코드에 벡터로 그리지 않는다(`ImageVector.Builder`·SwiftUI `Path`). 리소스 파일로 두고 화면은 이름만 참조한다.
- Compose는 공통 컴포넌트 `PassmateIcon(icon = PassmateIcons.X, …)`으로 그린다. 키는 `component/PassmateIcons.kt`의 enum이다.
- 파일 위치는 3곳이다. Compose 사본 2개는 **텍스트가 동일**해야 하며 `PassmateIconResourceTest`가 이를 강제한다.
  - Android: `composeApp/src/androidMain/res/drawable/ic_<snake_case>.xml` (VectorDrawable)
  - Desktop: `composeApp/src/jvmMain/resources/drawable/ic_<snake_case>.xml` (같은 파일의 사본)
  - iOS: `iosApp/iosApp/Assets.xcassets/<PascalCase>.imageset/` (SVG + `Contents.json`, 벡터 보존·템플릿 렌더링)
- **Android는 반드시 `R.drawable` 경로로 읽는다.** 클래스패스에서 읽는 방식은 안드로이드 스튜디오 프리뷰 렌더러가 찾지 못한다(compose-multiplatform #4476, wontfix). 이 리포는 프리뷰로 시안을 대조하므로 프리뷰에서 그려져야 한다.
- 리소스에는 **중립색만** 넣는다(`#FF000000` 스트로크·`#00000000` 채움). 표시 색은 호출부에서 `PassmateColors` 토큰으로 준다(§11-2). 테마 속성(`?attr/…`)·`@color/…` 참조는 Desktop 파서가 해석하지 못하므로 금지한다.
- 외부 아이콘 세트를 쓰면 **출처·버전·라이선스**를 파일 머리 주석에 남기고 버전을 고정한다(최신판이 시안과 방향이 다를 수 있다).
- 새 아이콘 추가는 파일 3개 + `PassmateIcons` 항목 1줄 + Android `drawableId()` 분기 1줄이면 끝난다. 테스트는 enum을 순회하므로 따로 추가하지 않는다.
- 미전환 잔재(후속 대상): `AlertCircleIcon`(Result·MyInfo)·`EmptyIcon`·`ErrorIcon`(CoinHistory)·`HintIcon`(VoiceHintBanner)·`PassyMascot`·`StudentAvatar`·jvm `GoogleSignInIcon`.

## 12. 테스트 규칙

- UseCase 단위 테스트를 우선 작성한다 (`shared/src/commonTest`).
- 가드 시나리오 테스트를 필수로 포함한다: 게스트 → 로그인 유도 → `pendingRoute` 복귀, 유료 방 게스트 차단(`LoginRequired`), 미결제 차단(`PaymentRequired`).
- 재접속 스냅샷 복구(스냅샷 ts 이전 이벤트 폐기)와 서버 `endsAt` 기반 남은 시간 렌더링은 단위 테스트 대상이다.
- 핵심 플로우(입장 → 대기실 → 풀이 → 결과 → 평가)는 스모크 테스트 대상이다.

## 13. 금지 규칙

- ViewModel 또는 UI에서 Ktor/STOMP 직접 호출 금지 (항상 UseCase→Repository→DataSource)
- 화면 코드에 아이콘 벡터 지오메트리를 직접 기술하는 구현 금지 (리소스 파일 + `PassmateIcon`, §11-3)
- UI에서 권한/입장 자격 판단 하드코딩 금지 (서버 코드 + 라우트 가드로 처리)
- 클라이언트에서 점수 계산·타이머 만료 판정·정오 판정 금지 (서버 권위 — 렌더링만)
- 정답을 클라이언트에 캐시하거나 `QUESTION_STARTED` 페이로드에 정답이 있다고 가정하는 구현 금지 (정답은 `QUESTION_ENDED`에서만 온다)
- domain/model에 mutable 상태(`var`) 남용 금지
- 에러를 `Exception` 문자열로만 처리하는 방식 금지
- 인증 필요 기능을 가드 없이 노출하는 구현 금지
- `event`를 상태처럼 재소비 가능한 구조로 저장하는 구현 금지
- `onAction` 전용 내부 메서드를 `public/internal`로 노출하는 구현 금지
- 계약 문서에 없는 필드·이벤트를 임의 추가하는 구현 금지 (계약 갱신이 먼저)
- iOS 16+ 전용 API를 `#available` 없이 화면에서 직접 사용하는 구현 금지 (최소 배포 타깃 15.0 — 공통 컴포넌트 안에서만 분기)

## 14. 코드 리뷰 체크리스트

- 모듈 경계 위반이 없는가 (shared에 UI 의존·ViewModel 없음)
- 로그인 가드 누락 기능이 없는가 (MyInfo·Payment·유료 방 입장·기록 연동)
- 에러 매핑이 `AppError`로 일관되는가 (서버 `code` 보존 포함)
- 상태 모델(`UiState`)이 로딩/실패를 포함하는가
- `uiState`, `event`, `action` 3프로퍼티 패턴을 준수하는가
- `onAction` 전용 처리 메서드가 `private`로 캡슐화되어 있는가
- 서버 권위 원칙을 지키는가 (endsAt 렌더만, 점수·정오 판정 없음)
- 재접속 복구가 스냅샷 프로토콜을 따르는가
- Compose 화면과 iosApp 미러의 UiState/Action/Event가 1:1인가
- 중복 코드가 공통화 가능한 수준인지 검토했는가

## 15. 적용 우선순위

1. 인증/권한/라우트 가드 규칙 (게스트·회원·유료 방)
2. 서버 권위 원칙 + 재접속 스냅샷 프로토콜
3. 도메인 반환 타입(`AppResult/AppError/PagedResult`) 일관성
4. 모듈 경계 및 레이어 분리 (DataSource=전송만/Repository=매핑·신호)
5. UI 상태/테스트 규칙

## 16. 코드 배치 규칙 (Kotlin/Swift 공통)

- 클래스/구조체 내부에서 프로퍼티 선언은 한 줄씩 선언하고 선언 사이를 개행한다.
- 메서드 내부에서는 변수 선언과 초기값 세팅을 상단에 모은다.
- 메서드 호출(비즈니스 실행/외부 호출)은 하단에 모은다.
- Kotlin 기준으로 `val name = 0` 같은 변수 선언 블록과 `invoke()` 호출 블록 사이에는 반드시 개행한다.
- `private` 메서드는 클래스 상단에 배치한다.
- `public` 메서드는 클래스 하단에 배치한다.
- `init` 블록은 클래스 하단(메서드 아래)에 배치한다.
- 지역변수 선언부는 줄 사이 개행 없이 연속 배치한다.
- 조건 분기는 `if-else`를 기본으로 사용한다.
- `if (condition) return` 형태의 조기 반환 패턴보다 `if-else`로 명시적으로 분기한다.
- Swift에서도 `guard ... else { return }` 남용보다 `if-else` 분기를 우선한다.
- 단, 중첩 인덴트 스코프가 3단계 이상 될 경우 가독성을 위해 가드(조기 종료)로 평탄화한다.

### 16-1. Kotlin 예시

```kotlin
class SampleViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase
) {
    private val _uiState = MutableStateFlow(JoinUiState())

    private val _event = MutableSharedFlow<JoinEvent>(replay = 0, extraBufferCapacity = 1)

    private fun buildRequest(pin: String?): JoinRequest {
        val resolvedPin = pin ?: ""
        val nickname = _uiState.value.nickname
        return JoinRequest(resolvedPin, nickname)
    }

    private suspend fun executeLoad(request: JoinRequest) {
        val result = getRoomInfoUseCase.invoke(request.pin)

        _uiState.update { it.copy(isLoading = false) }
        _event.emit(JoinEvent.LoadCompleted(result))
    }

    fun action(action: JoinAction) {
        when (action) {
            is JoinAction.Enter -> onEnter(action.pin)
            is JoinAction.Refresh -> onEnter(action.pin)
        }
    }

    fun onEnter(pin: String?) {
        val request = buildRequest(pin)
        val isFirstLoad = _uiState.value.roomInfo == null

        if (isFirstLoad) {
            _uiState.update { it.copy(isLoading = true) }
        }
        // 하단 호출부
        // launch { executeLoad(request) }
    }

    init {
        // 클래스 하단 init 블록
    }
}
```

#### Kotlin 조건문 예시

```kotlin
// 권장
if (isMember) {
    startPayment()
} else {
    showLoginRequired()
}

// 비권장
if (!isMember) return
startPayment()

// 예외 허용(3단계 이상 중첩 방지)
if (!isMember) return
if (!isPaidRoom) return
if (!hasPendingPayment) return
startPayment()
```

### 16-2. Swift 예시

```swift
final class SampleViewModel {
    private let useCase: GetRoomInfoUseCase

    private(set) var uiState: JoinUiState

    private let event = PassthroughSubject<JoinEvent, Never>()

    private func buildRequest(pin: String?) -> JoinRequest {
        let resolvedPin = pin ?? ""
        let nickname = uiState.nickname
        return JoinRequest(pin: resolvedPin, nickname: nickname)
    }

    private func executeLoad(request: JoinRequest) {
        let result = useCase.invoke(pin: request.pin)
        event.send(.loadCompleted(result))
    }

    func action(_ action: JoinAction) {
        switch action {
        case let .enter(pin):
            onEnter(pin: pin)
        case let .refresh(pin):
            onEnter(pin: pin)
        }
    }

    func onEnter(pin: String?) {
        let request = buildRequest(pin: pin)
        let isFirstLoad = uiState.roomInfo == nil

        if isFirstLoad {
            uiState.isLoading = true
        }
        // 하단 호출부
        executeLoad(request: request)
    }

    init(useCase: GetRoomInfoUseCase, initialState: JoinUiState) {
        self.useCase = useCase
        self.uiState = initialState
    }
}
```

#### Swift 조건문 예시

```swift
// 권장
if isMember {
    startPayment()
} else {
    showLoginRequired()
}

// 비권장
guard isMember else { return }
startPayment()

// 예외 허용(3단계 이상 중첩 방지)
guard isMember else { return }
guard isPaidRoom else { return }
guard hasPendingPayment else { return }
startPayment()
```
