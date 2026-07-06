package com.subramanya.artha.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.GhostButton
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal700
import kotlinx.coroutines.launch

private const val PAGE_COUNT: Int = 3

@Composable
fun OnboardingFlow(viewModel: OnboardingViewModel, onCompleted: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    // Once persistence succeeds, hand control back to MainActivity.
    LaunchedEffect(state.savedAndReady) {
        if (state.savedAndReady) onCompleted()
    }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = canScrollForward(state, pagerState.currentPage),
            ) { page ->
                when (page) {
                    0 -> WelcomeStep()
                    1 -> NameStep(
                        name = state.name,
                        onNameChanged = viewModel::onNameChanged,
                    )
                    2 -> AddAccountStep(
                        draft = state.accountDraft,
                        pendingCount = state.pendingAccounts.size,
                        onNameChanged = viewModel::onAccountNameChanged,
                        onTypeChanged = viewModel::onAccountTypeChanged,
                        onInstitutionChanged = viewModel::onAccountInstitutionChanged,
                        onOpeningBalanceChanged = viewModel::onOpeningBalanceChanged,
                        onAddAnother = { viewModel.stashCurrentAccount() },
                    )
                }
            }

            PageIndicator(
                pageCount = PAGE_COUNT,
                selected = pagerState.currentPage,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            BottomBar(
                currentPage = pagerState.currentPage,
                state = state,
                onBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                onDone = { viewModel.finishOnboarding() },
            )
        }
    }
}

/**
 * Step 2 needs a non-blank name before Next; step 3 needs a valid finish-criteria.
 * Returning false here also disables touch-swipe, so the user can't bypass validation.
 */
private fun canScrollForward(state: OnboardingUiState, currentPage: Int): Boolean = when (currentPage) {
    0 -> true
    1 -> state.name.isNotBlank()
    else -> false
}

@Composable
private fun PageIndicator(pageCount: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val color = if (index == selected) Teal700 else Surface4
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun BottomBar(
    currentPage: Int,
    state: OnboardingUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentPage > 0) {
            Box(modifier = Modifier.weight(1f)) {
                GhostButton(
                    label = stringResource(R.string.onboarding_back),
                    onClick = onBack,
                )
            }
        }
        Box(
            modifier = if (currentPage > 0) Modifier.weight(2f) else Modifier.weight(1f),
        ) {
            if (currentPage < PAGE_COUNT - 1) {
                SavePrimaryButton(
                    label = stringResource(R.string.onboarding_next),
                    enabled = canAdvance(currentPage, state),
                    onClick = onNext,
                )
            } else {
                SavePrimaryButton(
                    label = stringResource(R.string.onboarding_done),
                    enabled = state.canFinishOnboarding && !state.isSaving,
                    onClick = onDone,
                )
            }
        }
    }
}

private fun canAdvance(currentPage: Int, state: OnboardingUiState): Boolean = when (currentPage) {
    0 -> true
    1 -> state.name.isNotBlank()
    else -> false
}
