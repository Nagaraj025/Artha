package com.subramanya.artha.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Line2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.TiroDevanagariHindi

/**
 * The अ brand mark in a tinted rounded box. Used in the top bar, hero cards, etc.
 * Defaults match the design's "corner mark" style — teal-900 background with
 * teal-300 glyph. Override for the splash version (teal-700 / white).
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    background: Color = Teal900,
    foreground: Color = Teal300,
    cornerRadiusDp: Dp = (size.value * 0.22f).dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "अ",
            color = foreground,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = TiroDevanagariHindi,
                fontWeight = FontWeight.Normal,
                fontSize = (size.value * 0.6f).sp,
                lineHeight = (size.value * 0.6f).sp,
            ),
        )
    }
}

/**
 * Section eyebrow — tiny all-caps label with a hairline teal tick to its left,
 * recurring throughout the app per the design's [SectionHeader] component.
 */
@Composable
fun SectionEyebrow(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showTick: Boolean = true,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        if (showTick) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            )
        }
        Text(text = label.uppercase(), style = EyebrowStyle, color = color)
    }
}

/**
 * Block-print dot grid — extremely subtle background pattern used inside hero
 * cards. Tints the dots with [tint]; default opacity ~6% matches the design.
 */
@Composable
fun BlockPrintOverlay(modifier: Modifier = Modifier, tint: Color = Teal300, alpha: Float = 0.06f) {
    Canvas(modifier = modifier) {
        val step = 12.dp.toPx()
        val r = 0.7.dp.toPx()
        val r2 = 0.4.dp.toPx()
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val tinted = tint.copy(alpha = alpha)
        val faint = tint.copy(alpha = alpha * 0.5f)
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val x = i * step
                val y = j * step
                // central dot
                drawCircle(color = tinted, radius = r, center = androidx.compose.ui.geometry.Offset(x + step / 2, y + step / 2))
                // corner dots (smaller)
                drawCircle(color = faint, radius = r2, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}

/**
 * Jaali — temple-lattice tile: 5 overlapping stroke-only circles per the
 * design tokens (`p-jaali` pattern in patterns.jsx). Sits behind the
 * Net Position hero and the credit-card tile.
 */
@Composable
fun JaaliOverlay(modifier: Modifier = Modifier, tint: Color = Teal300, alpha: Float = 0.06f) {
    Canvas(modifier = modifier) {
        val tile = 32.dp.toPx()
        val r = 14.dp.toPx()
        val ink = tint.copy(alpha = alpha)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5.dp.toPx())
        val cols = (size.width / tile).toInt() + 2
        val rows = (size.height / tile).toInt() + 2
        for (i in 0..cols) {
            for (j in 0..rows) {
                val cx = i * tile
                val cy = j * tile
                drawCircle(
                    color = ink,
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = stroke,
                )
            }
        }
    }
}

/**
 * Chhatri silhouette — temple-pavilion outline used as a corner ornament on
 * credit cards + goal cards. Replicates Chhatri() from patterns.jsx exactly.
 */
@Composable
fun Chhatri(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // base platform
        drawRect(
            color = tint.copy(alpha = 0.8f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 6f / 64f, h * 52f / 64f),
            size = androidx.compose.ui.geometry.Size(w * 52f / 64f, h * 3f / 64f),
        )
        // four columns
        listOf(10f, 20f, 42f, 52f).forEach { x ->
            drawRect(
                color = tint.copy(alpha = 0.6f),
                topLeft = androidx.compose.ui.geometry.Offset(w * x / 64f, h * 32f / 64f),
                size = androidx.compose.ui.geometry.Size(w * 2f / 64f, h * 20f / 64f),
            )
        }
        // lintel
        drawRect(
            color = tint.copy(alpha = 0.8f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 6f / 64f, h * 29f / 64f),
            size = androidx.compose.ui.geometry.Size(w * 52f / 64f, h * 3f / 64f),
        )
        // dome — quadratic curve from (10, 29) through (32, 4) to (54, 29)
        val dome = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 10f / 64f, h * 29f / 64f)
            quadraticBezierTo(
                w * 32f / 64f,
                h * 4f / 64f,
                w * 54f / 64f,
                h * 29f / 64f,
            )
            close()
        }
        drawPath(dome, color = tint.copy(alpha = 0.85f))
        // finial
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 32f / 64f, h * 2f / 64f),
            end = androidx.compose.ui.geometry.Offset(w * 32f / 64f, h * 8f / 64f),
            strokeWidth = 1.5.dp.toPx(),
        )
        drawCircle(
            color = tint,
            radius = 1.2.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(w * 32f / 64f, h * 2f / 64f),
        )
    }
}

