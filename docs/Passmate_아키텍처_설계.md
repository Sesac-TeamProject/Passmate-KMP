# Passmate 아키텍처 설계 — 클린아키텍처 + Koin + MVI

**작성**: 2026-08-26 · **상태**: 확정 (구현 착수 전 설계)
**담당 범위**: 이 문서와 구현은 **KMP 학생 앱(Passmate-KMP, 담당 홍희표, 서승혁)만** 대상이다. 백엔드(Passmate-Backend)는 전혜림, 웹(Passmate-Frontend)은 서승혁, 이한결 담당 — 서버는 계약 문서(contracts/)를 통해서만 연동하고, 이 리포에서 백엔드 코드를 작성하지 않는다.
**관계 문서**: 코드 레벨 규범은 [Passmate_코드_패턴_규칙.md](Passmate_코드_패턴_규칙.md)(이하 "규칙 문서")가 담당하고, 이 문서는 **구조·의존 방향·DI 배선**을 담당한다. 두 문서가 충돌하면 이 문서를 갱신하기 전까지 규칙 문서가 우선한다.
**계약**: REST·WebSocket DTO/이벤트의 단일 진실은 `../specs/001-passmate-mvp/contracts/` — 구현이 계약과 다르면 계약을 먼저 갱신한다.

## 0. 결정 요약

| 항목 | 결정 | 근거 |
|---|---|---|
| 아키텍처 | 클린아키텍처 — **현행 3모듈 유지 + 패키지로 레이어 분리** | 10 작업일 MVP·모바일 1인 담당. Gradle 모듈 분리의 강제력 대신 패키지 규칙+리뷰로 경계 유지 |
| DI | **Koin 3.5.x** (koin-core + koin-compose 1.1.x + koin-test) | 팀 결정. 템플릿 버전 유지(Kotlin 1.9.20) 선택에 따라 Koin 4.x가 아닌 3.5.x 계열 |
| 패턴 | **MVI** — 규칙 문서 §7의 3프로퍼티(`uiState`/`event`/`onAction`) 그대로 | 기존 규칙 계승. 본 문서는 `MviViewModel` 기반 클래스로 시그니처를 타입 강제 |
| ViewModel | **플랫폼별 1:1 유지** (composeApp Kotlin ↔ iosApp Swift 미러) — shared에 VM 금지 | 규칙 문서 §2 유지. Koin은 shared(도메인·데이터)와 composeApp(VM)까지만 관여, iOS는 KoinHelper 수동 주입 |
| 버전 | **템플릿 유지**: Kotlin 1.9.20 · CMP 1.5.12 · AGP 8.1.4 · coroutines 1.8.1 | 팀 결정(업그레이드 리스크 회피). plan.md의 Kotlin 2.2.x 표기와 다름 — 차이는 §9 결정 기록 참조 |

## 1. 모듈 ↔ 레이어 매핑

```
┌─────────────────────────────┐   ┌──────────────────────┐
│ composeApp (presentation)   │   │ iosApp (presentation) │
│ Android + Desktop/jvm, CMP  │   │ SwiftUI 미러          │
└──────────────┬──────────────┘   └──────────┬───────────┘
               │  UseCase·도메인 모델만 참조   │
               ▼                              ▼
┌─────────────────────────────────────────────────────────┐
│ shared                                                  │
│  ┌──────────────┐        ┌───────────────────────────┐  │
│  │ domain       │ ◄──────│ data                      │  │
│  │ UseCase      │  구현   │ DataSource(전송만)·DTO    │  │
│  │ Repo 인터페이스│        │ RepositoryImpl·Mapper     │  │
│  │ 모델·Policy   │        │                           │  │
│  └──────────────┘        └───────────────────────────┘  │
│  core: network · model(AppResult 등) · storage · di     │
└─────────────────────────────────────────────────────────┘
```

