# iOS 15 호환 — 배포 타깃 15.0 복귀 설계

**작성**: 2026-08-31 · **상태**: 확정 (구현 착수 전 설계)
**브랜치**: `fix/ios15-deployment-target` ← `feature/home`(4888f0f). PR은 `develop` 대상, #18 위에 스택.
**관계 문서**: 홈 셸 스펙 [2026-08-30-home-shell-tabs-design.md](2026-08-30-home-shell-tabs-design.md) §1-4·§5(iOS 행을 이 문서가 대체), 규칙 문서 §2-1·§13(이 문서 §6에서 개정), Mac 체크리스트 §10(신설).

## 0. 결정 요약

| 항목 | 결정 | 근거 |
|---|---|---|
| 최소 배포 타깃 | **iOS 15.0** (현재 16.0 → 복귀) | 팀 실기기가 iOS 15. 템플릿 원본도 15.0이었고 PR #16이 Mac 컴파일을 위해 16.0으로 임시 상향 |
| 탭 셸 | **`NavigationView`(stack) 하나가 `TabView`를 감싸는 단일 스택** + 재귀 `RouteStackLevel` | iOS 15 SwiftUI에 배열 push·push 시 탭 바 숨김 API가 없음. push 시 TabView 전체가 밀려나 탭 바 숨김이 공짜. Android·Desktop도 단일 스택이라 3플랫폼 일치 |
| 칩 줄바꿈 | `Layout` 프로토콜(16+) 3벌 → **데이터 기반 `FlowLayout` 공통 1벌**(iOS 14+ 기법) | 15에는 자식 열거 공개 API가 없음. 중복 3벌 통합(규칙 §11) |
| 시트 반높이 | **`passmateDetents()` 공통 modifier** — 16+ 네이티브 / 15 UIKit `UISheetPresentationController` 브리지 | 실기기(15)에서도 시안대로 반높이 시트 유지 |
| 화면 파일 | **무변경** | 화면은 콜백만 받고 `NavigationStack`은 `ContentView`만 알고 있음. 시스템 내비바 사용 0건 |

## 1. 배경·감사 결과

배포 타깃을 15.0으로 덮어써 빌드(`-continue-building-after-errors`)한 결과 오류 **20건 / 8파일**, 3종류:

| 종류 | 위치 | 처리 |
|---|---|---|
| `NavigationStack(path:)`·`.navigationDestination(for:)`·`.toolbar(.hidden, for: .tabBar)` | `ContentView.swift:107,109,112` | §2 |
| `private struct FlowLayout: Layout`(`ProposedViewSize`·`callAsFunction`) ×3 | `ResultView.swift:344`·`RatingSectionView.swift:135`·`JoinedRoomsView.swift:390` | §3 |
| `.presentationDetents` ×5 | `RoomListView:63`·`ResultView:56`·`HostedRoomsView:63`·`EarningsView:49`·`MyInfoView:86` | §4 |

15에서 문제없는 것(컴파일 확인): `#Preview` 37건, `Text.kerning` 247건, `TabView`, Combine, `.refreshable`·`.searchable` 미사용. Kotlin/Native 프레임워크는 배포 타깃 무관.

`develop`의 `ContentView`도 이미 `NavigationStack`을 쓰고 있었다(Mac에서 컴파일된 적이 없어 15.0 설정이 검증된 값이 아니었음). 즉 이 작업은 "되돌리기"가 아니라 **iOS 15 호환의 최초 구현**이다.

## 2. 탭 셸 — `NavigationView` 단일 스택

**파일**: `iosApp/iosApp/ContentView.swift`(수정), `iosApp/iosApp/navigation/RouteStackLevel.swift`(신규)

### 2-1. 상태

- 탭별 경로 4개(`homePath`·`hostedRoomsPath`·`joinedRoomsPath`·`myInfoPath`)와 `currentPath` 스위치를 제거하고 **`@State private var path: [Route] = []` 하나**로 대체한다.
- `selectedTab`·`sessionGeneration`·`shellViewModel`은 그대로.

### 2-2. 구조

```swift
NavigationView {
    TabView(selection: tabSelection) { /* 탭 루트 4개, 기존 그대로 */ }
        .navigationBarHidden(true)
        .background(
            NavigationLink(isActive: isStackActive) {
                RouteStackLevel(path: $path, index: 0) { route, path in
                    destinationView(for: route, path: path)
                }
            } label: { EmptyView() }
            .isDetailLink(false)
        )
}
.navigationViewStyle(.stack)
.id(sessionGeneration)   // 기존에는 TabView에 걸려 있던 .id를 NavigationView로 올린다
.tint(PassmateColors.primary)
```

