package org.sesacteamproject.passmate.ui.auth

import java.awt.Desktop
import java.net.URI

actual fun openSignInPage(url: String) {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null

    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
        desktop.browse(URI(url))
    }
}
