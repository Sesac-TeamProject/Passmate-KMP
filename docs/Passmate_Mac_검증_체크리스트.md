# Passmate Mac/실기기 검증 체크리스트 — US2·US3·US7 (파트1)

> 2026-08-28 작성. 대상: `feature/us2-join-waiting-room`(develop 머지됨) + `feature/us3-realtime-play` + `feature/us7-voice-hint`.
> WSL 환경에서는 Kotlin(Android/JVM) 컴파일·테스트 30건만 검증됨 — **Swift·pbxproj·실기기 항목은 Mac에서 아래 순서로 확인한다.**
> 백엔드 API가 필요한 항목은 `[백엔드]` 표시 — 백엔드(전혜림) 해당 API 완성 후 진행.

## 1. Xcode 프로젝트 로드·빌드

- [ ] `iosApp/iosApp.xcodeproj` 열기 — 프로젝트 로드 오류 없음 (pbxproj 수동 편집분 확인)
- [ ] 신규 등록 파일 3개가 Project Navigator에 보이는지: `component/StudentAvatar.swift`(ID 0065) · `component/PassyMascot.swift`(0066) · `ui/play/VoiceHintPlayerView.swift`(0067) — **다음 가용 ID = 0068**
- [ ] Shared 프레임워크 빌드(iosSimulatorArm64) 성공 — Kotlin 1.9.20 · Ktor 2.3.12 · krossbow 5.12.0
- [ ] iosApp 타깃 빌드 성공 — 컴파일 실패 시 1순위 의심 지점(Kotlin↔Swift 인터롭 첫 사용처):
  - `JoinInputPolicy.companion.PIN_LENGTH` / `NICKNAME_MAX_LENGTH` (companion const 노출 방식)
  - `AppResultSuccess<AnyObject>` 캐스트 + `success?.value as? RoomInfo` 패턴
  - flatten된 클래스명: `ServerEventQuestionStarted`·`ServerEventParticipantLeft.companion.REASON_KICKED`·`SessionEventStreamStreamEventConnected/Received`·`AppErrorNotFound` 등
  - `QuestionDeadline.companion.fromServerTimes(endsAt:serverNow:)` · `RoomStatus.finished`(enum 엔트리 소문자 노출)
  - `KoinHelper.shared.*` getter 시그니처

## 2. 시뮬레이터 스모크 (백엔드 불필요)

- [ ] 앱 시작 → HomeView(임시 셸) → "PIN으로 입장하기" → JoinView push
- [ ] JoinView: PIN 6칸 입력(숨김 TextField 포커스·활성 칸 하이라이트), 닉네임 입력(12자 제한), 캐릭터 12종 렌더·선택 링, "기록을 남기려면 로그인" 행(게스트일 때만)
- [ ] StudentAvatarView 12종이 Figma 아바타와 일치하는지(코드 벡터 이식 — 34/36/44pt 크기별 확인)
- [ ] SignInView 렌더 + PassyMascotView(컴포넌트 승격 후에도 기존과 동일 렌더)
- [ ] PlayView·WaitingView 프리뷰(Preview Provider) 렌더

## 3. 실기기 (권한·카메라·오디오)

- [ ] "QR로 입장" → NSCameraUsageDescription 프롬프트 → 카메라 프리뷰 표시, QR 인식 시 PIN 자동 입력(쿼리 `pin=` 우선·6자리 추출), 닫기 버튼 동작
- [ ] ASWebAuthenticationSession Google 로그인 플로우(백엔드 `/auth/oauth/google?client=mobile` 리다이렉트 필요 시 `[백엔드]`)
- [ ] `[백엔드]` VoiceHintBannerView: HINT_PUBLISHED 수신 → AVPlayer 자동 재생·진행바·일시정지/이어 듣기·재생 완료 후 "다시 듣기" 칩·실패 시 "탭해서 다시 시도"
- [ ] **무음 스위치 ON 상태에서 힌트 재생되는지** — 안 되면 `AVAudioSession.setCategory(.playback)` 추가 필요(상태 시트 6번 "무음 모드 폴백" 조정 포인트)

## 4. 백엔드 연동 E2E `[백엔드]`

