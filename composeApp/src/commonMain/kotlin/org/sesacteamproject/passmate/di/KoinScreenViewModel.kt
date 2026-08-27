package org.sesacteamproject.passmate.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.mp.KoinPlatform

// 생성은 Koin, 보관·수명은 lifecycle (아키텍처 설계 §4-4). 컨테이너 Screen에서만 호출한다
@Composable
inline fun <reified VM : ViewModel> koinScreenViewModel(): VM =
    viewModel { KoinPlatform.getKoin().get<VM>() }