- `isStackActive`: get `!path.isEmpty`, set `false`이면 `path = []`.
- `destinationView(for:path:)`(line 117~211)는 **본문 무변경**. 탭 루트 콜백의 `homePath.append(...)` 등은 `path.append(...)`로 치환만 한다.
- 셸 이벤트: `.navigateToTab(tab)` → `selectedTab = tab`(기존 동일), `.requireSignIn` → `path.append(.signIn)`.
- `onSignedOut`·`onAccountDeleted`·`onSignedIn`은 기존처럼 `path = []` 후 `sessionGeneration += 1`. `.id`가 `NavigationView`에 있으므로 스택 전체가 즉시 루트로 재생성된다(pop 애니메이션 없음 — 현재와 동일).

### 2-3. `RouteStackLevel`

경로의 한 레벨을 그리는 재귀 뷰. **레벨당 숨은 `NavigationLink`는 정확히 1개**(iOS 15 `NavigationView`는 한 뷰에 링크가 여럿이면 오동작).

```swift
struct RouteStackLevel<Destination: View>: View {
    @Binding var path: [Route]
    let index: Int
    let destination: (Route, Binding<[Route]>) -> Destination
    @State private var shownRoute: Route?   // 이 레벨이 마지막으로 그린 라우트

    var body: some View {
        content
            .navigationBarHidden(true)
            .background(
                NavigationLink(isActive: isNextActive) {
                    RouteStackLevel(path: $path, index: index + 1, destination: destination)
                } label: { EmptyView() }
                .isDetailLink(false)
            )
            .onAppear { syncShownRoute(path) }
            .onChange(of: path) { syncShownRoute($0) }
    }
}
```

- **렌더 규칙**: `index < path.count`이면 `path[index]`를, 아니면 `shownRoute`를 그린다. 경로가 줄어들어 밀려나가는 레벨이 pop 애니메이션 중 빈 화면이 되지 않게 하기 위함. 둘 다 없으면 `EmptyView`.
- **`isNextActive`**: get `path.count > index + 1`; set `false`이고 `path.count > index + 1`이면 `path.removeSubrange((index + 1)...)` — 사용자 pop(뒤로가기 콜백이 아닌 시스템 pop)과 배열을 동기화한다.
- **`syncShownRoute`**: `index < path.count`일 때만 `shownRoute = path[index]`.
- **Result 전환** `[join, waiting, play] → [result]`(`PlayView.onOpenResult`, 스펙 §1-5 무변경): 레벨0 내용이 Result로 바뀌고 레벨0의 링크가 비활성 → Play(레벨2)가 오른쪽으로 밀려나며 Result가 드러난다. 레벨1·2는 `shownRoute`로 마지막 화면을 유지한 채 밀려난다.
- **팝 콜백**(`popOnce`·`path = []`)은 배열만 바꾸고, 링크 `isActive`가 이를 따라 pop한다.

### 2-4. 동작 차이(의도)

- 탭 전환 시 스택 보존 없음. 현재도 push 중엔 탭 바가 숨어 탭 전환이 불가능하므로 체감 차이 없음. Android(플랫 그래프 `popUpTo(Home)`)·Desktop(스택을 `[탭 루트]`로 교체)과 동일.
- 스와이프 백: 현재(`navigationBarBackButtonHidden`)도 꺼져 있고 그대로. 뒤로가기는 화면의 `onBack` 콜백.
- 시스템 내비바는 루트·모든 레벨에서 숨김(화면들이 자체 헤더 사용, `navigationTitle` 사용 0건).

### 2-5. iOS 15 `NavigationView` 완화책

- `.navigationViewStyle(.stack)` 필수(iPad에서 split 스타일로 바뀌는 것 방지).
- `.isDetailLink(false)` 모든 숨은 링크에 적용.
- 숨은 링크는 `.background`에 둔다. iOS 15에서 `.background` 속 링크가 트리거되지 않는 보고가 있으므로, 실기기에서 재현되면 `ZStack { content; NavigationLink(...) { EmptyView() }.hidden() }` 형태로 전환한다(동작 규칙은 동일).
- 한 번의 상태 변경으로 **push 2단계 이상**을 만들지 않는다(현재 코드는 전부 1단계 push). 향후 `pendingRoute`(로그인 후 원래 화면 복귀)는 **최상단 교체**(`path[path.count - 1] = target`, 같은 레벨 내용 교체)로 구현해 pop+push 동시 변경을 피한다.

## 3. `FlowLayout` · `WeakTopicsRow`

