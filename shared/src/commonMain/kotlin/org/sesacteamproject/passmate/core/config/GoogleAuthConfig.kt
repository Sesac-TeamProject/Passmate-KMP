package org.sesacteamproject.passmate.core.config

/**
 * 구글 로그인에 쓰는 OAuth 클라이언트 값. 클라이언트 ID는 비밀이 아니라 앱에 박아도 된다
 * (비밀은 백엔드만 가지는 client secret이다).
 *
 * [WEB_CLIENT_ID]는 백엔드의 `GOOGLE_CLIENT_ID`와 **같은 값이어야 한다** — 서버가 ID 토큰의
 * `aud`를 이 값으로 검증하기 때문이다(`GoogleOAuthClientImpl.AudienceValidator`).
 * Android SDK에는 `serverClientId`로 넘긴다. iOS는 자기 클라이언트 ID를 Info.plist
 * (`GIDClientID`)에서 읽으므로 이 값을 쓰지 않는다.
 */
object GoogleAuthConfig {

    const val WEB_CLIENT_ID = "806560773397-3anpgravtn4heol165uekbr5mas99kk2.apps.googleusercontent.com"
}
