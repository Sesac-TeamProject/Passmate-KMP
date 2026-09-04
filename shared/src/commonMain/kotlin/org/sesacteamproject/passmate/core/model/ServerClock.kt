package org.sesacteamproject.passmate.core.model

// 스냅샷 응답의 서버 시각을 본문 시각과 같은 시계에 올려 준다.
//
// 세션 스냅샷 본문에는 "지금"이 없어서 HTTP `Date` 헤더를 서버 시각으로 쓴다
// (SessionRemoteDataSource). 헤더는 규격상 GMT이고 본문 시각은 오프셋 없는
// `LocalDateTime`이라 표기가 달라 보이지만, 실측하면 같은 순간을 가리킨다.
//
// 2026-09-04 로컬 백엔드, 같은 응답에서:
//   Date 헤더         Fri, 04 Sep 2026 06:43:43 GMT
//   본문 lastLoginAt  2026-09-04T06:43:43.263979   ← 초까지 일치
//
// 즉 서버가 본문에도 UTC를 담고 있어 옮길 필요가 없다. `IsoTime`은 오프셋이 없으면
// UTC로 읽으므로 헤더 값을 그대로 본문 시각과 비교할 수 있다.
//
// ⚠️ 서버 JVM 시간대가 바뀌어 본문이 지역 시각(KST)이 되면 이 전제가 깨진다.
// 근본 해결은 서버가 오프셋을 실어 주는 것이다(`OffsetDateTime`) — 백엔드 전달 1순위.
// 그때까지 이 자리는 "헤더와 본문을 같은 시계에 놓는다"는 책임만 갖는다.
object ServerClock {

    fun toServerLocalIso(httpDate: String?): String? {
        return HttpDate.toIsoOrNull(httpDate)
    }
}
