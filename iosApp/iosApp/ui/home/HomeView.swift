import SwiftUI

// 임시 셸 화면 — 본 구현은 T098(공개 방 목록)에서 M-01 v6 디자인으로 대체된다 (Compose HomeScreen.kt 미러)
struct HomeView: View {
    var onJoinTapped: () -> Void = {}

    var onSignInTapped: () -> Void = {}

    var onMyInfoTapped: () -> Void = {}

    var onRoomListTapped: () -> Void = {}

    var body: some View {
        VStack(spacing: 0) {
            PassyMascotView()
                .frame(width: 80, height: 88)
            Text("패스메이트")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .padding(.top, 12)
            Text("홈 화면은 준비 중이에요")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 4)
            Button(action: onJoinTapped) {
                Text("PIN으로 입장하기")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(PassmateColors.primary)
                    .cornerRadius(14)
            }
            .padding(.top, 24)
            Button(action: onRoomListTapped) {
                Text("방 찾기")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .padding(4)
            }
            .padding(.top, 12)
            Button(action: onMyInfoTapped) {
                Text("내 학습 기록")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .padding(4)
            }
            .padding(.top, 4)
            Button(action: onSignInTapped) {
                Text("로그인")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(PassmateColors.primaryDeep)
                    .padding(4)
            }
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.backgroundMint.ignoresSafeArea())
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
