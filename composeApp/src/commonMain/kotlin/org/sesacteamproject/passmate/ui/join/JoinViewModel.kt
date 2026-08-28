package org.sesacteamproject.passmate.ui.join

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase

class JoinViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val isSignedInUseCase: IsSignedInUseCase,
    private val joinInputPolicy: JoinInputPolicy
) : MviViewModel<JoinUiState, JoinAction, JoinEvent>(JoinUiState()) {

    private var roomInfoJob: Job? = null

    private fun onChangePin(pin: String) {
        val digits = pin.filter { it.isDigit() }.take(JoinInputPolicy.PIN_LENGTH)

        _uiState.update { it.copy(pin = digits) }
        // PIN이 완성되면 방 정보(호스트 등급·별점)를 미리 불러온다, 바뀌면 초기화 (T081)
        if (joinInputPolicy.isValidPin(digits)) {
            prefetchRoomInfo(digits)
        } else {
            roomInfoJob?.cancel()
            _uiState.update { it.copy(roomInfo = null, isLoadingRoomInfo = false) }
        }
    }

    private fun prefetchRoomInfo(pin: String) {
        if (_uiState.value.roomInfo?.pin == pin) {
            return
        }
        roomInfoJob?.cancel()
        _uiState.update { it.copy(isLoadingRoomInfo = true) }
        roomInfoJob = viewModelScope.launch {
            getRoomInfoUseCase.invoke(pin)
                .onSuccess { room -> _uiState.update { it.copy(roomInfo = room, isLoadingRoomInfo = false) } }
                .onFailure { _uiState.update { it.copy(roomInfo = null, isLoadingRoomInfo = false) } }
        }
    }

    private fun onChangeNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname.take(JoinInputPolicy.NICKNAME_MAX_LENGTH)) }
    }

    private fun onSelectAvatar(avatarId: Int) {
        _uiState.update { it.copy(avatarId = avatarId) }
    }

    private fun onClickScanQr() {
        viewModelScope.launch {
            _event.emit(JoinEvent.RequestQrScan)
        }
    }

    private fun onReceiveQrResult(text: String?) {
        if (text == null) {
            return
        }
        val pin = joinInputPolicy.extractPin(text)

        if (pin != null) {
            _uiState.update { it.copy(pin = pin) }
        } else {
            viewModelScope.launch {
                _event.emit(JoinEvent.ShowNotice("QR 코드에서 PIN을 찾지 못했어요"))
            }
        }
    }

    private fun onClickSignIn() {
        viewModelScope.launch {
            _event.emit(JoinEvent.SignInRequested)
        }
    }

    private fun onClickJoin() {
        val state = _uiState.value

        if (state.isJoining) {
            return
        }
        viewModelScope.launch {
            if (!joinInputPolicy.isValidPin(state.pin)) {
                _event.emit(JoinEvent.ShowNotice("PIN 6자리를 입력해 주세요"))
            } else if (!joinInputPolicy.isValidNickname(state.nickname)) {
                _event.emit(JoinEvent.ShowNotice("이 방에서 쓸 닉네임을 입력해 주세요"))
            } else {
                _uiState.update { it.copy(isJoining = true) }
                loadRoomAndJoin(state.pin, state.nickname, state.avatarId)
            }
        }
    }

    private suspend fun loadRoomAndJoin(pin: String, nickname: String, avatarId: Int) {
        getRoomInfoUseCase.invoke(pin)
            .onSuccess { room -> joinIfAllowed(room, nickname, avatarId) }
            .onFailure { error ->
                _uiState.update { it.copy(isJoining = false) }
                _event.emit(JoinEvent.ShowNotice(roomErrorMessage(error)))
            }
    }

    // 클라이언트 가드는 UX 목적 — 최종 판정은 서버 4xx를 그대로 처리한다 (규칙 §8)
    private suspend fun joinIfAllowed(room: RoomInfo, nickname: String, avatarId: Int) {
        if (room.status == RoomStatus.FINISHED) {
            _uiState.update { it.copy(isJoining = false) }
            _event.emit(JoinEvent.ShowNotice("이미 종료된 방이에요"))
        } else if (room.isPaid && !_uiState.value.isSignedIn) {
            _uiState.update { it.copy(isJoining = false) }
            _event.emit(JoinEvent.ShowNotice("유료 방은 로그인 후 입장할 수 있어요"))
            _event.emit(JoinEvent.SignInRequested)
        } else if (room.isPaid) {
            // 회원의 유료 방 입장은 참가비 결제 화면으로 위임한다 (US14)
            _uiState.update { it.copy(isJoining = false) }
            _event.emit(JoinEvent.PaymentRequired(room.pin))
        } else {
            joinRoomUseCase.invoke(room, nickname, avatarId)
                .onSuccess {
                    _uiState.update { it.copy(isJoining = false) }
                    _event.emit(JoinEvent.JoinCompleted(room.pin))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isJoining = false) }
                    handleJoinFailure(error)
                }
        }
    }

    private suspend fun handleJoinFailure(error: AppError) {
        when (error) {
            is AppError.Conflict -> _event.emit(JoinEvent.ShowNotice("이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"))
            is AppError.LoginRequired -> {
                _event.emit(JoinEvent.ShowNotice("유료 방은 로그인 후 입장할 수 있어요"))
                _event.emit(JoinEvent.SignInRequested)
            }
            is AppError.PaymentRequired -> _event.emit(JoinEvent.PaymentRequired(_uiState.value.pin))
            else -> _event.emit(JoinEvent.ShowNotice(roomErrorMessage(error)))
        }
    }

    private fun roomErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NotFound -> "방을 찾을 수 없어요. PIN을 확인해 주세요"
            is AppError.Gone -> "이미 종료된 방이에요"
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> "입장하지 못했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    override fun onAction(action: JoinAction) {
        when (action) {
            is JoinAction.ChangePin -> onChangePin(action.pin)
            is JoinAction.ChangeNickname -> onChangeNickname(action.nickname)
            is JoinAction.SelectAvatar -> onSelectAvatar(action.avatarId)
            is JoinAction.ClickScanQr -> onClickScanQr()
            is JoinAction.ReceiveQrResult -> onReceiveQrResult(action.text)
            is JoinAction.ClickJoin -> onClickJoin()
            is JoinAction.ClickSignIn -> onClickSignIn()
        }
    }

    init {
        _uiState.update { it.copy(isSignedIn = isSignedInUseCase.invoke()) }
    }
}