/**
 * Bandhani dot rosette — used as texture on the gradient account cards.
 * Tinted white at low alpha; the gradient under it does the heavy lifting.
 */
@Composable
fun BandhaniOverlay(modifier: Modifier = Modifier, tint: Color = Color.White, alpha: Float = 0.18f) {
    Canvas(modifier = modifier) {
        val step = 20.dp.toPx()
        val rCentre = 1.2.dp.toPx()
        val rPetal = 0.5.dp.toPx()
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val ink = tint.copy(alpha = alpha)
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val cx = i * step + step / 2
                val cy = j * step + step / 2
                drawCircle(color = ink, radius = rCentre, center = androidx.compose.ui.geometry.Offset(cx, cy))
                val o = step * 0.3f
                drawCircle(color = ink, radius = rPetal, center = androidx.compose.ui.geometry.Offset(cx, cy - o))
                drawCircle(color = ink, radius = rPetal, center = androidx.compose.ui.geometry.Offset(cx, cy + o))
                drawCircle(color = ink, radius = rPetal, center = androidx.compose.ui.geometry.Offset(cx - o, cy))
                drawCircle(color = ink, radius = rPetal, center = androidx.compose.ui.geometry.Offset(cx + o, cy))
            }
        }
    }
}

/** Compact-form sparkline (no axis, no labels) for hero cards. */
@Composable
fun Sparkline(points: List<Double>, modifier: Modifier = Modifier, color: Color = Teal300) {
    if (points.size < 2) {
        Box(modifier = modifier.background(Surface4.copy(alpha = 0.4f)))
        return
    }
    val min = points.min()
    val max = points.max()
    val range = (max - min).takeIf { it != 0.0 } ?: 1.0
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 2.dp.toPx()
        val pts = points.mapIndexed { i, v ->
            val x = (i.toFloat() / (points.size - 1)) * w
            val y = (h - pad) - (((v - min) / range).toFloat()) * (h - pad * 2)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        // Gradient fill below the line
        val areaPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = areaPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)),
                startY = 0f,
                endY = h,
            ),
        )
        // Stroked line
        for (i in 0 until pts.size - 1) {
            drawLine(
                color = color,
                start = pts[i],
                end = pts[i + 1],
                strokeWidth = 1.4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        // End dot
        drawCircle(color = color, radius = 2.dp.toPx(), center = pts.last())
    }
}

/**
 * Editorial header for sub-routes (Investments, Insurance, etc.) — back arrow
 * on the left, eyebrow + Instrument-Serif title in the centre, optional
 * trailing slot for an overflow / action icon.
 */
@Composable
fun EditorialSubScreenHeader(
    eyebrow: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = eyebrow.uppercase(), style = EyebrowStyle, color = com.subramanya.artha.ui.theme.Text3)
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.InstrumentSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                ),
            )
        }
        trailing()
    }
}

/** Tiny separator dot used inline between metadata fragments. */
val SeparatorDot = "·"

/** Helper to format a "secondary numeric" subtitle in monospaced grey. */
@Composable
fun MonoMeta(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Text3,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = com.subramanya.artha.ui.theme.IbmPlexMono,
            fontFeatureSettings = "tnum",
            fontSize = 11.sp,
        ),
        modifier = modifier,
    )
}

