package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.navigation.ArthaDestination
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.PlusJakartaSans
import com.subramanya.artha.ui.theme.Surface0
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text2

/**
 * HANDOFF chrome.jsx · BottomTabs — Surface0 @ 92% deep-ink fill with a single
 * Line1 hairline on top, NO Material surface tint or shadow. Active tab shows a
 * 56×28 Teal900 pill behind a Teal300 icon; label sits below, 11sp 500,
 * Teal300 when active / Text2 when not. Padding mirrors the JSX: 6dp top,
 * 8dp horizontal, 10dp bottom (plus the system nav-bar inset).
 */
@Composable
fun ArthaBottomBar(
    currentDestination: ArthaDestination?,
    onItemSelected: (ArthaDestination) -> Unit,
    modifier: Modifier = Modifier,
    reviewBadgeCount: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface0.copy(alpha = 0.92f))
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = Line1, thickness = Dp.Hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArthaDestination.bottomNav.forEach { dest ->
                BottomTabItem(
                    destination = dest,
                    selected = dest == currentDestination,
                    onClick = { onItemSelected(dest) },
                    badgeCount = if (dest == ArthaDestination.Review) reviewBadgeCount else 0,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    destination: ArthaDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val interaction = remember { MutableInteractionSource() }
    val pillBg = if (selected) Teal900 else Color.Transparent
    val tint = if (selected) Teal300 else Text2

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center,
        ) {
            if (badgeCount > 0) {
                androidx.compose.material3.BadgedBox(
                    badge = {
                        androidx.compose.material3.Badge {
                            Text(text = if (badgeCount > 99) "99+" else badgeCount.toString())
                        }
                    },
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = stringResource(destination.labelRes),
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = stringResource(destination.labelRes),
            color = tint,
            fontFamily = PlusJakartaSans,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
