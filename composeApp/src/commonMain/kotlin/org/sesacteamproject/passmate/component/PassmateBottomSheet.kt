package org.sesacteamproject.passmate.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.theme.PassmateColors

// 시안 v6의 바텀 시트 공통 껍데기 — 앱의 모든 ModalBottomSheet는 이걸 쓴다.
//
// tonalElevation을 0으로 끄는 것이 핵심이다. Material3의 Surface는 containerColor가
// MaterialTheme.colorScheme.surface와 같을 때만 surfaceColorAtElevation을 태우는데,
// PassmateTheme이 surface에 PassmateColors.Surface를 그대로 넣어 두어 조건이 맞는다.
// 그 결과 기본 elevation에서 surfaceTint(지정하지 않아 primary=민트)가 5%가량 덧칠돼
// 시트가 순백으로 보이지 않았다. 0.dp면 surface를 그대로 반환한다.
//
// 표시 여부와 생명주기는 호출한 화면이 소유한다 (규칙 §11-1)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassmateBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = PassmateColors.Surface,
        tonalElevation = 0.dp,
        content = content
    )
}
