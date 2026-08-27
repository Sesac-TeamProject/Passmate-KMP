package org.sesacteamproject.passmate.core.di

import org.koin.core.module.Module

// TokenStorage처럼 플랫폼 생성자가 다른 core 의존성만 등록한다
expect val platformCoreModule: Module
