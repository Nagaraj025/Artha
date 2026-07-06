package com.subramanya.artha.ui.common

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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line amount Text that auto-shrinks the font size to fit its measured
 * width before drawing. Hero amounts in the editorial style (Net Position,
 * Total Liquid, Total Outstanding, Net Worth) all render in InstrumentSerif at
 * 32-56sp inside a column constrained to ~screen-width minus padding/corner
 * glyph. A long INR amount like ₹1,00,00,000 wraps to a second line at the
 * default size; this composable shrinks the font 10% at a time until it fits
 * (or hits [minFontSize]) so the layout stays one row on every device width.
 *
 * The first frame is suppressed via [drawWithContent] so the user never sees
 * the oversized intermediate before the shrink lands. Matches Apple's
 * "minimumScaleFactor" behaviour on UILabel.
 */
@Composable
fun AutoShrinkAmountText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 14.sp,
) {
    // Re-seed when text or starting style changes so a longer amount can still
    // restart the shrink loop instead of staying clamped at the previous fit.
    var resolvedStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        style = resolvedStyle,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && resolvedStyle.fontSize > minFontSize) {
                resolvedStyle = resolvedStyle.copy(
                    fontSize = resolvedStyle.fontSize * 0.9f,
                    // Keep line-height proportional so the box doesn't end up
                    // taller than the visual baseline of the smaller glyph.
                    lineHeight = resolvedStyle.lineHeight * 0.9f,
                )
            } else {
                readyToDraw = true
            }
        },
    )
}
