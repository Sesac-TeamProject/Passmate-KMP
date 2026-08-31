package org.sesacteamproject.passmate.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

// viewModelScope가 쓰는 Dispatchers.Main을 테스트 디스패처로 교체한다
@OptIn(ExperimentalCoroutinesApi::class)
object TestMainDispatcher {

    fun install() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun reset() {
        Dispatchers.resetMain()
    }
}
