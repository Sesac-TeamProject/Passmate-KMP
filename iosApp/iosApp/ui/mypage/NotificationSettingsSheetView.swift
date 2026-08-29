import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-12-10 미러 — 알림 설정 3종, 토글 즉시 저장.
// 시트 표시 여부는 호스팅 화면(SettingsView)이 소유한다 (규칙 §11-1)
struct NotificationSettingsSheetView: View {
    var onNotice: (String) -> Void = { _ in }

    var onClose: () -> Void = {}

    @StateObject private var viewModel = NotificationSettingsViewModel(
        getNotificationSettingsUseCase: KoinHelper.shared.getNotificationSettingsUseCase(),
        updateNotificationSettingsUseCase: KoinHelper.shared.updateNotificationSettingsUseCase()
    )

    var body: some View {
        NotificationSettingsContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClose: onClose
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .showNotice(message):
                onNotice(message)
            }
        }
    }
}

private struct NotificationSettingsContentView: View {
    let uiState: NotificationSettingsUiState

    let onAction: (NotificationSettingsAction) -> Void

    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("알림 설정")
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Button(action: onClose) {
                    Text("✕")
                        .font(.system(size: 18))
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            if uiState.isLoading {
                HStack {
                    Spacer()
                    ProgressView().tint(PassmateColors.primary)
                    Spacer()
                }
                .frame(height: 160)
            } else if uiState.loadFailed {
                Button(action: { onAction(.retry) }) {
                    Text("설정을 불러오지 못했어요 · 다시 시도")
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.weakTopicText)
                        .padding(.vertical, 24)
                }
            } else {
                toggleRow(
                    title: "세션 시작",
                    subtitle: "참여한 방의 세션이 시작되면 알려드려요",
                    isOn: uiState.sessionStart,
                    kind: .sessionStart
                )
                toggleRow(
                    title: "별점 요청",
                    subtitle: "세션 종료 후 평가 요청을 알려드려요",
                    isOn: uiState.ratingRequest,
                    kind: .ratingRequest
                )
                toggleRow(
                    title: "정산 완료",
                    subtitle: "유료 방 정산이 지급되면 알려드려요",
                    isOn: uiState.settlementDone,
                    kind: .settlementDone
                )
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .padding(.bottom, 28)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private func toggleRow(title: String, subtitle: String, isOn: Bool, kind: NotificationKind) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .medium))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textTertiary)
            }
            Spacer()
            Toggle("", isOn: Binding(
                get: { isOn },
                set: { _ in onAction(.toggle(kind: kind)) }
            ))
            .labelsHidden()
            .tint(PassmateColors.primary)
            .disabled(uiState.isSaving)
        }
        .padding(.vertical, 8)
    }
}
