package com.subramanya.artha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subramanya.artha.data.importing.BankImporter
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.ThemeMode
import com.subramanya.artha.ui.common.ArthaBottomBar
import com.subramanya.artha.ui.common.ArthaTopBar
import com.subramanya.artha.ui.lock.BiometricLockGate
import com.subramanya.artha.ui.more.MoreAction
import com.subramanya.artha.ui.more.MoreSheet
import com.subramanya.artha.ui.navigation.ArthaDestination
import com.subramanya.artha.ui.navigation.ArthaNavHost
import com.subramanya.artha.ui.navigation.SubRoutes
import com.subramanya.artha.ui.onboarding.OnboardingFlow
import com.subramanya.artha.ui.onboarding.OnboardingViewModel
import com.subramanya.artha.ui.onboarding.OnboardingViewModelFactory
import com.subramanya.artha.ui.splash.SplashScreen
import com.subramanya.artha.ui.theme.ArthaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Switched to [FragmentActivity] in Phase 5 — BiometricPrompt requires a
 * FragmentActivity host. Behaviour is otherwise unchanged from ComponentActivity.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArthaRoot() }
    }
}

/**
 * Top-level state machine: Splash gates first launch on a real DB init (forces
 * Room schema + CategorySeeder to run) and on reading `userName` from DataStore.
 * Once ready, we show either Onboarding (first run) or MainApp (returning user).
 *
 * Theme mode + dynamic-color come from SettingsPreferences so Settings → Appearance
 * changes recompose immediately.
 */
private sealed interface StartupState {
    data object Loading : StartupState
    data object NeedsOnboarding : StartupState
    data class Ready(val userName: String) : StartupState
}

private const val MIN_SPLASH_MILLIS: Long = 500L

/** Bumped whenever the bundled bank-statement asset (or the schema it writes into)
 *  changes so existing installs re-run the importer after a destructive migration.
 *  Tracks AppDatabase.version for now. */
private const val CURRENT_BUNDLED_IMPORT_VERSION: Int = 3

@Composable
private fun ArthaRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val themeMode by app.settingsPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val useDynamicColor by app.settingsPreferences.useDynamicColor.collectAsState(initial = true)
    val biometricLock by app.settingsPreferences.biometricLockEnabled.collectAsState(initial = false)

    // If the user revokes RECEIVE_SMS from system Settings while auto-import is still
    // flagged on, flip the DataStore flag back off next time the app resumes — otherwise
    // the Settings toggle would keep showing "on" for a listener that can no longer fire.
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val enabled = app.settingsPreferences.smsAutoImportEnabled.first()
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECEIVE_SMS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (enabled && !granted) {
                        app.settingsPreferences.setSmsAutoImportEnabled(false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ArthaTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        // Biometric gate wraps the whole inner scope when enabled.
        if (biometricLock) {
            BiometricLockGate { ArthaInner(app) }
        } else {
            ArthaInner(app)
        }
    }
}

@Composable
private fun ArthaInner(app: ArthaApplication) {
    // Triggers DB init + reads userName once; emits a Ready/NeedsOnboarding terminal state.
    // Also runs the bundled bank-statement importer when its tracked version is older
    // than [CURRENT_BUNDLED_IMPORT_VERSION] — that covers fresh installs AND post-upgrade
    // re-runs when a schema bump destroys the old Room DB.
    val startup by produceState<StartupState>(initialValue = StartupState.Loading, app) {
        value = withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            app.database.categoryDao().count()
            if (app.settingsPreferences.bundledImportVersion.first() < CURRENT_BUNDLED_IMPORT_VERSION) {
                runCatching { BankImporter(app, app.database).importBundled() }
                app.settingsPreferences.setBundledImportVersion(CURRENT_BUNDLED_IMPORT_VERSION)
            }
            val name = app.settingsPreferences.userName.first()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_SPLASH_MILLIS) delay(MIN_SPLASH_MILLIS - elapsed)
            if (name.isBlank()) StartupState.NeedsOnboarding else StartupState.Ready(name)
        }
    }

    // Local override so completing onboarding can transition us into MainApp without
    // re-running the produceState block.
    var override: StartupState? by remember { mutableStateOf(null) }
    val current = override ?: startup

    when (val state = current) {
        StartupState.Loading -> SplashScreen()
        StartupState.NeedsOnboarding -> {
            val vm: OnboardingViewModel = viewModel(
                factory = OnboardingViewModelFactory(app.accountRepository, app.settingsPreferences),
            )
            OnboardingFlow(
                viewModel = vm,
                onCompleted = {
                    val saved = vm.state.value.name.trim()
                    override = StartupState.Ready(saved)
                },
            )
        }
        is StartupState.Ready -> MainApp(
            settingsPreferences = app.settingsPreferences,
            initialName = state.userName,
        )
    }
}

