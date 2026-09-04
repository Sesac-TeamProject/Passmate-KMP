package org.sesacteamproject.passmate.session.domain.model

// 서버가 준 응답 분포(Map<제출문자열, 수>)를 보기 순서 목록으로 편다.
//
// 키는 학생이 제출한 문자열 그대로다 — 백엔드가 answers.groupingBy { it.submitted }로 세기 때문에
// 객관식은 보기 원문("1"·"2"…), OX는 "O"/"X"가 키로 온다(로컬 실서버 실측 2026-09-04).
// 그래서 보기 목록과는 문자열 비교로 짝지어진다.
//
// Compose와 SwiftUI가 각자 짝지으면 미러가 어긋나므로 여기 한 곳에만 둔다 (규칙 §2)
fun SessionQuestion.distributionOf(
    raw: Map<String, Int>,
    answer: String?,
    myChoiceIndex: Int?
): List<ChoiceDistribution> {
    val choices = answerChoices
    val myChoice = myChoiceIndex?.let { choices.getOrNull(it) }

    return choices.mapIndexed { index, label ->
        ChoiceDistribution(
            choiceNo = index + 1,
            label = label,
            // 아무도 안 고른 보기는 키가 없다 — 0명 행으로 그린다(시안 M-04의 4번 보기)
            count = raw[label] ?: 0,
            isAnswer = label == answer,
            isMine = label == myChoice
        )
    }
}