- 의존 방향은 **안쪽(domain)으로만** 향한다: `presentation → domain ← data`
- **의존 역전**: Repository 인터페이스는 domain에, 구현(`*RepositoryImpl`)은 data에 둔다. UseCase는 인터페이스만 안다
- `shared`는 UI 프레임워크 의존을 갖지 않는다 (규칙 문서 §2). 템플릿에 남아 있는 `GreetingViewModel`(shared/commonMain)은 이 규칙 위반이므로 **구현 착수 시 제거**한다

## 2. 패키지 구조 (기능 우선, 레이어 차선)

규칙 문서 §3의 기능 패키지를 유지하고, 각 기능 아래에 레이어를 둔다.

```
shared/src/commonMain/kotlin/org/sesacteamproject/passmate/
├── core/
│   ├── network/    # ApiClient(Ktor, 401 refresh)·StompClient·SessionEventStream
│   ├── model/      # AppResult · AppError · PagedResult
│   ├── storage/    # 토큰 저장 expect/actual
│   └── di/         # coreModule · 기능 모듈 집계 · initKoin
└── {auth, room, session, question, feedback, report, rating, payment, user}/
    ├── domain/
    │   ├── model/         # 불변 data class
    │   ├── repository/    # 인터페이스
    │   ├── usecase/       # operator fun invoke
    │   └── policy/        # 입력 검증 (최종 판정은 서버)
    ├── data/
    │   ├── remote/        # *RemoteDataSource — Ktor/STOMP 호출·DTO 반환까지만
    │   ├── dto/           # 계약 문서와 1:1
    │   ├── mapper/        # DTO → Domain
    │   └── repository/    # *RepositoryImpl — AppResult 변환·매핑·스트림
    └── di/                # 기능 Koin 모듈 (예: RoomModule.kt)

composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/
├── ui/{home, auth, join, waiting, play, result, mypage, payment}/
│   #  화면당: XxxScreen · XxxContentScreen · XxxViewModel · XxxUiState · XxxAction · XxxEvent
├── navigation/     # 라우트 규격은 규칙 문서 §2-1 준수
├── component/      # 공통 UI (PassmateCard 등)
├── theme/          # PassmateColors 시맨틱 토큰
├── mvi/            # MviViewModel (§5)
└── di/             # viewModelModule · koinScreenViewModel 헬퍼
```

### 레이어별 허용/금지

| 레이어 | 허용 의존 | 금지 |
|---|---|---|
| domain | kotlinx-coroutines, core/model | Ktor·serialization·Koin 어노테이션·DTO·플랫폼 API |
| data | domain, core/network·storage, Ktor, kotlinx-serialization, krossbow | UI, ViewModel |
| presentation | domain(UseCase·모델), core/model, CMP/SwiftUI, Koin | data 직접 참조(DataSource·DTO·Impl), Ktor/STOMP 직접 호출 (규칙 문서 §13) |

- 예외: `core/di`는 배선을 위해 data 구현체를 참조한다 (DI 조립 지점은 경계 규칙의 유일한 예외)

## 3. 데이터 흐름 (단방향)

```
[요청]  UI ─onAction(Action)→ ViewModel ─invoke→ UseCase → Repository(인터페이스)
                                                              └→ RepositoryImpl → DataSource → 서버
[응답]  서버 → DataSource(DTO) → RepositoryImpl(Mapper·AppResult) → UseCase → ViewModel
            └ ViewModel은 uiState 갱신(상태) 또는 event 발행(단발 효과)만 한다
[실시간] STOMP → StompClient → SessionEventStream(단일 Flow, core/network)
            └ 각 ViewModel은 자기 화면에 필요한 이벤트만 필터링해 소비 (규칙 문서 §9)
```

- 서버 권위 원칙(규칙 문서 §1)·재접속 스냅샷 프로토콜(§2-1-2)은 그대로 적용된다. 재연결·스냅샷 조회·증분 반영은 shared(core/network + session)가 담당하고 ViewModel은 결과만 소비한다

