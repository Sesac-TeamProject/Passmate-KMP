import SwiftUI
import Shared

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

    var body: some View {
        ZStack {
            PaymentContentView(uiState: viewModel.uiState, onAction: viewModel.action, onBack: onBack)
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

    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onBack) {
                Text("‹ 뒤로").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.top, 16)
            Text("유료 방 입장")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 8)
            if uiState.isLoading {
                centerProgress
            } else if uiState.hasLoadError {
                retryState
            } else {
                loaded
            }
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.backgroundMint.ignoresSafeArea())
    }

    private var loaded: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                roomCard
                coinCard
                nicknameField
                avatarPicker
                if !uiState.hasEnough {
                    methodPicker
                }
                if let error = uiState.errorMessage {
                    Text(error).font(.system(size: 13)).foregroundColor(PassmateColors.wrongPinkText)
                }
                payButton.padding(.top, 8)
                Spacer().frame(height: 24)
            }
            .padding(.top, 16)
        }
    }

    private var roomCard: some View {
        PassmateCard {
            VStack(alignment: .leading, spacing: 0) {
                Text(uiState.room?.title ?? "")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(PassmateColors.textPrimary)
                if let topic = uiState.room?.topic {
                    Text(topic).font(.system(size: 13)).foregroundColor(PassmateColors.textSecondary).padding(.top, 4)
                }
                HStack {
                    Text("참가비").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
                    Spacer()
                    Text("\(uiState.entryFee) C").font(.system(size: 16, weight: .bold)).foregroundColor(PassmateColors.textPrimary)
                }
                .padding(.top, 10)
            }
            .padding(16)
        }
    }

    private var coinCard: some View {
        PassmateCard {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("보유 코인").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
                    Spacer()
                    Text("\(uiState.balance) C").font(.system(size: 16, weight: .bold)).foregroundColor(PassmateColors.textPrimary)
                }
                if !uiState.hasEnough {
                    HStack {
                        Text("부족 코인").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
                        Spacer()
                        Text("\(uiState.shortfall) C").font(.system(size: 16, weight: .bold)).foregroundColor(PassmateColors.wrongPinkText)
                    }
                }
            }
            .padding(16)
        }
    }

    private var nicknameField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("이 방에서 쓸 닉네임").font(.system(size: 13)).foregroundColor(PassmateColors.textSecondary)
            TextField("닉네임", text: Binding(
                get: { uiState.nickname },
                set: { onAction(.changeNickname(nickname: $0)) }
            ))
            .font(.system(size: 14))
            .padding(12)
            .background(PassmateColors.surface)
            .cornerRadius(14)
        }
    }

    private var avatarPicker: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("캐릭터").font(.system(size: 13)).foregroundColor(PassmateColors.textSecondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(1...12, id: \.self) { id in
                        StudentAvatarView(avatarId: id)
                            .frame(width: 36, height: 36)
                            .padding(6)
                            .background(PassmateColors.surface)
                            .clipShape(Circle())
                            .overlay(
                                Circle().stroke(
                                    id == uiState.avatarId ? PassmateColors.primary : PassmateColors.border,
                                    lineWidth: 2
                                )
                            )
                            .onTapGesture { onAction(.selectAvatar(avatarId: id)) }
                    }
                }
            }
        }
    }

    private var methodPicker: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("충전 결제 수단").font(.system(size: 13)).foregroundColor(PassmateColors.textSecondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(paymentMethods, id: \.name) { method in
                        let isSelected = method == uiState.selectedMethod

                        Text(method.label)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(isSelected ? PassmateColors.surface : PassmateColors.textSecondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(isSelected ? PassmateColors.primary : PassmateColors.surface)
                            .clipShape(Capsule())
                            .onTapGesture { onAction(.selectMethod(method: method)) }
                    }
                }
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
                    Text(label).font(.system(size: 15, weight: .bold)).foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(uiState.isProcessing ? PassmateColors.border : PassmateColors.primary)
            .cornerRadius(16)
        }
        .disabled(uiState.isProcessing)
    }

    private var paymentMethods: [PaymentMethod] {
        [.kakaoPay, .naverPay, .tossPay, .card, .transfer]
    }

    private var centerProgress: some View {
        VStack {
            Spacer()
            ProgressView().tint(PassmateColors.primary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private var retryState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("방 정보를 불러오지 못했어요").font(.system(size: 14)).foregroundColor(PassmateColors.textSecondary)
            Button(action: { onAction(.retry) }) {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("부족분 있음") {
    PaymentContentView(
        uiState: PaymentUiState(
            isLoading: false,
            room: RoomInfo(
                roomId: 601,
                pin: "731204",
                title: "8월 4주차 Spring 스터디",
                topic: "이차방정식 심화",
                status: .waiting,
                questionCount: KotlinInt(int: 8),
                estimatedMinutes: KotlinInt(int: 20),
                scheduledAt: nil,
                participantCount: KotlinInt(int: 12),
                maxParticipants: KotlinInt(int: 30),
                isPaid: true,
                entryFee: KotlinInt(int: 500),
                host: RoomHost(userId: KotlinLong(value: 11), nickname: "김선생", level: KotlinInt(int: 3), avgStars: KotlinDouble(double: 4.8), ratingCount: KotlinInt(int: 32))
            ),
            balance: 200,
            shortfall: 300,
            nickname: "민지",
            avatarId: 3,
            selectedMethod: .kakaoPay
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("결제 진행 중") {
    PaymentContentView(
        uiState: PaymentUiState(
            isLoading: false,
            room: RoomInfo(
                roomId: 601,
                pin: "731204",
                title: "8월 4주차 Spring 스터디",
                topic: "이차방정식 심화",
                status: .waiting,
                questionCount: KotlinInt(int: 8),
                estimatedMinutes: KotlinInt(int: 20),
                scheduledAt: nil,
                participantCount: KotlinInt(int: 12),
                maxParticipants: KotlinInt(int: 30),
                isPaid: true,
                entryFee: KotlinInt(int: 500),
                host: RoomHost(userId: KotlinLong(value: 11), nickname: "김선생", level: KotlinInt(int: 3), avgStars: KotlinDouble(double: 4.8), ratingCount: KotlinInt(int: 32))
            ),
            balance: 800,
            shortfall: 0,
            nickname: "민지",
            avatarId: 3,
            isProcessing: true
        ),
        onAction: { _ in },
        onBack: {}
    )
}

#Preview("실패 재시도") {
    PaymentContentView(
        uiState: PaymentUiState(isLoading: false, hasLoadError: true),
        onAction: { _ in },
        onBack: {}
    )
}