**파일**: `iosApp/iosApp/component/FlowLayout.swift`(신규), `iosApp/iosApp/component/WeakTopicsRow.swift`(신규·승격). 호출부: `RatingSectionView.swift`(수정), `ResultView.swift`·`JoinedRoomsView.swift`(private `FlowLayout`·`WeakTopicsRow` 삭제, 공통 사용).

### 3-1. `FlowLayout`

```swift
struct FlowLayout<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    init(_ data: Data, id: KeyPath<Data.Element, ID>, spacing: CGFloat = 8,
         @ViewBuilder content: @escaping (Data.Element) -> Content)
}
```

- 구현: `GeometryReader`로 컨테이너 폭을 얻고, `ZStack(alignment: .topLeading)` 안의 `ForEach`에 `alignmentGuide(.leading)`·`alignmentGuide(.top)`으로 줄바꿈 오프셋을 누적 계산한다(iOS 14+ 표준 기법). 전체 높이는 `PreferenceKey`로 되읽어 `.frame(height:)`에 고정 → 부모 레이아웃에서 크기가 정확히 잡힌다(기존 `Layout.sizeThatFits`와 동일 결과).
- 가로·세로 간격은 `spacing` 하나(기존과 동일). 정렬은 leading 고정.
- 데이터가 비면 높이 0.

### 3-2. `WeakTopicsRow`

`ResultView`·`JoinedRoomsView`에 글자 하나 안 다르게 복사된 "보완할 주제" 라벨+칩 행을 `component/WeakTopicsRow.swift`로 승격한다. 라벨이 칩들과 같은 흐름에 있으므로 private 아이템 enum으로 표현한다:

```swift
private enum Item: Hashable { case label; case topic(String) }
FlowLayout([.label] + topics.map(Item.topic), id: \.self, spacing: 8) { item in ... }
```

- 인터페이스 `WeakTopicsRow(topics: [String])`, `topics`가 비면 아무것도 그리지 않음(기존 동일).

### 3-3. `RatingSectionView`

`FlowLayout(spacing: 8) { ForEach(RatingTag.companion.all, id: \.self) { tagChip($0) } }` → `FlowLayout(RatingTag.companion.all, id: \.self, spacing: 8) { tagChip($0) }`.

## 4. 시트 반높이 — `passmateDetents`

**파일**: `iosApp/iosApp/component/SheetDetents.swift`(신규). 호출부 5곳 `.presentationDetents([...])` → `.passmateDetents([...])`.

```swift
enum PassmateSheetDetent { case medium, large }

extension View {
    func passmateDetents(_ detents: [PassmateSheetDetent]) -> some View
}
```

- `if #available(iOS 16, *)` → `presentationDetents(Set(...))` 네이티브.
- else → `UIViewControllerRepresentable`(빈 컨트롤러)을 `.background`로 얹고, 컨트롤러의 `viewWillAppear`에서 `sheetPresentationController?.detents = [...]`를 설정한다(`UISheetPresentationController.Detent.medium()/large()`는 iOS 15.0+, `sheetPresentationController`는 조상 시트를 자동 탐색).
- 그래버·dim 등 추가 옵션은 두지 않는다(현재 SwiftUI 코드도 안 씀).

## 5. 배포 타깃 · pbxproj

- `IPHONEOS_DEPLOYMENT_TARGET = 16.0` → **`15.0`** 2곳(Debug/Release, `project.pbxproj:856,919`).
- 신규 Swift 4파일 등록 — idx **155~158**(ID 형식 `A1010001{idx}AABBCCDDEEFF0{idx}`/`A1011001{idx}AABBCCDDEEFF0{idx}`), **다음 가용 idx = 159**:
  - 155 `navigation/RouteStackLevel.swift` → 그룹 `A1012019…0019` (navigation)
  - 156 `component/FlowLayout.swift`, 157 `component/WeakTopicsRow.swift`, 158 `component/SheetDetents.swift` → 그룹 `A1012021…0021` (component)
- 그룹 ID(`A10120xx`) 신설·변경 없음. 편집 후 중복 ID 검사(체크리스트 §9 명령).
- Xcode 빌드 스크립트가 `gradlew`에 +x를 붙이므로 커밋 전 `git checkout -- gradlew`.

## 6. 문서·규칙 개정

- **규칙 §2-1**(팀 규칙 — PR에서 홍희표 님 리뷰 요청, §2-1-1 개정 때와 같은 절차):
  - "iOS는 `NavigationStack`(`NavigationPath`)을 사용한다." → **"iOS는 `NavigationView`(stack 스타일) 위에 상태 배열 `[Route]` 기반 push(`RouteStackLevel`)를 사용한다."**
  - 추가: **"iOS 최소 배포 타깃은 15.0이다. iOS 16+ 전용 API(`NavigationStack`·`presentationDetents`·`Layout` 등)는 화면에서 직접 쓰지 않고 `#available` 분기를 가진 공통 컴포넌트(`component/`) 뒤로만 사용한다."**
