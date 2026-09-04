import SwiftUI
import Shared

// 캐릭터 선택 한 줄에 놓는 수 (M-01 입장 폼과 동일)
private let avatarsPerRow = 6

// 유료 방 입장 결제 (M-01 v2 / W-11) — Compose PaymentScreen.kt 미러.
// 포트원 결제창(웹뷰) 오버레이는 이 컨테이너가 소유한다 (규칙 §11-1).
struct PaymentView: View {
    let pin: String

    @StateObject private var viewModel = PaymentViewModel(
        getRoomInfoUseCase: KoinHelper.shared.getRoomInfoUseCase(),
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        requestChargeUseCase: KoinHelper.shared.requestChargeUseCase(),
        confirmChargeUseCase: KoinHelper.shared.confirmChargeUseCase(),
        payEntryFeeUseCase: KoinHelper.shared.payEntryFeeUseCase(),
        joinRoomUseCase: KoinHelper.shared.joinRoomUseCase(),
        coinPolicy: KoinHelper.shared.coinPolicy(),
        joinInputPolicy: KoinHelper.shared.joinInputPolicy()
    )

    var onEnterRoom: (String) -> Void = { _ in }

    var onSignInRequired: () -> Void = {}

    var onBack: () -> Void = {}

    @State private var notice: String? = nil

    // 결제 수단 드롭다운 펼침은 순수 UI 상태 — 컨테이너가 소유한다 (규칙 §11-1)
    @State private var isMethodExpanded = false

    var body: some View {
        ZStack {
            PaymentContentView(
                uiState: viewModel.uiState,
                onAction: handleAction,
                isMethodExpanded: isMethodExpanded,
                onToggleMethodExpanded: { isMethodExpanded.toggle() },
                onBack: onBack
            )
            if let request = viewModel.uiState.checkout {
                PortOnePaymentView(request: request) { result in
                    viewModel.action(.receivePortOneResult(result: result))
                }
                .background(PassmateColors.surface.ignoresSafeArea())
            }
            if let notice {
                VStack {
                    Spacer()
                    Text(notice)
                        .font(.system(size: 13))
                        .foregroundColor(PassmateColors.surface)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(PassmateColors.textPrimary.opacity(0.9))
                        .cornerRadius(10)
                        .padding(.bottom, 24)
                }
            }
        }
        .onAppear { viewModel.action(.start(pin: pin)) }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .enterRoom(pin):
                onEnterRoom(pin)
            case .signInRequired:
                onSignInRequired()
            case let .showNotice(message):
                showNotice(message)
            }
        }
    }

    private func handleAction(_ action: PaymentAction) {
        if case .selectMethod = action {
            isMethodExpanded = false
        }
        viewModel.action(action)
    }

    private func showNotice(_ message: String) {
        notice = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            if notice == message {
                notice = nil
            }
        }
    }
}

private struct PaymentContentView: View {
    let uiState: PaymentUiState

    let onAction: (PaymentAction) -> Void

    let isMethodExpanded: Bool

    let onToggleMethodExpanded: () -> Void

    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                if uiState.isLoading {
                    centerProgress
                } else if uiState.hasLoadError {
                    retryState
                } else {
                    loaded
                }
                Spacer().frame(height: 24)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var header: some View {
        ZStack(alignment: .topLeading) {
            PassmateBackButton(onClick: onBack)
                .padding(.leading, 20)
                .padding(.top, 14)
            HStack {
                Spacer()
                PassyMascotView()
                    .frame(width: 68, height: 75)
                    .padding(.trailing, 28)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text("유료 방 입장")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.primaryDeep)
                Text("유료 방이에요 — 결제 후 입장할 수 있어요")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.leading, 60)
            .padding(.trailing, 24)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
    }

