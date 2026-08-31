import SwiftUI

// T075(US9) 게스트 가입 유도 — "가입하고 이 기록 저장하기" → 로그인 → claim (M-05·M-06 하단).
// 게스트 기록은 7일 내 연동 가능 (FR-036). 회원에게는 표시하지 않는다
struct SignupPromptSectionView: View {
    let onClickSignup: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("이 기록, 계정에 저장할까요?")
                .font(.system(size: 14, weight: .bold))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            Text("가입하면 지금 푼 결과와 리포트가 계정에 남아요 (7일 내 저장 가능)")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
            Button(action: onClickSignup) {
                Text("가입하고 이 기록 저장하기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.backgroundMint)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(16)
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("게스트 가입 유도") {
    SignupPromptSectionView(onClickSignup: {})
        .padding()
}
