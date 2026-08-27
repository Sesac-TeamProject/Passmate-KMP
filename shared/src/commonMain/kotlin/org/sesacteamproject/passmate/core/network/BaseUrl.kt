package org.sesacteamproject.passmate.core.network

// 로컬 백엔드 기준 기본값 — 배포 환경 전환은 추후 빌드 설정으로 분리한다
expect fun defaultApiBaseUrl(): String

expect fun defaultWsUrl(): String