    private var loaded: some View {
        PassmateCard {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("코인 충전하고 입장하기")
                        .font(.system(size: 20, weight: .bold))
                        .kerning(-0.4)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text("PIN \(formatPin(uiState.room?.pin)) · 방 정보를 확인하세요")
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                if let room = uiState.room {
                    RoomPreviewView(room: room)
                }
                entryFeeSection
                nicknameField
                avatarField
                if !uiState.hasEnough {
                    methodField
                }
                if let error = uiState.errorMessage {
                    Text(error)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.wrongPinkText)
                }
                payButton
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 26)
        }
        .padding(.horizontal, 20)
    }

    private var entryFeeSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("참가비")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Text("\(uiState.entryFee) C")
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            // 정산 비율은 SettlementPolicy가 단일 출처다. 금액 분해는 서버 권위라 비율만 안내한다 (규칙 §13)
            Text("선생님 정산 \(SettlementPolicy.shared.hostSharePercent)% · 플랫폼 수수료 \(SettlementPolicy.shared.platformFeePercent)% · 세션 시작 전 취소 시 전액 환불")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
        }
    }

    private var nicknameField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("닉네임")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            TextField("이 방에서 쓸 이름", text: Binding(
                get: { uiState.nickname },
                set: { onAction(.changeNickname(nickname: $0)) }
            ))
            .font(.system(size: 14))
            .foregroundColor(PassmateColors.textPrimary)
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(PassmateColors.fieldGray)
            .cornerRadius(14)
        }
    }

    // 시안에는 없지만 입장(join)이 캐릭터를 요구해 남긴다 — 닉네임 필드와 같은 라벨 규격을 쓴다
    private var avatarField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("내 캐릭터")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            ForEach(avatarRows, id: \.self) { rowIds in
                HStack(spacing: 8) {
                    ForEach(rowIds, id: \.self) { avatarId in
                        avatarPickItem(avatarId: avatarId)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private func avatarPickItem(avatarId: Int) -> some View {
        Button {
            onAction(.selectAvatar(avatarId: avatarId))
        } label: {
            StudentAvatarView(avatarId: avatarId)
                .frame(width: 36, height: 36)
                .padding(4)
                .overlay(
                    Circle().stroke(
                        avatarId == uiState.avatarId ? PassmateColors.primary : Color.clear,
                        lineWidth: 2
                    )
                )
        }
    }

    private var methodField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("보유 코인 \(uiState.balance) C · 부족 \(uiState.shortfall) C")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button(action: onToggleMethodExpanded) {
                HStack {
                    Text("\(uiState.selectedMethod.label) ▾")
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
            }
            if isMethodExpanded {
                VStack(spacing: 0) {
                    ForEach(paymentMethods, id: \.name) { method in
                        Button(action: { onAction(.selectMethod(method: method)) }) {
                            HStack {
                                Text(method.label)
                                    .font(.system(size: 14, weight: .medium))
                                    .kerning(-0.28)
                                    .foregroundColor(
                                        method == uiState.selectedMethod
                                            ? PassmateColors.primaryDeep
                                            : PassmateColors.textPrimary
                                    )
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                    }
                }
                .padding(.vertical, 4)
                .frame(maxWidth: .infinity)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
            }
        }
    }

    private var payButton: some View {
        let label = uiState.hasEnough
            ? "\(uiState.entryFee) C 결제하고 입장"
            : "\(uiState.shortfall) C 충전하고 입장"

        return Button(action: { onAction(.clickPay) }) {
            ZStack {
                if uiState.isProcessing {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text(label)
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(uiState.isProcessing ? PassmateColors.border : PassmateColors.primary)
            .cornerRadius(16)
        }
        .disabled(uiState.isProcessing)
    }

    private var avatarRows: [[Int]] {
        stride(from: 0, to: StudentAvatars.ids.count, by: avatarsPerRow).map { start in
            Array(StudentAvatars.ids[start..<min(start + avatarsPerRow, StudentAvatars.ids.count)])
        }
    }

    private var paymentMethods: [PaymentMethod] {
        [.kakaoPay, .naverPay, .tossPay, .card, .transfer]
    }

    private var centerProgress: some View {
        VStack {
            ProgressView().tint(PassmateColors.primary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 240)
    }

    private var retryState: some View {
        VStack(spacing: 12) {
            Text("방 정보를 불러오지 못했어요")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button(action: { onAction(.retry) }) {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 240)
    }

    // 시안 "PIN 482 913" — 6자리를 3자리씩 끊어 읽기 쉽게 표시한다
    private func formatPin(_ pin: String?) -> String {
        let resolved = pin ?? ""

        if resolved.count == 6 {
            let index = resolved.index(resolved.startIndex, offsetBy: 3)
            return "\(resolved[resolved.startIndex..<index]) \(resolved[index...])"
        } else {
            return resolved
        }
    }
}

private struct RoomPreviewView: View {
    let room: RoomInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(room.title)
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Text("₩ 유료")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.weakTopicText)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(PassmateColors.weakTopicBg)
                    .clipShape(Capsule())
            }
            if let host = room.host {
                HStack(spacing: 8) {
                    Text("\(host.nickname) 선생님")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textPrimary)
                    if let level = HostLevel.from(host.level?.intValue) {
                        ReputationBadgeView(level: level)
                    }
                }
            }
            if !metaLine.isEmpty {
                Text(metaLine)
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textSecondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.fieldGray)
        .cornerRadius(14)
    }

    // 시안 "8문항 · 약 15분 · 20:00 시작 · 현재 12명 대기" — 서버가 준 값만 이어 붙인다
    private var metaLine: String {
        var parts: [String] = []

        if let questionCount = room.questionCount {
            parts.append("\(questionCount)문항")
        }
        if let minutes = room.estimatedMinutes {
            parts.append("약 \(minutes)분")
        }
        if let scheduledAt = room.scheduledAt {
            parts.append("\(formatTime(scheduledAt)) 시작")
        }
        if let participantCount = room.participantCount {
            parts.append("현재 \(participantCount)명 대기")
        }

        return parts.joined(separator: " · ")
    }

    private func formatTime(_ isoTime: String) -> String {
        let parts = isoTime.split(separator: "T", maxSplits: 1, omittingEmptySubsequences: false)

        if parts.count == 2, parts[1].count >= 5 {
            return String(parts[1].prefix(5))
        } else {
            return isoTime
        }
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

private let previewPaidRoom = RoomInfo(
    roomId: 601,
    pin: "482913",
    title: "8월 4주차 Spring 스터디",
    topic: "이차방정식 심화",
    status: .waiting,
    questionCount: KotlinInt(int: 8),
    estimatedMinutes: KotlinInt(int: 15),
    scheduledAt: "2026-09-01T20:00:00",
    participantCount: KotlinInt(int: 12),
    maxParticipants: KotlinInt(int: 30),
    isPaid: true,
    entryFee: KotlinInt(int: 500),
    isGuestAllowed: true,
    host: RoomHost(userId: KotlinLong(value: 11), nickname: "김민지", level: KotlinInt(int: 3), avgStars: KotlinDouble(double: 4.8), ratingCount: KotlinInt(int: 32))
)

#Preview("부족분 있음") {
    PaymentContentView(
        uiState: PaymentUiState(
            isLoading: false,
            room: previewPaidRoom,
            balance: 200,
            shortfall: 300,
            nickname: "민지",
            avatarId: 3,
            selectedMethod: .kakaoPay
        ),
        onAction: { _ in },
        isMethodExpanded: false,
        onToggleMethodExpanded: {},
        onBack: {}
    )
}

#Preview("결제 진행 중") {
    PaymentContentView(
        uiState: PaymentUiState(
            isLoading: false,
            room: previewPaidRoom,
            balance: 800,
            shortfall: 0,
            nickname: "민지",
            avatarId: 3,
            isProcessing: true
        ),
        onAction: { _ in },
        isMethodExpanded: false,
        onToggleMethodExpanded: {},
        onBack: {}
    )
}

#Preview("실패 재시도") {
    PaymentContentView(
        uiState: PaymentUiState(isLoading: false, hasLoadError: true),
        onAction: { _ in },
        isMethodExpanded: false,
        onToggleMethodExpanded: {},
        onBack: {}
    )
}
