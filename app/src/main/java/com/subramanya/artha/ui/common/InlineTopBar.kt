package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Text1

/**
 * HANDOFF chrome.jsx · TopBar — inline scrolling header for sub-screens.
 *
 * NOT a Material `TopAppBar` (which is pinned). This is intended to be the
 * first child of a scroll container so it moves up with the content, exactly
 * like the design prototype's `TopBar` does inside its `phone-content` div.
 *
 * Layout: 44dp circular back button + 20sp/600 title with -0.01em tracking,
 * Surface1 fill, ~56dp minHeight. `statusBars = true` adds the system status
 * bar inset so the back button sits below the clock/icons even on edge-to-edge.
 */
@Composable
fun InlineTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    statusBars: Boolean = true,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .background(Surface1)
        .let { if (statusBars) it.statusBarsPadding() else it }

    Row(
        modifier = baseModifier
            .heightIn(min = 56.dp)
            .padding(start = 4.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Text1,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            // Maintain the same left gutter (16dp) the JSX uses when no back arrow exists.
            Box(modifier = Modifier.size(width = 12.dp, height = 1.dp))
        }
        Text(
            text = title,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Text1,
                letterSpacing = (-0.01).em,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 12.dp else 0.dp),
        )
        if (trailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
    }
}
