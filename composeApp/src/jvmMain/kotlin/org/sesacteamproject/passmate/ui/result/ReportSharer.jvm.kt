package org.sesacteamproject.passmate.ui.result

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

// Desktop은 시스템 공유 시트가 없어 클립보드 복사로 대체한다
@Composable
actual fun rememberReportSharer(): (String) -> Unit {
    return { summary ->
        val selection = StringSelection(summary)

        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
}