## 4. Koin DI 설계

### 4-1. 라이브러리

| 라이브러리 | 버전 | 위치 |
|---|---|---|
| `io.insert-koin:koin-core` | 3.5.x (예: 3.5.6) | shared commonMain |
| `io.insert-koin:koin-compose` | 1.1.x (예: 1.1.2, CMP 1.5 호환) | composeApp commonMain |
| `io.insert-koin:koin-test` | 3.5.x | shared commonTest |

- Ktor는 2.3.x(Kotlin 1.9 호환), krossbow는 Kotlin 1.9 호환 버전으로 구현 시 확정한다. **Koin 4.x·Ktor 3.x는 Kotlin 2.x 요구라 사용 불가** (버전 유지 결정의 귀결)

### 4-2. 모듈 구성 — 기능별 모듈

레이어별 거대 모듈(dataSourceModule/repositoryModule/…) 대신 **기능별 모듈**로 구성한다. tasks.md의 스토리 단위 증분(US1→US16)과 1:1로 대응 — 스토리를 붙일 때 해당 기능 모듈 하나만 추가하면 된다.

| Koin 모듈 | 등록 내용 | 위치 |
|---|---|---|
| `coreModule` | ApiClient, StompClient, SessionEventStream, TokenStorage | shared `core/di` |
| `authModule` `roomModule` `sessionModule` `questionModule` `feedbackModule` `reportModule` `ratingModule` `paymentModule` `userModule` | 기능별 DataSource · Repository(인터페이스 바인딩) · UseCase | shared 각 기능 패키지의 `di/` (집계는 `core/di`의 `initKoin`만 담당) |
| `viewModelModule` | 화면별 ViewModel | composeApp `di` |

**등록 스코프 규칙**:

- DataSource · Repository · SessionEventStream · ApiClient = `single`
- UseCase = `factory` (무상태이므로 매번 생성해도 무방, 상태 공유 사고 방지)
- ViewModel = `factory` (보관·수명은 Koin이 아니라 lifecycle `viewModel { }`이 담당 — §4-4)

```kotlin
// shared: core/di/CoreModule.kt
val coreModule = module {
    single { TokenStorage() }
    single { ApiClient(get()) }
    single { StompClient(get()) }
    single { SessionEventStream(get()) }
}

// shared: room/di/RoomModule.kt — 기능 모듈 예시
val roomModule = module {
    single { RoomRemoteDataSource(get()) }
    single<RoomRepository> { RoomRepositoryImpl(get()) }
    factory { GetRoomInfoUseCase(get()) }
    factory { JoinRoomUseCase(get()) }
}
```

### 4-3. 시작점 (플랫폼별)

shared에 `initKoin()` 하나만 두고 세 플랫폼이 각자 호출한다.

```kotlin
// shared: core/di/InitKoin.kt
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            coreModule,
            authModule, roomModule, sessionModule, questionModule,
            feedbackModule, reportModule, ratingModule, paymentModule, userModule
        )
    }
}
```

| 플랫폼 | 호출 지점 |
|---|---|
| Android | `PassmateApplication.onCreate()` — `initKoin { androidContext(this@PassmateApplication) }` + `viewModelModule` 추가 로드 |
| Desktop | `main()` 최상단 — `initKoin()` + `viewModelModule` 추가 로드 |
| iOS | shared `iosMain`의 `KoinHelper.doInitKoin()` — SwiftUI `App.init`에서 호출 |

### 4-4. composeApp 주입 — `koinScreenViewModel`

VM 주입 헬퍼는 하나만 둔다. **생성은 Koin, 보관·수명은 lifecycle**로 역할을 나눈다(템플릿 동봉 `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` 2.8.4 사용).

```kotlin
// composeApp: di/KoinScreenViewModel.kt
@Composable
inline fun <reified VM : ViewModel> koinScreenViewModel(): VM =
    viewModel { KoinPlatform.getKoin().get<VM>() }
```

