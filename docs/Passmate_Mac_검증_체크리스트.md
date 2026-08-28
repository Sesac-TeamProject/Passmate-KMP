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
