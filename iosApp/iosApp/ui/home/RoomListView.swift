import SwiftUI
import Shared

// 프로필 시트 표시용 Identifiable 래퍼 — 시트 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private struct ProfileHost: Identifiable {
    let id: Int64
}

// 공개 방 목록·탐색 (M-11) — Compose RoomListScreen.kt 미러.
// 선생님 이름 탭 시 프로필 시트(M-10)를 연다
struct RoomListView: View {
    @StateObject private var viewModel = RoomListViewModel(
        getPublicRoomsUseCase: KoinHelper.shared.getPublicRoomsUseCase()
    )

    var onOpenRoom: (String) -> Void = { _ in }

    var onOpenPinEntry: () -> Void = {}

    var onRequireSignIn: () -> Void = {}

    @State private var profileHost: ProfileHost?

    @State private var noticeMessage: String?

    var body: some View {
        RoomListContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.action
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case let .openRoom(pin):
                onOpenRoom(pin)
            case .openPinEntry:
                onOpenPinEntry()
            case let .openHostProfile(hostId):
                profileHost = ProfileHost(id: hostId)
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(item: $profileHost) { host in
            HostProfileSheetView(
                hostId: host.id,
                onJoinRoom: { pin in
                    profileHost = nil
                    onOpenRoom(pin)
                },
                onRequireSignIn: {
                    profileHost = nil
                    onRequireSignIn()
                },
                onBlocked: {
                    profileHost = nil
                    // 차단 호스트의 방은 공개 목록에서 숨겨진다 — 목록 새로고침 (M-10)
                    viewModel.action(.retry)
                },
                onNotice: { message in
                    viewModel.action(.notice(message: message))
                }
            )
            .presentationDetents([.medium, .large])
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                RoomListNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct RoomListNoticeToast: View {
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

private struct RoomListContentView: View {
    let uiState: RoomListUiState

    let onAction: (RoomListAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("방 찾기")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Button(action: { onAction(.clickPinEntry) }) {
                    Text("PIN 입장")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(PassmateColors.surface)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(PassmateColors.primary)
                        .cornerRadius(12)
                }
            }
            .padding(.top, 20)
            searchField
                .padding(.top, 14)
            typeFilters
                .padding(.top, 12)
            content
                .padding(.top, 12)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.backgroundMint.ignoresSafeArea())
    }

    private var searchField: some View {
        TextField("방 이름·선생님·주제 검색", text: Binding(
            get: { uiState.query },
            set: { onAction(.changeQuery(query: $0)) }
        ))
        .font(.system(size: 14))
        .padding(12)
        .background(PassmateColors.surface)
        .cornerRadius(14)
        .submitLabel(.search)
        .onSubmit { onAction(.submitSearch) }
    }

    private var typeFilters: some View {
        HStack(spacing: 8) {
            filterChip(.all, "전체")
            filterChip(.free, "무료")
            filterChip(.paid, "유료")
        }
    }

    private func filterChip(_ type: RoomTypeFilter, _ label: String) -> some View {
        let isSelected = uiState.typeFilter == type

        return Button(action: { onAction(.selectType(type: type)) }) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(isSelected ? PassmateColors.surface : PassmateColors.textSecondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? PassmateColors.primary : PassmateColors.surface)
                .clipShape(Capsule())
        }
    }

    @ViewBuilder
    private var content: some View {
        if uiState.isLoading {
            centerProgress
        } else if uiState.hasError {
            retryState
        } else if uiState.isEmpty {
            centerText("조건에 맞는 방이 없어요")
        } else {
            roomList
        }
    }

    private var roomList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(uiState.rooms, id: \.roomId) { room in
                    RoomCardView(
                        room: room,
                        onClickHost: { hostId in onAction(.clickHost(hostId: hostId)) }
                    )
                    .onTapGesture { onAction(.clickRoom(pin: room.pin)) }
                }
                if uiState.hasNext {
                    ProgressView()
                        .padding(12)
                        .onAppear { onAction(.loadMore) }
                }
                Spacer().frame(height: 16)
            }
            .padding(.top, 4)
        }
    }

    private var centerProgress: some View {
        VStack {
            Spacer()
            ProgressView().tint(PassmateColors.primary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private func centerText(_ text: String) -> some View {
        VStack {
            Spacer()
            Text(text).font(.system(size: 14)).foregroundColor(PassmateColors.textTertiary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private var retryState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("목록을 불러오지 못했어요")
                .font(.system(size: 14))
                .foregroundColor(PassmateColors.textSecondary)
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

private struct RoomCardView: View {
    let room: PublicRoom

    var onClickHost: (Int64) -> Void = { _ in }

    var body: some View {
        PassmateCard {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    Text(room.title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    feeBadge
                }
                if let topic = room.topic {
                    Text(topic)
                        .font(.system(size: 13))
                        .foregroundColor(PassmateColors.textSecondary)
                        .padding(.top, 4)
                }
                // 선생님 이름을 누르면 레벨·별점·뱃지 프로필 시트 (M-10, M-11 노트)
                HStack(spacing: 8) {
                    Text(room.hostName)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(PassmateColors.textSecondary)
                    if let level = HostLevel.from(room.hostLevel.map { Int(truncating: $0) }) {
                        ReputationBadgeView(level: level)
                    }
                    if let rating = room.hostRating?.doubleValue {
                        Text("★ \(formatRating(rating))")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(PassmateColors.starGold)
                    }
                }
                .padding(.top, 10)
                .onTapGesture {
                    if let hostId = room.hostId?.int64Value {
                        onClickHost(hostId)
                    }
                }
                Text(participantsText)
                    .font(.system(size: 12))
                    .foregroundColor(PassmateColors.textTertiary)
                    .padding(.top, 8)
            }
            .padding(16)
        }
    }

    private var feeBadge: some View {
        let label = room.isPaid ? "유료 \(room.entryFee?.intValue ?? 0) C" : "무료"
        let bg = room.isPaid ? PassmateColors.chipGold : PassmateColors.chipGreen
        let fg = room.isPaid ? PassmateColors.chipGoldText : PassmateColors.chipGreenText

        return Text(label)
            .font(.system(size: 12, weight: .medium))
            .foregroundColor(fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(bg)
            .clipShape(Capsule())
    }

    private var participantsText: String {
        let current = room.participantCount?.intValue ?? 0

        if let max = room.maxParticipants?.intValue {
            return "참여 \(current) / \(max) 명"
        } else {
            return "참여 \(current) 명"
        }
    }

    private func formatRating(_ value: Double) -> String {
        String(format: "%.1f", value)
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("방 3개 (유료 · Lv.3 포함)") {
    RoomListContentView(
        uiState: RoomListUiState(
            isLoading: false,
            rooms: [
                PublicRoom(roomId: 701, pin: "482913", title: "8월 4주차 Spring 스터디", topic: "이차함수 심화", hostId: KotlinLong(value: 11), hostName: "김선생", hostLevel: KotlinInt(int: 3), hostRating: KotlinDouble(double: 4.8), status: .waiting, participantCount: KotlinInt(int: 12), maxParticipants: KotlinInt(int: 30), isPaid: true, entryFee: KotlinInt(int: 500), scheduledAt: nil),
                PublicRoom(roomId: 702, pin: "115820", title: "확률과 통계 총정리", topic: "조건부확률", hostId: KotlinLong(value: 11), hostName: "이선생", hostLevel: KotlinInt(int: 2), hostRating: KotlinDouble(double: 4.5), status: .waiting, participantCount: KotlinInt(int: 8), maxParticipants: nil, isPaid: false, entryFee: nil, scheduledAt: nil),
                PublicRoom(roomId: 703, pin: "930447", title: "함수의 극한 무료 특강", topic: nil, hostId: KotlinLong(value: 11), hostName: "박선생", hostLevel: nil, hostRating: nil, status: .running, participantCount: KotlinInt(int: 20), maxParticipants: KotlinInt(int: 20), isPaid: false, entryFee: nil, scheduledAt: nil)
            ]
        ),
        onAction: { _ in }
    )
}

#Preview("검색 결과 없음") {
    RoomListContentView(
        uiState: RoomListUiState(isLoading: false, rooms: []),
        onAction: { _ in }
    )
}
