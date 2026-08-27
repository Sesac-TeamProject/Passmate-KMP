package org.sesacteamproject.passmate.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import org.koin.mp.KoinPlatform

actual fun openSignInPage(url: String) {
    val context = KoinPlatform.getKoin().get<Context>()
    val customTabsIntent = CustomTabsIntent.Builder().build()

    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    customTabsIntent.launchUrl(context, Uri.parse(url))
}