- [ ] PIN 입장: 잘못된 PIN 404 안내 · 종료 방 410 안내 · 닉네임 중복 409 안내 · 게스트 participantToken 보관
- [ ] 대기실: 참가자 초기 목록(REST) + PARTICIPANT_JOINED/LEFT 실시간 증분 · 강퇴(reason=KICKED) 시 안내 후 홈 · 나가기(DELETE participants/me)
- [ ] SESSION_STARTED 수신 → 1초 내 풀이 화면 전환 (SC-003)
- [ ] 문항 타이머가 서버 endsAt과 동기(기기 시계를 5분 틀어놓고도 정상인지 — QuestionDeadline 검증)
- [ ] 제출 → +점수·순위 ▲▼ 즉시 표시 · 마감 문항 410 → "이미 마감" · 중복 409 → "이미 제출"
- [ ] QUESTION_ENDED → 정답·해설·정답자 수 공개 / 미제출이면 "시간 종료" 처리
- [ ] 앱 강제 종료 후 재진입(또는 비행기 모드 토글) → 스냅샷 복구(현재 문항·타이머·내 답변·랭킹) · 스냅샷 ts 이전 이벤트 폐기 확인
- [ ] SESSION_ENDED → 최종 결과(포디움 TOP3·전체 랭킹·내 행 하이라이트·"n위 · 점수 · 정답 x/y")
- [ ] PTT 종료 → 학생 재생 시작 3초 이내 (SC-006 계측)

## 5. Android/Desktop 스모크

- [ ] Android 에뮬/실기기: zxing QR 스캔(CAMERA 권한 프롬프트) · ExoPlayer 힌트 재생 `[백엔드]`
- [ ] Desktop: QR 버튼 숨김 확인 · 힌트 도착 시 "재생 미지원" 안내 칩 `[백엔드]`
- [ ] `gradlew.bat :composeApp:run` 데스크톱 스모크 — Home→Join→(백엔드 없으면 네트워크 오류 스낵바까지)

## 6. 알려진 조정·협의 포인트

- BaseUrl: 로컬 백엔드 기준 기본값(`BaseUrl.*.kt`) — 배포 환경 전환은 빌드 설정 분리 예정
- 로그인 방식 협의 미결: 백엔드 명세서 `POST /auth/login/{provider}` vs 앱 딥링크 콜백(T022 유지 중) — 합의 시 AuthRepository 교체
- `ANSWER_SUBMITTED`(학생 화면 제출 현황) 존폐 — 백엔드 명세서엔 호스트 토픽 `SUBMISSION_UPDATED`만 존재
- 답 제출 `content` 규약(객관식=보기 원문·OX="O"/"X"·서술형=텍스트) — 백엔드 채점 비교 방식과 합의 필요
- M-05 "내 리포트 보기"=준비 중 안내(T062 파트2) · 가입 유도 버튼 미구현(T075 파트2)

## 7. v6 앱 확장 (T116~T120, 2026-08-29 — PR #9~#13)

> 신규 Swift 30개 + pbxproj idx **84~123** 등록(그룹 profile=0019·hostroom=0020 신설). **다음 가용 idx = 124.**

### 7-1. 빌드·인터롭 1순위 의심 지점

- [ ] pbxproj 로드: ui/profile(5) · ui/hostroom(15) · ui/mypage Reputation*(5) · ui/payment Earnings/SettlementAccount*(10) 전부 Navigator에 표시
- [ ] Kotlin enum 신규 노출: `BadgeType.firstRoom` 등 8종 · `ReportReason.nickname` 등 6종 · `SettlementStatus.scheduled/paid/held` · `QuestionType.multipleChoice`(리모컨·리포트 첫 사용)
- [ ] `Shared.HostLevel.verified` 모듈 한정 비교(ReputationView — 로컬 Swift HostLevel과 이름 충돌 지점)
- [ ] `SessionEventStreamWatcher.startAsHost(roomId:onEvent:)` 신규 메소드 + `ServerEventSubmissionUpdated`·`ServerEventProjectorConnected/Disconnected` 클래스명
- [ ] `PagedResult.items` 캐스트: `[HostedRoom]`·`[QuestionSetSummary]`·`[SettlementItem]` · `NextGrade.criteria`→`[GradeCriterion]`
- [ ] `CreateRoomUseCase.invoke(title:questionSetId:isPaid:entryFee:)` — `KotlinLong?`/`KotlinInt?` 래핑 전달부(CreateRoomViewModel.swift)

### 7-2. 시뮬레이터 스모크 (백엔드 불필요)

