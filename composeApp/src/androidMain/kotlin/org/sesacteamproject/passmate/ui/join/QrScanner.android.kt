package org.sesacteamproject.passmate.ui.join

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
actual fun rememberQrScanLauncher(onResult: (String?) -> Unit): (() -> Unit)? {
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        currentOnResult.value(result.contents)
    }

    return remember(launcher) {
        {
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("방 화면의 QR 코드를 비춰 주세요")
                .setBeepEnabled(false)
                .setOrientationLocked(true)

            launcher.launch(options)
        }
    }
}
