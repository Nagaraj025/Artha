package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.Text4

/**
 * Lambda for ModalBottomSheet's `contentWindowInsets` slot. Defaults to
 * `WindowInsets.systemBars.only(Top + Horizontal)` so the sheet doesn't draw
 * under the status-bar clock/icons on edge-to-edge devices once the parent
 * Scaffold sets contentWindowInsets = WindowInsets(0).
 *
 * Pass via `contentWindowInsets = SheetWindowInsets` on every ModalBottomSheet
 * in the app.
 */
val SheetWindowInsets: @Composable () -> WindowInsets = {
    WindowInsets.systemBars.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
}

/**
 * HANDOFF sheets-extra.jsx · SheetTitle — 22sp 600 title (-0.01em), optional
 * 12sp Text3 sub. Used at the top of every Add/Edit sheet, just below the
 * 36×4 drag handle.
 */
@Composable
fun SheetTitle(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 6.dp)) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text1,
                letterSpacing = (-0.01).em,
            ),
        )
        if (sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = Text3,
            )
        }
    }
}

/**
 * HANDOFF sheets-extra.jsx · FieldRow — eyebrow label + optional "· optional"
 * suffix + optional right-aligned hint, with the body slot below at 8dp
 * separation. mt 18dp gives consistent rhythm between rows.
 */
@Composable
fun FieldRow(
    label: String,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(top = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    style = EyebrowStyle,
                    color = Text2,
                )
                if (optional) {
                    Text(
                        text = " · optional",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.04.em,
                        ),
                        color = Text3,
                    )
                }
            }
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Text3,
                )
            }
        }
        content()
    }
}

/**
 * HANDOFF tokens.css · `.chip` / `.chip.active` — segmented chip row. Single-
 * select variant. Active chips render Teal900 fill + Teal500 border + Teal300
 * text; inactive ones use transparent + Line2 border + Text1.
 */
data class PillOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> PillRadio(value: T?, options: List<PillOption<T>>, onChange: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            ChipPill(
                active = value == opt.value,
                label = opt.label,
                icon = opt.icon,
                onClick = { onChange(opt.value) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> PillRadioMulti(values: Set<T>, options: List<PillOption<T>>, onToggle: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            ChipPill(
                active = opt.value in values,
                label = opt.label,
                icon = opt.icon,
                onClick = { onToggle(opt.value) },
            )
        }
    }
}

@Composable
private fun ChipPill(active: Boolean, label: String, icon: ImageVector?, onClick: () -> Unit) {
    val container = if (active) Teal900 else Color.Transparent
    val border = if (active) Teal500 else Line1
    val content = if (active) Teal300 else Text1
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * HANDOFF tokens.css · `.input` — text field used inside FieldRow. 56dp tall
 * Surface2 fill, 1dp Line1 border, 10dp radius. Optional `suffix` renders a
 * mono Text3 unit (₹ / %) right-aligned at 16dp.
 *
 * Built on BasicTextField for full styling control (Material's OutlinedTextField
 * has its own padding/floating-label rules that fight the design).
 */
@Composable
fun ArthaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    /** Optional helper text shown below the field. When non-null and isError
     *  is true, it renders in Danger; otherwise in Text3. */
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    large: Boolean = false,
) {
    val height = if (large) 56.dp else 48.dp
    val border = if (isError) com.subramanya.artha.ui.theme.Danger else Line1
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                cursorBrush = SolidColor(Teal300),
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSans,
                    fontSize = if (large) 16.sp else 14.sp,
                    color = Text1,
                ),
                // fillMaxWidth + weight(1f) ensures the tappable + focusable region
                // covers the whole input — without it, an empty BasicTextField is
                // 0dp wide so taps on the placeholder land on the parent Row and
                // nothing happens. (This is what shipped before the fix.)
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontFamily = PlusJakartaSans,
                                fontSize = if (large) 16.sp else 14.sp,
                                color = Text4,
                            ),
                        )
                    }
                    inner()
                },
            )
            if (suffix != null) {
                Spacer(Modifier.size(10.dp))
                Text(
                    text = suffix,
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 13.sp,
                        color = Text3,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
        if (supportingText != null) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isError) com.subramanya.artha.ui.theme.Danger else Text3,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/**
 * HANDOFF sheets-extra.jsx · ColorSwatchRow — 30dp colour circles, 10dp gap.
 * Selected gets a 2dp Text1 outline with 2dp offset (a thin gap then the ring).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchRow(
    value: Long,
    swatches: List<Long>,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    /** When non-null, renders a trailing "+" tile to add a custom swatch. */
    onAdd: (() -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        swatches.forEach { c ->
            val isSelected = c == value
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    // Outline ring with a 2dp gap.
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, Text1, CircleShape),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .clickable { onChange(c) },
                )
            }
        }
        onAdd?.let { add ->
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(1.dp, Line1, CircleShape)
                        .clickable { add() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = Text2,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * HANDOFF sheets-extra.jsx · IconChipRow — 40dp Surface2 chip tiles (11dp
 * radius) with Line1 border. Selected = Teal900 fill + Teal500 border +
 * Teal300 icon. Used by Add Account / Add Category for the glyph picker.
 */
data class IconChoice(val key: String, val icon: ImageVector)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconChipRow(
    value: String,
    icons: List<IconChoice>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** When non-null, renders a trailing "+" tile to add a custom icon. */
    onAdd: (() -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icons.forEach { choice ->
            val isSelected = choice.key == value
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) Teal900 else Surface2)
                    .border(
                        1.dp,
                        if (isSelected) Teal500 else Line1,
                        RoundedCornerShape(11.dp),
                    )
                    .clickable { onChange(choice.key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = choice.icon,
                    contentDescription = null,
                    tint = if (isSelected) Teal300 else Text2,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        onAdd?.let { add ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Surface2)
                    .border(1.dp, Line1, RoundedCornerShape(11.dp))
                    .clickable { add() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = Text2,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * HANDOFF tokens.css · `.btn-primary` — 52dp Teal700 pill button, 15sp/600,
 * 0.02em letter-spacing, Text1. Disabled state goes Surface3 + Text3.
 * Width-stretched by default so Add sheets can use it as a footer.
 */
@Composable
fun SavePrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val container = if (enabled) Teal700 else Surface3
    val content = if (enabled) Text1 else Text3
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = content,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.02.em,
            ),
        )
    }
}

/** Ghost variant used for secondary buttons (Cancel / Skip). 44dp tall. */
@Composable
fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Transparent)
            .border(1.dp, Line1, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Text1,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Convenience field-row that wraps a stand-alone chip-shaped picker (e.g. the
 * date-time row). Surface2 + Line1 + 10dp radius matching ArthaTextField's
 * visual weight so they line up neatly inside a sheet.
 */
@Composable
fun SheetChip(label: String, leading: ImageVector? = null, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Line1, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = Text2,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            color = Text1,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Shorthand alias to keep call sites stable. */
@OptIn(ExperimentalMaterial3Api::class)
internal val DefaultBottomSheetWindowInsets
    @Composable
    get() = BottomSheetDefaults.windowInsets

@Suppress("unused")
private val keepLineTealRef = LineTeal

@Suppress("unused")
private val keepSurface4Ref = Surface4

@Suppress("unused")
private val keepStatusBars: WindowInsets
    @Composable
    get() = WindowInsets.statusBars
