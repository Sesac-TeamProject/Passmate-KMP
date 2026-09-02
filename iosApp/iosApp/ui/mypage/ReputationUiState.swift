import Shared

struct ReputationUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    // 시안 상단 프로필 카드(닉네임·캐릭터)용 — 등급 집계는 grade.stats가 담당한다
    var profile: UserProfile?

    var grade: MyGrade?

    var badges: [Badge] = []
}
