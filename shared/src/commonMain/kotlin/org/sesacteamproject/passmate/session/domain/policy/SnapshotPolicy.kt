package org.sesacteamproject.passmate.session.domain.policy

import org.sesacteamproject.passmate.core.model.IsoTime

// 재접속 스냅샷 프로토콜 — 스냅샷 ts 이전 STOMP 이벤트는 폐기한다 (규칙 §2-1-2)
class SnapshotPolicy {

    fun isStaleFrame(frameTs: String, snapshotTs: String): Boolean {
        val frameMillis = IsoTime.toEpochMillis(frameTs)
        val snapshotMillis = IsoTime.toEpochMillis(snapshotTs)

        return if (frameMillis == null || snapshotMillis == null) {
            false
        } else {
            frameMillis < snapshotMillis
        }
    }
}