@Composable
private fun MainApp(
    settingsPreferences: SettingsPreferences,
    initialName: String,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = ArthaDestination.fromRoute(backStackEntry?.destination?.route)

    val userName by settingsPreferences.userName.collectAsState(initial = initialName)
    var showMoreSheet by remember { mutableStateOf(false) }

    // Show the global greeting top-bar only on the five bottom-nav destinations.
    // Sub-routes (Settings/Categories/Tags/About/details) bring their own TopAppBar
    // with a back button; stacking two would leave a giant gap above their title.
    val isBottomNavRoute = currentDestination != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Insets are handled by each screen's own chrome — ArthaTopBar uses
        // statusBarsPadding, ArthaBottomBar uses navigationBarsPadding, and
        // sub-screens with their own Scaffold/TopAppBar inset there. Setting
        // this to WindowInsets(0) avoids double-padding when a sub-screen's
        // inner Scaffold inserts its own status-bar inset on top of ours.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            if (isBottomNavRoute) {
                ArthaTopBar(
                    userName = userName,
                    onSearchClick = { navController.navigate(SubRoutes.SEARCH) },
                )
            }
        },
        bottomBar = {
            ArthaBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    if (destination == ArthaDestination.More) {
                        showMoreSheet = true
                    } else {
                        // Always navigate (don't skip when "already selected") — that lets the
                        // user tap Dashboard from anywhere to come home cleanly. Skipping
                        // saveState/restoreState avoids the bug where revisiting Cards (or any
                        // tab whose stack contained a sub-route at exit time) re-restores the
                        // sub-route and makes the bottom-nav pill flicker off.
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        ArthaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showMoreSheet) {
        MoreSheet(
            onDismiss = { showMoreSheet = false },
            onActionSelected = { action ->
                showMoreSheet = false
                navigateForMoreAction(navController, action)
            },
        )
    }
}

/** Maps a More-drawer tile selection to a navigation push. */
private fun navigateForMoreAction(navController: NavHostController, action: MoreAction) {
    val route = when (action) {
        MoreAction.Categories -> SubRoutes.CATEGORIES
        MoreAction.Tags -> SubRoutes.TAGS
        MoreAction.Settings -> SubRoutes.SETTINGS
        MoreAction.About -> SubRoutes.ABOUT
        MoreAction.Investments -> SubRoutes.INVESTMENTS
        MoreAction.Insurance -> SubRoutes.INSURANCES
        MoreAction.Rules -> SubRoutes.RULES
        MoreAction.People -> SubRoutes.PEOPLE
        MoreAction.Budgets -> SubRoutes.BUDGETS
        MoreAction.Goals -> SubRoutes.GOALS
        MoreAction.Subscriptions -> SubRoutes.SUBSCRIPTIONS
        MoreAction.Recurring -> SubRoutes.RECURRING
        MoreAction.Reports -> SubRoutes.REPORTS
    }
    navController.navigate(route) { launchSingleTop = true }
}
