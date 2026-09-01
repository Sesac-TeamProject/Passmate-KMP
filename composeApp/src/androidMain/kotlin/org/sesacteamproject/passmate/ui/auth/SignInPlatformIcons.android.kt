package org.sesacteamproject.passmate.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.sesacteamproject.passmate.R

@Composable
internal actual fun GoogleSignInIcon(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.frame),
        contentDescription = null,
        modifier = modifier
    )
}
