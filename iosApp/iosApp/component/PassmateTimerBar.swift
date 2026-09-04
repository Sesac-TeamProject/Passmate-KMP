import SwiftUI

// Compose PassmateTimerBar.kt 미러 — 시안 v6 M-03 풀이 · M-T2 진행 리모컨의 남은 시간 표시.
// 시계 아이콘 + mm : ss + "남은 시간" 한 줄, 그 아래 진행 바.
// 남은 시간은 서버 endsAt 기준 값을 렌더링만 한다 — 여기서 판정하지 않는다 (규칙 §5)
struct PassmateTimerBar: View {
    let remainingSeconds: Int

    let totalSeconds: Int

    private var safeRemaining: Int {
        max(remainingSeconds, 0)
    }

    private var progress: Double {
        if totalSeconds > 0 {
            return min(max(Double(safeRemaining) / Double(totalSeconds), 0), 1)
        } else {
            return 0
        }
    }

    // mm : ss — 시안 표기(공백 있는 콜론)를 그대로 따른다
    private var label: String {
        let minutes = safeRemaining / 60
        let rest = safeRemaining % 60
        return String(format: "%02d : %02d", minutes, rest)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 0) {
                PassmateIconView(icon: .clock, tint: PassmateColors.timerAmber, size: 22)
                Text(label)
                    .font(.system(size: 26, weight: .bold))
                    .kerning(-0.5)
                    .foregroundColor(PassmateColors.textPrimary)
                    .padding(.leading, 10)
                Spacer()
                Text("남은 시간")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(PassmateColors.timerTrack)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(PassmateColors.timerAmber)
                        .frame(width: geometry.size.width * progress)
                }
            }
            .frame(height: 8)
        }
    }
}
