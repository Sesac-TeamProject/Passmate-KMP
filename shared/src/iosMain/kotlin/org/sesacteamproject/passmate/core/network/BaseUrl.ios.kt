package org.sesacteamproject.passmate.core.network

import platform.Foundation.NSBundle

// 서버 주소는 xcconfig → Info.plist를 거쳐 들어온다. 실기기 테스트에서 소스를 고치지 않도록
// `iosApp/Configuration/Local.xcconfig`(gitignore)에서 PASSMATE_SERVER_HOST를 덮어쓴다
private const val SERVER_HOST_KEY = "PassmateServerHost"

private val serverHost: String by lazy {
    val configured = NSBundle.mainBundle.objectForInfoDictionaryKey(SERVER_HOST_KEY) as? String

    resolveServerHost(configured)
}

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(serverHost)

actual fun defaultWsUrl(): String = wsUrlOf(serverHost)
