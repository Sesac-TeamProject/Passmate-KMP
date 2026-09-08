import SwiftUI

// Compose PassmateDisconnectedOverlay.kt 미러 — Figma "UI 디자인 v6" M-07(349:9529) 연결 끊김·재접속.
// 흰 바탕 전체 화면 위에 카드 하나(r28·테두리·패딩 36/28/32/28·간격 16). 자동 재연결은 shared가 계속 시도하고,
// 버튼은 백오프를 건너뛰는 즉시 재구독이다. 표시 여부와 생명주기는 호출한 컨테이너 View가 소유한다 (규칙 §11-1)
struct PassmateDisconnectedOverlayView: View {
    let onReconnect: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(PassmateColors.fieldGray)
                    .frame(width: 80, height: 80)
                Text("!")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            Text("연결이 끊겼어요")
                .font(.system(size: 24, weight: .bold))
                .kerning(-0.48)
                .foregroundColor(PassmateColors.textPrimary)
            Text("네트워크를 확인하고 있어요.\n다시 연결되면 진행 중인 문항으로 돌아가요.")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .multilineTextAlignment(.center)
            PassmateWaitingDots()
            Button(action: onReconnect) {
                Text("지금 다시 연결")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            .buttonStyle(.plain)
        }
        .padding(.top, 36)
        .padding(.horizontal, 28)
        .padding(.bottom, 32)
        .background(PassmateColors.surface)
        .cornerRadius(28)
        .overlay(
            RoundedRectangle(cornerRadius: 28)
                .stroke(PassmateColors.border, lineWidth: 1)
        )
        .padding(.horizontal, 20)
        .padding(.top, 64)
        .padding(.bottom, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // 뒤에 깔린 화면으로 터치가 새지 않게 전체를 히트 영역으로 잡는다
        .contentShape(Rectangle())
        .background(PassmateColors.surface.ignoresSafeArea())
    }
}
