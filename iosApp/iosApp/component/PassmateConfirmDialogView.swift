import SwiftUI

// Compose PassmateConfirmDialog.kt 미러 — 시안 v6 확인 다이얼로그 (M-12-11 로그아웃 확인 등).
// iOS 기본 .alert 대신 쓴다. 오버레이라 iOS 15에서도 그대로 동작한다.
// 표시 여부와 생명주기는 호출한 화면이 소유한다 (규칙 §11-1)
struct PassmateConfirmDialogView: View {
    let title: String

    let message: String

    let confirmLabel: String

    var dismissLabel: String = "취소"

    let onConfirm: () -> Void

    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }
            VStack(spacing: 12) {
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.textPrimary)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(.system(size: 15))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textSecondary)
                    .multilineTextAlignment(.center)
                HStack(spacing: 12) {
                    dialogButton(label: dismissLabel, isPrimary: false, action: onDismiss)
                    dialogButton(label: confirmLabel, isPrimary: true, action: onConfirm)
                }
                .padding(.top, 12)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 28)
            .background(PassmateColors.surface)
            .cornerRadius(24)
            .padding(.horizontal, 32)
        }
    }

    private func dialogButton(label: String, isPrimary: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 15, weight: .bold))
                .kerning(-0.3)
                .foregroundColor(isPrimary ? PassmateColors.surface : PassmateColors.textPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(isPrimary ? PassmateColors.primary : PassmateColors.surface)
                .cornerRadius(16)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(isPrimary ? Color.clear : PassmateColors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }
}
