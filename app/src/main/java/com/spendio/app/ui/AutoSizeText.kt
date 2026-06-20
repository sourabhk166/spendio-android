package com.spendio.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A single-line [Text] that shrinks its font size (down to [minFontSize]) until it fits the
 * available width, instead of wrapping or spilling outside its box. Used wherever a value of
 * unpredictable length (a growing rupee total, a formatted date range) must stay inside a
 * fixed-size space — the donut chart's center hole, a dialog headline.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Center,
    minFontSize: TextUnit = 11.sp,
) {
    var scaledStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.fillMaxWidth().drawWithContent { if (readyToDraw) drawContent() },
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        style = scaledStyle,
        onTextLayout = { result ->
            val current = scaledStyle.fontSize
            if (result.didOverflowWidth && current > minFontSize) {
                scaledStyle = scaledStyle.copy(fontSize = (current.value * 0.92f).sp)
            } else {
                readyToDraw = true
            }
        },
    )
}
