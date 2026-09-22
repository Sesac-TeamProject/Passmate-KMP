package org.sesacteamproject.passmate.core.config

/**
 * 베타 운영 잠금 (2026-09-20 결정, Figma "13 · 베타 운영").
 *
 * BM(코인·참가비)이 확정되기 전까지 결제를 통째로 막는다 — 코인 충전(M-12-4), 유료 방 입장 결제(M-11),
 * 유료 방 개설(M-13a)이 한 스위치로 같이 잠긴다. 결제만 막고 유료 방을 열어 두면 아무도 못 들어가는 방이 생긴다.
 *
 * 정식 출시 때 `false`로 바꾸면 세 곳이 한꺼번에 열린다. 화면마다 따로 되돌릴 것은 없다.
 * 웹의 `src/config/beta.ts`(`BETA_PAYMENT_LOCKED`)와 **같은 뜻·같은 이름**이다 — 출시 때 둘을 같이 내린다.
 * iOS는 `BetaConfig.shared.BETA_PAYMENT_LOCKED`로 읽는다.
 */
object BetaConfig {

    const val BETA_PAYMENT_LOCKED = true
}