- **규칙 §13** 금지 항목 추가: "iOS 16+ 전용 API를 `#available` 없이 화면에서 직접 사용하는 구현 금지 (최소 배포 타깃 15.0)".
- **홈 셸 스펙 §1-4 iOS 행·§5 첫 줄**: "탭마다 `NavigationStack`" 서술을 "`NavigationView` 단일 스택 — [2026-08-31 iOS 15 호환 스펙](2026-08-31-ios15-compat-design.md) §2"로 정정(당시 결정 기록은 취소선 없이 한 줄 정정).
- **Mac 체크리스트 §10 신설**: iOS 15 호환 — 15.0 컴파일·iOS 26 시뮬 스모크·실기기 항목(§8 참조), idx 155~158·다음 가용 159.
- 주석: `ContentView` 머리말("탭마다 NavigationStack" → 단일 스택), `FlowLayout`의 "(iOS 16+)" 제거.
- 코드 패턴 규칙 §8·§2-1-2 등 나머지 문구는 무변경(경로 의미·가드·Result 클리어 범위 동일).

## 7. 브랜치 · PR

- 브랜치 `fix/ios15-deployment-target` ← `feature/home`(4888f0f). 커밋은 덩어리별: 셸 / FlowLayout·WeakTopicsRow / 시트 브리지 / 배포 타깃·pbxproj / 문서.
- PR → `develop`, **#18 위에 스택**. #18(17커밋) 리뷰 중이라 섞지 않는다. #18 병합 후 `git rebase develop` → 이 변경만 남음.
- PR #16: "16.0은 Mac 컴파일용 임시, 후속 PR에서 iOS 15 호환과 함께 15.0 복귀" 코멘트. #16 본문이 #17 내용으로 잘못 들어가 있어 올바른 본문 초안을 제공(교체는 사용자).
- push·PR 생성은 사용자 승인 후.

## 8. 검증

1. **컴파일(필수)**: pbxproj 15.0 상태에서 `xcodebuild -project … -scheme iosApp -configuration Debug -destination "generic/platform=iOS Simulator" -derivedDataPath iosApp/build/DerivedData CODE_SIGNING_ALLOWED=NO build` → 오류 0, "only available in iOS 16" 경고 0. Kotlin 무변경이지만 `sh gradlew :composeApp:jvmTest :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid` 1회 회귀 확인.
2. **iOS 26.3 시뮬레이터 스모크**(iOS 16+ 경로, 비목킹 — 데이터 화면의 "불러오지 못했어요"는 정상):
   - 홈 폼 + 탭 4개 표시 → 게스트가 마이 탭 → SignIn push(탭 바 숨김) → 뒤로 → 홈 탭·폼 유지
   - 홈에서 방 목록 push → 뒤로 / 마이→설정 2단 push·pop
   - 방 목록 프로필 시트·마이 시트 4종 반높이(네이티브 경로)
   - 홈 탭 재선택 시 폼 리셋(`onOpenPinEntry`/`path = []`)
   - 스크린샷은 PR 첨부용으로 보관.
3. **실기기 iOS 15 = 정본**(iOS 15 경로: `NavigationView` 재귀 스택·UIKit 시트 브리지·`FlowLayout`). 이 Mac엔 iOS 26.3 시뮬 런타임만 있어 iOS 15 시뮬은 불가 → **사용자가 Xcode에서 본인 Apple ID 팀으로 서명해 기기 설치**(`Config.xcconfig`의 `TEAM_ID` 비어 있음). 체크리스트 §10 항목: 각 화면 push/pop, push 시 탭 바 숨김, 시트 반높이, 칩 줄바꿈, Result 전환 애니메이션(Play→Result), 로그아웃 후 홈 복귀.
4. Swift 테스트 타깃 없음(`xcodebuild -list`: iosApp만). `RouteStackLevel`의 순수 로직은 배열 동기화 한 줄이라 2·3의 뷰 검증으로 갈음.

## 9. 범위 밖

- 탭별 백스택 보존(3플랫폼 모두 미지원 — 필요해지면 별도 설계).
- 스와이프 백 제스처 복원.
- `pendingRoute`(후속 작업 — §2-5의 최상단 교체 지침만 남김).
- PR #16 본문 교체·코멘트 게시(사용자 액션).