- 호출은 **컨테이너 Screen에서만** 한다. ContentScreen은 VM을 참조하지 않는다 (규칙 문서 §11-1 유지)
- Android에서는 lifecycle 보관 덕에 회전 시 VM이 유지된다. Desktop은 단일 윈도우라 무의미하지만 동일 API로 통일한다
- ⚠️ 호환 리스크: lifecycle-viewmodel 2.8.4 ↔ CMP 1.5.12 조합은 템플릿이 동봉한 조합이지만 `viewModel { }` 컴포저블(-compose 아티팩트)이 컴파일되지 않으면 **폴백**으로 `remember { KoinPlatform.getKoin().get<VM>() }` + `DisposableEffect` 정리로 대체한다 (API 시그니처는 헬퍼 내부에 격리되어 화면 코드는 불변)

```kotlin
// composeApp: di/ViewModelModule.kt
val viewModelModule = module {
    factory { HomeViewModel(get(), get()) }
    factory { JoinViewModel(get(), get()) }
    factory { WaitingRoomViewModel(get(), get()) }
    factory { PlayViewModel(get(), get(), get()) }
    // 화면 추가 시 여기에 factory 1줄 추가
}
```

### 4-5. iOS 주입 — KoinHelper 명시 getter

Swift는 Kotlin reified 제네릭을 쓸 수 없으므로, shared `iosMain`의 `KoinHelper`가 **화면별 의존성을 명시 getter로 노출**하고 Swift ViewModel 생성자에 수동 주입한다.

```kotlin
// shared: iosMain …/core/di/KoinHelper.kt
object KoinHelper {
    fun doInitKoin() {
        initKoin()
    }

    fun getRoomInfoUseCase(): GetRoomInfoUseCase = KoinPlatform.getKoin().get()

    fun joinRoomUseCase(): JoinRoomUseCase = KoinPlatform.getKoin().get()

    fun sessionEventStream(): SessionEventStream = KoinPlatform.getKoin().get()
    // 화면(Swift VM) 추가 시 필요한 getter를 여기에 추가
}
```

```swift
// iosApp — Swift VM은 생성자 주입 (규칙 문서 §16-2 배치 준수)
let viewModel = WaitingRoomViewModel(
    joinRoomUseCase: KoinHelper.shared.joinRoomUseCase(),
    eventStream: KoinHelper.shared.sessionEventStream()
)
```

- getter가 늘어나는 보일러플레이트는 감수한다 — VM 미러 규칙(§0)을 유지하는 비용이며, Swift 쪽에서 Koin 컨테이너를 직접 만지지 않게 하는 격리 장치다

## 5. MVI 구체화 — MviViewModel

규칙 문서 §7의 3프로퍼티 패턴을 composeApp에서 **타입으로 강제**하는 기반 클래스를 둔다. (iosApp은 기존 관례대로 Base 없이 규칙 문서로 강제 — Swift 미러는 §16-2 예시 형태 유지)

```kotlin
// composeApp: mvi/MviViewModel.kt
abstract class MviViewModel<S : Any, A : Any, E : Any>(initialState: S) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    protected val _event = MutableSharedFlow<E>(replay = 0, extraBufferCapacity = 1)
    val event: SharedFlow<E> = _event.asSharedFlow()

    abstract fun onAction(action: A)
}
```

- `onAction` 내부에서만 호출되는 처리 메서드는 `private` (규칙 문서 §7·§13)
- 단발 효과(토스트·네비게이션·힌트 재생)는 `event`로만, 상태는 `uiState`로만 (규칙 문서 §7)
- `viewModelScope`는 ViewModel 상속으로 확보 — 화면 이탈 시 진행 중 작업 자동 취소
- 화면 전환은 서버 이벤트로만 일어난다는 세션 플로우 규칙(§2-1-2)에 따라, `GAME_STARTED` 같은 전환 트리거는 VM이 `event`로 변환해 Screen 컨테이너가 네비게이션한다 (규칙 문서 §11-1-1 예시와 동일)

