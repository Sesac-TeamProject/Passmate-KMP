package org.sesacteamproject.passmate.room.domain.model

// 학생 캐릭터 키 — 서버와 주고받는 값은 숫자가 아니라 문자열이다
// (시안 "학생 아바타 — 키 이름 (avatarId)", ERD `avatar_id varchar(30)`).
// 화면은 1..12 인덱스로 캐릭터를 그리므로 데이터 계층에서만 키↔인덱스를 바꾼다.
//
// 이 순서는 시안의 캐릭터 선택 그리드 순서다. 순서를 바꾸면 기존 사용자의
// 캐릭터가 전부 어긋나므로 바꾸지 않는다.
object StudentAvatarKeys {

    val ordered: List<String> = listOf(
        "cat", "dog", "bear", "panda", "rabbit", "fox",
        "frog", "penguin", "owl", "tiger", "raccoon", "dino"
    )

    // 서버가 모르는 키를 주면 null — 화면은 기본 캐릭터로 접는다
    fun toIndex(key: String?): Int? {
        val position = ordered.indexOf(key)

        return if (position >= 0) {
            position + 1
        } else {
            null
        }
    }

    fun toKey(index: Int?): String? {
        return if (index != null && index in 1..ordered.size) {
            ordered[index - 1]
        } else {
            null
        }
    }
}
