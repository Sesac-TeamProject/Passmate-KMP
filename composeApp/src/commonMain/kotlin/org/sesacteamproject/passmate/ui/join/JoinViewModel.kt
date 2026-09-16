package org.sesacteamproject.passmate.ui.join

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.ServerErrorCode
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.RejoinRoomUseCase

class JoinViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val rejoinRoomUseCase: RejoinRoomUseCase,
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
            _event.emit(JoinEvent.SignInRequiredForPaidRoom(room.pin))
        } else if (room.isPaid) {
            // 회원의 유료 방 입장은 참가비 결제 화면으로 위임한다 (US14)
            _uiState.update { it.copy(isJoining = false) }
            _event.emit(JoinEvent.PaymentRequired(room.pin))
        } else {
            joinRoomUseCase.invoke(room, nickname, avatarId)
                .onSuccess { participation ->
                    // 입장에 성공하면 폼의 PIN을 비운다 — 대기실에서 나와 홈으로 돌아왔을 때
                    // 지난 방의 PIN이 남아 있으면 안 된다. 닉네임·캐릭터는 다음 입장에도 쓰므로 남긴다
                    _uiState.update { it.copy(isJoining = false, pin = "", roomInfo = null) }
                    // 새 입장이 아니라 원래 자리로 돌아간 경우 — 입력한 이름이 아닌 처음 이름으로
                    // 들어가므로 말없이 바뀌면 혼란스럽다
                    if (participation.isRejoined) {
                        _event.emit(JoinEvent.ShowNotice(RESUMED_NOTICE))
                    }
                    _event.emit(JoinEvent.JoinCompleted(room.pin))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isJoining = false) }
                    handleJoinFailure(room, error)
                }
        }
    }

    // 403은 code로 원인을 가른다 — 새 입장과 재입장이 같은 문구를 써야 강퇴 안내가 경로마다 달라지지 않는다.
    // 입장 흐름에서 ACCESS_DENIED는 강퇴당한 방의 재입장 거부다 (규칙 §10)
    private fun forbiddenMessage(code: String?): String {
        return if (code == ServerErrorCode.HOST_CANNOT_JOIN) {
            "내가 만든 방에는 참가자로 입장할 수 없어요"
        } else if (code == ServerErrorCode.ACCESS_DENIED) {
            "내보내진 방에는 다시 들어올 수 없어요"
        } else {
            "이 방에 입장할 권한이 없어요"
        }
    }

    // 재입장 실패는 원인별로 갈라야 한다 — 강퇴는 다시 눌러도 안 되고, 끝난 방은 결과로 가야 한다 (규칙 §10)
    private fun rejoinErrorMessage(error: AppError, fallbackMessage: String): String {
        return when (error) {
            is AppError.PermissionDenied -> forbiddenMessage(error.serverCode)
            is AppError.Gone -> "이미 종료된 방이에요"
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> fallbackMessage
        }
    }

    // 새 입장이 막힌 방(기입장·진행 중)은 재입장으로 원래 참가자 행을 되살려 들어간다.
    // 새 행을 만들지 않아야 참가자 id가 채워지고 점수·답안이 갈라지지 않는다 (규칙 §2-1-2 재접속 복구)
    private suspend fun enterAlreadyJoinedRoom(room: RoomInfo, fallbackMessage: String) {
        rejoinRoomUseCase.invoke(room)
            .onSuccess {
                _uiState.update { it.copy(pin = "", roomInfo = null) }
                _event.emit(JoinEvent.ShowNotice(RESUMED_NOTICE))
                _event.emit(JoinEvent.JoinCompleted(room.pin))
            }
            .onFailure { error ->
                _event.emit(JoinEvent.ShowNotice(rejoinErrorMessage(error, fallbackMessage)))
            }
    }

    // 서버는 입장 409를 닉네임 중복·기입장·입장 불가·정원 초과 네 가지로 준다.
    // code로 갈라야 문구가 맞고, 닉네임을 바꿔도 안 들어가지는 상태가 안 생긴다.
    // 모르는 409(code 없음 포함)를 닉네임 중복이라고 하면 이유를 알 수 없으니 일반 실패로 둔다 (규칙 §10)
    private suspend fun handleJoinConflict(room: RoomInfo, error: AppError.Conflict) {
        val code = error.serverCode

        if (code == ServerErrorCode.ALREADY_JOINED) {
            enterAlreadyJoinedRoom(room, "이미 입장해 있는 방인데 다시 들어가지 못했어요. 잠시 후 다시 시도해 주세요")
        } else if (code == ServerErrorCode.ROOM_NOT_JOINABLE) {
            // 진행 중인 방은 새로 입장할 수 없지만, 전에 들어갔던 사람은 돌아올 수 있다.
            // 들어간 적이 없으면 서버가 404를 주고 원래 문구로 돌아간다
            enterAlreadyJoinedRoom(room, "이미 시작했거나 끝난 방이라 입장할 수 없어요")
        } else if (code == ServerErrorCode.ROOM_FULL) {
            _event.emit(JoinEvent.ShowNotice("정원이 가득 찼어요"))
        } else if (code == ServerErrorCode.NICKNAME_DUPLICATED) {
            _event.emit(JoinEvent.ShowNotice("이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"))
        } else {
            _event.emit(JoinEvent.ShowNotice(roomErrorMessage(error)))
        }
    }

    private suspend fun handleJoinFailure(room: RoomInfo, error: AppError) {
        when (error) {
            is AppError.Conflict -> handleJoinConflict(room, error)
            is AppError.PermissionDenied -> _event.emit(JoinEvent.ShowNotice(forbiddenMessage(error.serverCode)))
            is AppError.LoginRequired -> {
                _event.emit(JoinEvent.ShowNotice("유료 방은 로그인 후 입장할 수 있어요"))
                _event.emit(JoinEvent.SignInRequiredForPaidRoom(_uiState.value.pin))
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

    companion object {
        private const val RESUMED_NOTICE = "이미 입장해 있는 방이에요. 처음 입장한 이름으로 이어서 들어갈게요"
    }
}
