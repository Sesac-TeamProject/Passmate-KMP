package org.sesacteamproject.passmate.ui.home

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.payment.domain.usecase.GetPublicRoomsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomPinUseCase
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

// GET /rooms/public 응답에는 pin이 없다(계약 `PublicRoomResponse`). 방 카드를 누르면
// roomId로 방을 한 번 더 조회해 pin을 얻은 뒤 Join 라우트로 보낸다.
@OptIn(ExperimentalCoroutinesApi::class)
class RoomListPinResolveTest {

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    private fun roomListViewModel(getRoomPinUseCase: GetRoomPinUseCase): RoomListViewModel {
        return RoomListViewModel(
            getPublicRoomsUseCase = GetPublicRoomsUseCase(FakePaymentRepository()),
            getRoomPinUseCase = getRoomPinUseCase
        )
    }

    @Test
    fun resolvesPinByRoomIdBeforeOpeningRoom() = runTest {
        val repository = FakeRoomRepository()
        repository.pinByRoomId = mapOf(701L to "482913")
        val useCase = GetRoomPinUseCase(repository)
        val events = mutableListOf<RoomListEvent>()

        val viewModel = roomListViewModel(useCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }

        viewModel.onAction(RoomListAction.ClickRoom(roomId = 701L))

        assertEquals(1, events.size)
        assertEquals(RoomListEvent.OpenRoom("482913"), events.first())
    }

    @Test
    fun showsNoticeWhenRoomLookupFails() = runTest {
        val repository = FakeRoomRepository()
        repository.pinByRoomId = emptyMap()
        val useCase = GetRoomPinUseCase(repository)
        val events = mutableListOf<RoomListEvent>()

        val viewModel = roomListViewModel(useCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }

        viewModel.onAction(RoomListAction.ClickRoom(roomId = 999L))

        // pin을 얻지 못하면 이동하지 않고 안내만 낸다
        assertEquals(1, events.size)
        assertTrue(events.first() is RoomListEvent.ShowNotice)
    }
}
