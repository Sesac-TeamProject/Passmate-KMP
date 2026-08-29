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
