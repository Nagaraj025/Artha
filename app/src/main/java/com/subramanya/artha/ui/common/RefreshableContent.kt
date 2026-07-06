package com.subramanya.artha.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Pull-to-refresh wrapper. Our screens are Flow-driven (Room emits on every write),
 * so refresh is mostly a UX affordance — users expect to be able to pull. We flash
 * the spinner for [feedbackMillis] to acknowledge the gesture and then drop it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(feedbackMillis: Long = 400L, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(feedbackMillis)
            isRefreshing = false
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = modifier,
        content = content,
    )
}
