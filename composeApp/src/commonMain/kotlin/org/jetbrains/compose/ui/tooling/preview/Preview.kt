package org.jetbrains.compose.ui.tooling.preview

// ⚠️ CMP 1.6이 제공하는 공용 @Preview의 자리를 임시로 메우는 선언이다. 남의 패키지에 두는 이유:
//
//   Android Studio는 commonMain의 Compose 프리뷰를 이 FQN
//   (org.jetbrains.compose.ui.tooling.preview.Preview)으로만 인식한다.
//   우리 이름(@PassmatePreview)이나 expect 애노테이션은 인식하지 못한다.
//   — 2026-09-01 AS 2025.3.3에서 프로브 파일로 직접 확인(A/B/C/D 대조).
//
//   CMP 1.5.12의 ui-tooling-preview 공용 아티팩트는 비어 있어 이 타입이 존재하지 않는다
//   (metadata jar 안에 클래스 0개). 버전 고정은 팀 결정이라 올릴 수 없다(설계문서 ADR #2).
//
// CMP를 1.6 이상으로 올릴 때 이 파일을 삭제한다. 안 지우면 중복 선언으로 컴파일이 즉시
// 실패하므로 조용히 어긋날 여지는 없다.
//
// 앱 런타임에는 영향이 없다 — 프리뷰 애노테이션은 IDE 툴링만 읽는다.
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Preview