- [ ] 마이(MyInfoView) → "내가 만든 방"·"정산"·"내 명성·뱃지" 행 3개 push 진입
- [ ] ReputationView: 등급 카드(엠블럼·진행 바·조건 행 ✓/현재·목표)·뱃지 그리드 렌더(로딩 실패 화면이라도 크래시 없음)
- [ ] RoomListView 호스트 이름 탭 → HostProfileSheetView 시트(medium/large) · 신고 confirmationDialog 6종 · 차단 alert
- [ ] HostedRoomsView: + FAB → CreateRoomSheetView(세트 Menu·무료/유료 토글·유료 시 참가비 필드)
- [ ] RoomReportView: 개요/문항별/학생별 탭 전환 · 내보내기 → UIActivityViewController
- [ ] SessionControlView: 대기 패널→(백엔드 없으면 오류 토스트) · 타이머 링·제어 버튼 렌더
- [ ] EarningsView: 요약 카드·상태 칩 3종 · "계좌 관리" → SettlementAccountSheetView(숫자 키패드)

### 7-3. 백엔드 연동 E2E `[백엔드]`

- [ ] 명성: GET /users/me/grade·badges → 승급 조건 12/20·✓4.7 표기 · 프로필: 차단 후 목록에서 방 숨김 확인
- [ ] 방 생성: POST /rooms(세트 연결) → PIN 발급 스낵바 · 유료 선택 Lv.3 미만 403 문구
- [ ] 리모컨: 세션 시작→QUESTION_STARTED 반영·SUBMISSION_UPDATED 재조회·바로 마감·화면 잠금(SCREEN_LOCKED 학생 측 확인)·세션 종료→방 리포트 자동 전환
- [ ] 정산: earnings 요약·페이징 · 계좌 저장 PUT → 요약 행 갱신

## 8. 파트1 마감 2건 — PTT 송출(T121, PR #14)·설정 허브(T122, PR #15)

> T121: `VoiceHintRecorder.swift`(pbxproj idx 124) + Info.plist `NSMicrophoneUsageDescription`. T122: 설정 20파일(idx 125~144, mypage 15·payment 5). **다음 가용 idx = 145.**

- [ ] **PTT(iOS)**: 리모컨에서 길게 눌러 녹음 — 최초 마이크 권한 프롬프트 → 허용 후 다시 길게 → 녹음 중 표시 → 놓으면 업로드 `[백엔드]` · 500ms 미만 "너무 짧아요" · AVAudioSession playAndRecord 전환 후 힌트 재생(AVPlayer)과 충돌 없는지
- [ ] **PTT(Android 실기기)**: RECORD_AUDIO 런타임 권한 플로우 동일 확인 · m4a 업로드 → 학생 기기 자동 재생 3초 SLA `[백엔드]`
- [ ] **설정 허브(M-12)**: 마이 → "설정" → 프로필 카드(아바타·레벨 칩·코인) + 행 6종 렌더
- [ ] 계정 정보 시트: 닉네임 12자 제한·아바타 12종 선택 링 → 저장 → 카드 갱신 `[백엔드]`
- [ ] 결제 수단 시트: 기본값 로드(coins.defaultMethod)·라디오 5종 → 저장 `[백엔드]`
- [ ] 알림 설정 시트: 토글 즉시 저장·실패 시 원복 `[백엔드]`
- [ ] 로그아웃: 확인 → 홈 복귀·게스트 전환(isSignedIn=false) — 네트워크 꺼도 로컬 로그아웃 성공
- [ ] 회원 탈퇴: 확인 경고 문구 → 409(정산 대기·진행 중 방) 시 서버 메시지 표시 `[백엔드]`

## 9. 홈 셸·하단 4탭 (feature/home, 2026-08-30 — 파트2)

> 신규 Swift 10개(navigation 5 + mypage Settings 5) pbxproj idx **145~154**, Home* 5파일 삭제(HomeEvent 포함), MyInfo*↔Settings*↔JoinedRooms* 이름 재배치. **다음 가용 idx = 155.** 그룹 ID 신규 없음.