## 6. 테스트 전략

| 대상 | 방법 | Koin 사용 |
|---|---|---|
| UseCase (shared commonTest) | fake Repository를 **생성자 주입** | 사용 안 함 — 클린 레이어 덕에 DI 프레임워크 불필요 |
| ViewModel (composeApp test) | fake UseCase 생성자 주입 | 사용 안 함 |
| Koin 배선 정합성 | `checkModules()` 테스트 1개 — 등록 누락·순환을 실행 전에 검출 | koin-test |

- 우선 대상은 규칙 문서 §12를 따른다: 가드 시나리오(로그인 유도·유료 방 차단), 재접속 스냅샷 복구, `endsAt` 렌더링

## 7. 적용 순서 (구현 로드맵 — 본 문서는 설계까지만)

1. `gradle/libs.versions.toml`에 koin-core·koin-compose·koin-test·ktor 2.3.x 추가
2. 템플릿 잔재 정리 — shared의 `GreetingViewModel` 제거(§1 규칙 위반), `Greeting`/`Platform` 정리
3. `core/`(network·model·storage) 골격 + `coreModule` + `initKoin` + 플랫폼 시작점 3곳 — tasks.md **T021**(mobile shared 기반)·**T022**(로그인)에 대응
4. 스토리 증분마다: 기능 패키지(domain→data) + 기능 Koin 모듈 + composeApp 화면·VM + `viewModelModule` 1줄 + (iOS 시) KoinHelper getter
5. `checkModules` 테스트는 3단계에서 함께 추가

## 8. 코드 리뷰 체크 추가 항목 (규칙 문서 §14에 더해)

- [ ] presentation이 data 구현(DataSource·DTO·Impl)을 직접 참조하지 않는가
- [ ] Repository 인터페이스가 domain에, 구현이 data에 있는가
- [ ] Koin 등록 스코프가 규칙(§4-2)대로인가 (Repo=single, UseCase·VM=factory)
- [ ] 새 화면이 `koinScreenViewModel`(컨테이너에서만) + `viewModelModule` factory로 배선됐는가
- [ ] iOS 화면 추가 시 KoinHelper getter가 함께 추가됐는가

## 9. 결정 기록 (ADR 요약)

| # | 결정 | 대안 | 이유 |
|---|---|---|---|
| 1 | 현행 3모듈 + 패키지 레이어 | 레이어별/기능별 Gradle 모듈 | 10일 MVP·1인 담당. 컴파일 강제력보다 단순함. 경계는 §2 표+리뷰 체크로 유지 |
| 2 | Koin 3.5.x | Koin 4.x | 팀이 템플릿 버전(Kotlin 1.9.20) 유지를 선택 — Koin 4.x·Ktor 3.x는 Kotlin 2.x 요구. plan.md의 "Kotlin 2.2.x" 표기와 다르므로, 추후 업그레이드 시 Koin 4.x·Ktor 3.x 이관을 함께 검토 |
| 3 | 기능별 Koin 모듈 | 레이어별 모듈 | 스토리 단위 증분(US1~16)과 1:1 — 기능 추가가 모듈 1개 추가로 끝남 |
| 4 | VM 플랫폼별 1:1 유지 | shared 공유 VM | 규칙 문서 §2 기존 결정 유지. Swift에서 Flow 직접 구독 보일러플레이트 회피, 미러 관례 유지 |
| 5 | UseCase=factory, Repo=single | 전부 single | UseCase는 무상태 — factory가 상태 공유 사고를 구조적으로 차단. Repo·스트림은 연결·캐시 보유라 single |
| 6 | VM 생성=Koin, 보관=lifecycle | Koin scope로 수명 관리 | Koin scope는 화면 수명과 결합하는 코드가 더 필요. lifecycle이 이미 해결한 문제를 중복 해결하지 않음 |
