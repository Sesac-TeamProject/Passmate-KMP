struct NotificationSettingsUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var sessionStart: Bool = true

    var ratingRequest: Bool = true

    var settlementDone: Bool = true

    // 저장 in-flight — 토글 즉시 저장 방식이라 저장 중 추가 토글을 막는다 (규칙 §9)
    var isSaving: Bool = false
}
