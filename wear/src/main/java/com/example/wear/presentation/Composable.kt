package com.example.wear.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun WearAppWithPages(onScoreUpdate: (command: String) -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> ScoreControlPage(onScoreUpdate = onScoreUpdate)
            1 -> ResetPage(onScoreUpdate = onScoreUpdate)
        }
    }
}

@Composable
fun ScoreControlPage(onScoreUpdate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Quadrant(
                baseColor = Color.Red,
                onClick = { onScoreUpdate(WearableConstants.PLAYER_1_DEC) },
                text = "-",
                modifier = Modifier.weight(1f)
            )
            Quadrant(
                baseColor = Color.Red,
                onClick = { onScoreUpdate(WearableConstants.PLAYER_1_INC) },
                text = "+",
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Quadrant(
                baseColor = Color.Blue,
                onClick = { onScoreUpdate(WearableConstants.PLAYER_2_DEC) },
                text = "-",
                modifier = Modifier.weight(1f)
            )
            Quadrant(
                baseColor = Color.Blue,
                onClick = { onScoreUpdate(WearableConstants.PLAYER_2_INC) },
                text = "+",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun Quadrant(
    baseColor: Color,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val pressedColor = baseColor.copy(
        red = baseColor.red * 0.7f,
        green = baseColor.green * 0.7f,
        blue = baseColor.blue * 0.7f
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) pressedColor else baseColor,
        animationSpec = tween(durationMillis = 100), // Fast animation
        label = "QuadrantColorAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(animatedColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 80.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}


@Composable
fun ResetPage(onScoreUpdate: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { onScoreUpdate(WearableConstants.RESET_SCORES) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.error,
                contentColor = MaterialTheme.colors.onError
            )
        ) {
            Text("Reset\nScores", textAlign = TextAlign.Center)
        }
    }
}

// Preview function remains the same
@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
fun QuadrantPreview() {
    ScoreControlPage(onScoreUpdate = {})
}
