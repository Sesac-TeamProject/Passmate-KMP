package org.sesacteamproject.passmate.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.sesacteamproject.passmate.session.domain.model.VoiceHint
import org.sesacteamproject.passmate.theme.PassmateColors

// PTT 학생 배너 — Figma "PTT 음성 힌트 상태 시트"(349:5051) 재생/일시정지/다시 듣기/실패 상태 (FR-040)
@Composable
fun VoiceHintBanner(
    hint: VoiceHint,
    controller: VoiceHintPlayerController?,
    onClickReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playbackState by remember { mutableStateOf(VoiceHintPlaybackState.IDLE) }
    var positionMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hint.hintId, controller) {
        while (controller != null) {
            playbackState = controller.playbackState()
            positionMillis = controller.positionMillis()
            delay(POLL_INTERVAL_MS)
        }
    }
    if (controller == null) {
        UnsupportedChip(modifier = modifier)
    } else {
        when (playbackState) {
            VoiceHintPlaybackState.PLAYING, VoiceHintPlaybackState.PAUSED -> PlayingBar(
                hint = hint,
                isPlaying = playbackState == VoiceHintPlaybackState.PLAYING,
                positionMillis = positionMillis,
                onClickPause = { controller.pause() },
                onClickResume = { controller.resume() },
                modifier = modifier
            )
            VoiceHintPlaybackState.FAILED -> FailedChip(
                onClickRetry = onClickReplay,
                modifier = modifier
            )
            else -> ReplayChip(
                hint = hint,
                onClickReplay = onClickReplay,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PlayingBar(
    hint: VoiceHint,
    isPlaying: Boolean,
    positionMillis: Long,
    onClickPause: () -> Unit,
    onClickResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(PassmateColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HintIcon()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "선생님 음성 힌트",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            HintProgressBar(
                positionMillis = positionMillis,
                durationMillis = hint.durationMs
            )
        }
        Text(
            text = "${formatClock(positionMillis)} / ${formatClock(hint.durationMs)}",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        if (isPlaying) {
            PauseButton(onClick = onClickPause)
        } else {
            Text(
                text = "▶",
                color = PassmateColors.Primary,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onClickResume)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun HintIcon() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(PassmateColors.Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 11.dp)
                    .background(PassmateColors.Surface, RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(width = 2.dp, height = 3.dp)
                    .background(PassmateColors.Surface)
            )
        }
    }
}

@Composable
private fun HintProgressBar(
    positionMillis: Long,
    durationMillis: Long
) {
    val fraction = if (durationMillis > 0) {
        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(6.dp)
            .background(PassmateColors.FieldGray, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun PauseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ReplayChip(
    hint: VoiceHint,
    onClickReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(PassmateColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClickReplay)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "▶",
            color = PassmateColors.Primary,
            fontSize = 12.sp
        )
        Text(
            text = "다시 듣기 · ${formatClock(hint.durationMs)}",
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun FailedChip(
    onClickRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(PassmateColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClickRetry)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "!",
            color = PassmateColors.ChipOrangeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "재생에 실패했어요 — 탭해서 다시 시도",
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun UnsupportedChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "음성 힌트가 도착했어요 — 이 기기에서는 재생이 지원되지 않아요",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
    }
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minutePart = minutes.toString().padStart(2, '0')
    val secondPart = seconds.toString().padStart(2, '0')

    return "$minutePart:$secondPart"
}

private const val POLL_INTERVAL_MS = 200L
