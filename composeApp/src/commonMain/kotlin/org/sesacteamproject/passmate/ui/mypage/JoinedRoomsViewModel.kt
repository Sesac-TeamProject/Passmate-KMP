package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase

class JoinedRoomsViewModel(
    private val getMyPageUseCase: GetMyPageUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<JoinedRoomsUiState, JoinedRoomsAction, JoinedRoomsEvent>(JoinedRoomsUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도한다 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(JoinedRoomsEvent.RequireSignIn)
            }
        } else {
            loadFirstPage()
        }
    }

    private fun loadFirstPage() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getMyPageUseCase.invoke(null)
                .onSuccess { myPage ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = false,
                            summary = myPage.summary,
                            ongoing = myPage.ongoing,
                            rooms = myPage.rooms,
                            nextCursor = myPage.nextCursor
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun onLoadMore() {
        val state = _uiState.value
        val cursor = state.nextCursor

        if (cursor == null || state.isLoadingMore) {
            return
        }
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            getMyPageUseCase.invoke(cursor)
                .onSuccess { myPage ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            rooms = it.rooms + myPage.rooms,
                            nextCursor = myPage.nextCursor
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _event.emit(JoinedRoomsEvent.ShowNotice("목록을 더 불러오지 못했어요"))
                }
        }
    }

    private fun onClickRoomReport(roomId: Long) {
        viewModelScope.launch {
            _event.emit(JoinedRoomsEvent.OpenReport(roomId))
        }
    }

    private fun onClickRejoin(pin: String) {
        viewModelScope.launch {
            _event.emit(JoinedRoomsEvent.Rejoin(pin))
        }
    }

    private fun onClickContactSupport() {
        viewModelScope.launch {
            _event.emit(JoinedRoomsEvent.ShowNotice(CONTACT_SUPPORT_NOTICE))
        }
    }

    override fun onAction(action: JoinedRoomsAction) {
        when (action) {
            is JoinedRoomsAction.Enter -> onEnter()
            is JoinedRoomsAction.Retry -> loadFirstPage()
            is JoinedRoomsAction.LoadMore -> onLoadMore()
            is JoinedRoomsAction.ClickRoomReport -> onClickRoomReport(action.roomId)
            is JoinedRoomsAction.ClickRejoin -> onClickRejoin(action.pin)
            is JoinedRoomsAction.ClickContactSupport -> onClickContactSupport()
        }
    }
}

// 문의 창구(고객센터·메일)는 아직 계약·라우트에 없다 — 연결 전까지 안내 문구만 노출한다
internal const val CONTACT_SUPPORT_NOTICE = "문의 창구는 준비 중이에요"
