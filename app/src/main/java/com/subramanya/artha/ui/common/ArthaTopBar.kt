package com.subramanya.artha.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.utils.DateFormatter

/**
 * Greeting top bar per the design's [DashHeader] component:
 *
 *   [अ] EYEBROW DATE [search]
 *       Namaste, {name}
 *
 * The brand mark replaces the old greeting icon; "Namaste, X" replaces the
 * earlier wave-emoji greeting so we own an Indian voice consistently.
 * (HANDOFF §6.11 — no emoji anywhere in the app.)
 */
@Composable
fun ArthaTopBar(userName: String? = null, modifier: Modifier = Modifier, onSearchClick: () -> Unit = {}) {
    val today = remember { DateFormatter.todayShort() }
    val greeting = if (userName.isNullOrBlank()) {
        stringResource(R.string.greeting_guest)
    } else {
        stringResource(R.string.greeting_named, userName)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Edge-to-edge: keep the greeting bar clear of the system status bar
            // (clock + signal + battery) instead of letting it overlap.
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(size = 40.dp, cornerRadiusDp = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.uppercase(),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.transactions_search_placeholder),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
