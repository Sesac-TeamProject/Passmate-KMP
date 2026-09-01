import AVFoundation
import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-01(349:9151) 미러 — PIN 6칸·QR 입장·닉네임·캐릭터 선택·입장하기
struct JoinView: View {
    var initialPin: String?

    var onJoined: (String) -> Void = { _ in }

    var onPaymentRequired: (String) -> Void = { _ in }

    var onSignInRequested: () -> Void = {}

    var onSignInRequiredForPaidRoom: (String) -> Void = { _ in }

    var onBack: () -> Void = {}

    @StateObject private var viewModel = JoinViewModel(
        getRoomInfoUseCase: KoinHelper.shared.getRoomInfoUseCase(),
        joinRoomUseCase: KoinHelper.shared.joinRoomUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase(),
        joinInputPolicy: KoinHelper.shared.joinInputPolicy()
    )

    @State private var isScannerPresented = false

    @State private var noticeMessage: String?

    var body: some View {
        JoinContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) }
        )
        .onAppear {
            if let initialPin, !initialPin.isEmpty {
                viewModel.action(.changePin(pin: initialPin))
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requestQrScan:
                isScannerPresented = true
            case let .joinCompleted(pin):
                onJoined(pin)
            case let .paymentRequired(pin):
                onPaymentRequired(pin)
            case .signInRequested:
                onSignInRequested()
            case let .signInRequiredForPaidRoom(pin):
                onSignInRequiredForPaidRoom(pin)
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(isPresented: $isScannerPresented) {
            QrScannerSheet { text in
                isScannerPresented = false
                viewModel.action(.receiveQrResult(text: text))
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                NoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct JoinContentView: View {
    let uiState: JoinUiState

    let onAction: (JoinAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                card
                if !uiState.isSignedIn {
                    signInLinkRow
                }
            }
        }
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var header: some View {
        ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 6) {
                Text("패스메이트")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.primaryDeep)
                Text("방 코드를 입력하고 시작하세요")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 40)
            .padding(.bottom, 24)
            PassyMascotView()
                .frame(width: 68, height: 75)
                .padding(.top, 20)
                .padding(.trailing, 4)
        }
        .padding(.horizontal, 24)
    }

    private var card: some View {
        PassmateCard {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("PIN으로 입장하기")
                        .font(.system(size: 20, weight: .bold))
                        .kerning(-0.4)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text("선생님 화면의 6자리 숫자를 입력하세요")
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                PinInputField(
                    pin: uiState.pin,
                    onPinChange: { onAction(.changePin(pin: $0)) }
                )
                qrScanButton
                // 입장 전 방 정보 슬롯 (T081)
                if let room = uiState.roomInfo {
                    RoomInfoCardView(room: room)
                }
                nicknameField
                avatarField
                joinButton
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 26)
        }
        .padding(.horizontal, 20)
    }

    private var qrScanButton: some View {
        Button {
            onAction(.clickScanQr)
        } label: {
            Text("QR로 입장")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.primaryDeep)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
        }
    }

    private var nicknameField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("닉네임")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            TextField(
                "이 방에서 쓸 이름",
                text: Binding(
                    get: { uiState.nickname },
                    set: { onAction(.changeNickname(nickname: $0)) }
                )
            )
            .font(.system(size: 14))
            .foregroundColor(PassmateColors.textPrimary)
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(PassmateColors.fieldGray)
            .cornerRadius(14)
        }
    }

    private var avatarField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("내 캐릭터")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            ForEach(avatarRows, id: \.first) { rowIds in
                HStack(spacing: 8) {
                    ForEach(rowIds, id: \.self) { avatarId in
                        avatarPickItem(avatarId: avatarId)
                            .frame(maxWidth: .infinity)
                    }
                    ForEach(0..<max(0, avatarsPerRow - rowIds.count), id: \.self) { _ in
                        Color.clear
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            Text("대기실·결과 화면에서 이 캐릭터로 보여요 (닉네임과 함께)")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
        }
    }

    private var avatarRows: [[Int]] {
        stride(from: 0, to: StudentAvatars.ids.count, by: avatarsPerRow).map { start in
            Array(StudentAvatars.ids[start..<min(start + avatarsPerRow, StudentAvatars.ids.count)])
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
        .frame(maxWidth: 44)
    }

    private var joinButton: some View {
        Button {
            onAction(.clickJoin)
        } label: {
            Group {
                if uiState.isJoining {
                    ProgressView()
                        .tint(PassmateColors.surface)
                } else {
                    Text("입장하기")
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(PassmateColors.primary)
            .cornerRadius(16)
        }
        .disabled(uiState.isJoining)
    }

    private var signInLinkRow: some View {
        HStack(spacing: 4) {
            Text("기록을 남기려면")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button {
                onAction(.clickSignIn)
            } label: {
                Text("로그인")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .padding(.vertical, 20)
    }
}

private let avatarsPerRow = 6

// 6칸 PIN 박스 — 숨김 TextField가 입력을 받고 박스는 상태 렌더링만 한다
private struct PinInputField: View {
    let pin: String

    let onPinChange: (String) -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        ZStack {
            GeometryReader { geometry in
                let spacing: CGFloat = 8
                let digitWidth = (geometry.size.width - (spacing * 5)) / 6

                HStack(spacing: spacing) {
                    ForEach(0..<6, id: \.self) { index in
                        PinDigitBox(
                            digit: digit(at: index),
                            isActive: index == pin.count
                        )
                        .frame(width: digitWidth, height: 56)
                    }
                }
            }
            .frame(height: 56)
            TextField(
                "",
                text: Binding(
                    get: { pin },
                    set: { onPinChange($0) }
                )
            )
            .keyboardType(.numberPad)
            .focused($isFocused)
            .opacity(0.011)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            isFocused = true
        }
    }

    private func digit(at index: Int) -> String? {
        if index < pin.count {
            return String(Array(pin)[index])
        } else {
            return nil
        }
    }
}

private struct PinDigitBox: View {
    let digit: String?

    let isActive: Bool

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(isActive ? PassmateColors.surface : PassmateColors.fieldGray)
            if isActive {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(PassmateColors.primary, lineWidth: 2)
            }
            if let digit {
                Text(digit)
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 56)
    }
}

private struct NoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
    }
}

// QR 스캔 시트 — AVFoundation 카메라 + QR 메타데이터 인식
private struct QrScannerSheet: View {
    let onResult: (String?) -> Void

    var body: some View {
        ZStack(alignment: .top) {
            QrCameraView(onCode: { onResult($0) })
                .ignoresSafeArea()
            HStack {
                Text("방 화면의 QR 코드를 비춰 주세요")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.surface)
                Spacer()
                Button("닫기") {
                    onResult(nil)
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(PassmateColors.surface)
            }
            .padding(16)
            .background(Color.black.opacity(0.5))
        }
    }
}

private struct QrCameraView: UIViewControllerRepresentable {
    let onCode: (String) -> Void

    func makeUIViewController(context: Context) -> QrCameraViewController {
        let controller = QrCameraViewController()

        controller.onCode = onCode
        return controller
    }

    func updateUIViewController(_ uiViewController: QrCameraViewController, context: Context) {}
}

final class QrCameraViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCode: ((String) -> Void)?

    private let session = AVCaptureSession()

    private var hasEmitted = false

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video) else { return }
        guard let input = try? AVCaptureDeviceInput(device: device) else { return }
        guard session.canAddInput(input) else { return }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()

        if session.canAddOutput(output) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]
        }

        let preview = AVCaptureVideoPreviewLayer(session: session)

        preview.frame = view.layer.bounds
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        let value = (metadataObjects.first as? AVMetadataMachineReadableCodeObject)?.stringValue

        if let value, !hasEmitted {
            hasEmitted = true
            onCode?(value)
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
        DispatchQueue.global(qos: .userInitiated).async { [session] in
            session.startRunning()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        session.stopRunning()
        super.viewWillDisappear(animated)
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, Koin 미초기화 상태에서도 안전한 콘텐츠 뷰 기반)
// JoinUiState에는 인라인 에러/알림 필드가 없다 — 알림은 이벤트로만 전달되고
// 표시(오버레이) 소유는 컨테이너(JoinView)다 (규칙 §11-1) — 그래서 3번째 상태는 생략한다.

#Preview("빈 입력 상태") {
    JoinContentView(
        uiState: JoinUiState(),
        onAction: { _ in }
    )
}

#Preview("PIN 입력 완료 + 닉네임 + 캐릭터 선택") {
    JoinContentView(
        uiState: JoinUiState(
            pin: "482913",
            nickname: "민지",
            avatarId: 3,
            isSignedIn: true,
            roomInfo: RoomInfo(
                roomId: 801,
                pin: "482913",
                title: "8월 4주차 Spring 스터디",
                topic: "이차함수 심화",
                status: .waiting,
                questionCount: KotlinInt(int: 8),
                estimatedMinutes: KotlinInt(int: 20),
                scheduledAt: nil,
                participantCount: KotlinInt(int: 12),
                maxParticipants: KotlinInt(int: 30),
                isPaid: false,
                entryFee: nil,
                host: RoomHost(userId: KotlinLong(value: 11), nickname: "김선생", level: KotlinInt(int: 3), avgStars: KotlinDouble(double: 4.8), ratingCount: KotlinInt(int: 32))
            )
        ),
        onAction: { _ in }
    )
}