/**
 * HANDOFF §6 — sheet drag handle: 36 dp wide × 4 dp tall, Line2 colour,
 * 12 dp from the sheet top edge.
 */
@Composable
fun ArthaSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Line2),
        )
    }
}

/**
 * Loading placeholder used by detail screens while the underlying entity is
 * still being resolved from Room. Replaces the bare blank `Box(fillMaxSize())`
 * that gave users nothing to look at on a slow open. Centred, dim, deliberately
 * understated — the data usually arrives within ~200 ms so this should rarely
 * be on screen for long.
 */
@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = Teal300,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Skinned wrapper around M3 AlertDialog. Sets the design tokens (Surface3
 * container, Text1 title, Text2 body, 20dp rounded), and tints the confirm
 * action Teal300 by default — pass `confirmDestructive = true` for delete /
 * reset flows so the confirm button goes Danger instead.
 *
 * Usage:
 *
 *   ArthaAlertDialog(
 *       onDismissRequest = ::dismiss,
 *       title = stringResource(R.string.x_title),
 *       text = stringResource(R.string.x_body),
 *       confirmLabel = stringResource(R.string.x_yes),
 *       confirmDestructive = true,
 *       onConfirm = ::confirm,
 *   )
 */
@Composable
fun ArthaAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = onDismissRequest,
    confirmDestructive: Boolean = false,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        titleContentColor = com.subramanya.artha.ui.theme.Text1,
        textContentColor = com.subramanya.artha.ui.theme.Text2,
        shape = RoundedCornerShape(20.dp),
        title = {
            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                androidx.compose.material3.Text(
                    text = confirmLabel,
                    color = if (confirmDestructive) {
                        com.subramanya.artha.ui.theme.Danger
                    } else {
                        com.subramanya.artha.ui.theme.Teal300
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        },
        dismissButton = if (cancelLabel != null && onCancel != null) {
            {
                androidx.compose.material3.TextButton(onClick = onCancel) {
                    androidx.compose.material3.Text(
                        text = cancelLabel,
                        color = com.subramanya.artha.ui.theme.Text2,
                    )
                }
            }
        } else {
            null
        },
    )
}

/**
 * Tealed Material Switch — checked thumb Text1 on Teal700 track, unchecked
 * thumb Text2 on Surface4 track with Line1 border. Use everywhere instead of
 * raw `Switch(...)` so the lock/data/etc toggles match the design instead of
 * showing M3's default green.
 */
@Composable
fun ArthaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = com.subramanya.artha.ui.theme.Text1,
            checkedTrackColor = Teal700,
            uncheckedThumbColor = com.subramanya.artha.ui.theme.Text2,
            uncheckedTrackColor = com.subramanya.artha.ui.theme.Surface4,
            uncheckedBorderColor = com.subramanya.artha.ui.theme.Line1,
        ),
    )
}

/**
 * Single-segment progress bar matching the rest of the design (rounded pill,
 * Surface4 / White-on-white-18% track depending on context). Replaces M3
 * `LinearProgressIndicator` which has its own M3-tinted defaults that fight
 * the editorial palette.
 */
@Composable
fun LinearMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    fillColor: androidx.compose.ui.graphics.Color = Teal700,
    trackColor: androidx.compose.ui.graphics.Color = com.subramanya.artha.ui.theme.Surface4,
    heightDp: Int = 6,
) {
    val safe = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        if (safe > 0f) {
            Box(
                modifier = Modifier
                    .height(heightDp.dp)
                    .fillMaxWidth(safe)
                    .clip(RoundedCornerShape(999.dp))
                    .background(fillColor),
            )
        }
    }
}

@Suppress("unused")
private fun previewKeep() {
    // Keep an unused reference so Teal700 doesn't get flagged as unused import.
    @Suppress("UNUSED_VARIABLE")
    val t = Teal700
}