- [ ] 앱 시작 = 입장 폼(JoinView) + 하단 탭 4개(홈·내가 만든 방·참여한 방·마이) — 탭 바 tint Primary
- [ ] 게스트: 내가 만든 방/참여한 방/마이 탭 → SignInView push(탭 바 숨김) → 닫기 → 직전 탭 유지
- [ ] `[백엔드]` 회원: 참여한 방 탭(M-08) 로드 · 마이 탭(M-12) 프로필/코인/정산 카드 · 시트 4종 저장 후 카드 갱신 · 로그아웃 → 홈 탭 + 게스트 전환
- [ ] `[백엔드]` 홈 탭 입장 → 대기실 → 풀이 → 결과에서 뒤로가기 시 홈 탭 루트로(세션 엔트리 제거)
- [ ] 마이 → 설정 → 회원 탈퇴 확인 알림(409 시 서버 문구) `[백엔드]`
- [ ] Desktop `sh gradlew :composeApp:run`: 첫 화면 입장 폼 + 탭 4개 → 게스트로 3탭 클릭 시 로그인 화면(탭 바 숨김) → 홈 재클릭 시 폼 리셋 (샌드박스에선 화면 캡처 불가라 수동 확인)
- [ ] iOS: 홈 탭 마지막 행 "기록을 남기려면 로그인"이 플로팅 탭 바에 가려지는지 확인(가려지면 JoinView 하단 inset 추가 검토)
- [ ] Android: 홈 탭 "로그인" → 로그인 화면 "PIN으로 바로 입장 (게스트)" → 홈 입장 폼으로 복귀(로그인 화면 재등장 없음)
- [ ] `[백엔드]` Android/iOS: 게스트 → 마이 → 로그인 완료 → 홈 탭 폼에 로그인 행이 사라지고 마이 탭에 새 프로필 · 로그아웃 → 홈 폼에 로그인 행 복귀
- [ ] iOS: 게스트가 마이 탭 탭 → 현재 탭 스택 위에 SignIn push(탭 바 숨김), 하단 선택은 홈 유지(깜빡임 여부 확인)

## 10. iOS 15 호환 — 배포 타깃 15.0 복귀 (fix/ios15-deployment-target, 2026-08-31 — 파트2)

> 신규 Swift 4개 pbxproj idx **155~158**(navigation `RouteStackLevel` 155 · component `FlowLayout` 156 · `WeakTopicsRow` 157 · `SheetDetents` 158). **다음 가용 idx = 159.** 그룹 ID 신규 없음. 스펙: `docs/superpowers/specs/2026-08-31-ios15-compat-design.md`
> 이 Mac엔 iOS 26.3 시뮬 런타임만 있어 **iOS 15 경로(NavigationView 재귀 스택·UIKit 시트 브리지·FlowLayout)의 정본 검증은 실기기**. 시뮬(iOS 26)은 iOS 16+ 경로(네이티브 detents)만 탄다.

- [ ] 컴파일: pbxproj `IPHONEOS_DEPLOYMENT_TARGET = 15.0` 2곳 · `xcodebuild … build` 오류 0 · "only available in iOS 16" 0 · `grep -rn "NavigationStack\|presentationDetents\|: Layout" iosApp/iosApp --include='*.swift' | grep -v "^[^:]*:[0-9]*: *//"`는 `component/SheetDetents.swift`의 `#available` 분기 1건만(주석 미필터 시 `navigation/RouteStackLevel.swift`·`component/SheetDetents.swift` 주석 2건이 추가로 매치되어 3건으로 보임)
- [ ] 시뮬(iOS 26): 홈 폼 + 탭 4개 → 게스트 마이 탭 → SignIn push(탭 바 숨김) → 닫기 → 홈 탭·폼 유지
- [ ] 시뮬(iOS 26): 마이 → 설정 2단 push → 뒤로 2회 → 마이 루트 · 홈 탭 재선택 시 폼 리셋
- [ ] 시뮬(iOS 26): 방 목록 프로필 시트·마이 시트 4종 반높이(네이티브 detents 경로)
- [ ] **실기기(iOS 15)** — Xcode에서 본인 Apple ID 팀으로 서명(`Configuration/Config.xcconfig` `TEAM_ID` 설정) 후 설치:
  - [ ] 각 탭 루트 → push(SignIn·설정·명성·코인 내역·정산) → 뒤로 콜백으로 pop — push 중 탭 바 숨김, pop 후 탭 바 복귀
  - [ ] 시트 반높이: 방 목록 프로필·정산 계좌·마이 시트 4종이 반높이로 열리고 위로 끌면 전체 높이(UIKit 브리지 경로)
  - [ ] 칩 줄바꿈: 결과 화면 "보완할 주제" 행·평가 시트 태그 칩이 폭에 맞춰 줄바꿈되고 아래 요소와 겹치지 않음(FlowLayout 높이 고정)
  - [ ] `[백엔드]` Play → Result 전환: Play가 오른쪽으로 밀려나며 Result가 드러남(빈 화면·깜빡임 없음), Result "홈으로" → 탭 루트
  - [ ] 로그아웃/로그인 후 홈 탭 폼이 세션 상태를 다시 읽음(`sessionGeneration` 재생성)
  - [ ] 숨은 `NavigationLink`가 트리거되지 않는 경우(push가 전혀 안 됨) → 스펙 §2-5의 `ZStack` + `.hidden()` 대안으로 전환
