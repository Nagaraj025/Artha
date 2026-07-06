package com.subramanya.artha.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.subramanya.artha.ui.accounts.AccountDetailScreen
import com.subramanya.artha.ui.accounts.AccountsScreen
import com.subramanya.artha.ui.cards.CardDetailScreen
import com.subramanya.artha.ui.cards.CardsScreen
import com.subramanya.artha.ui.categories.CategoriesScreen
import com.subramanya.artha.ui.dashboard.DashboardScreen
import com.subramanya.artha.ui.insurance.InsuranceDetailScreen
import com.subramanya.artha.ui.insurance.InsurancesScreen
import com.subramanya.artha.ui.budgets.BudgetsScreen
import com.subramanya.artha.ui.goals.GoalsScreen
import com.subramanya.artha.ui.investments.InvestmentDetailScreen
import com.subramanya.artha.ui.investments.InvestmentsScreen
import com.subramanya.artha.ui.people.PeopleScreen
import com.subramanya.artha.ui.recurring.RecurringScreen
import com.subramanya.artha.ui.reports.ReportsScreen
import com.subramanya.artha.ui.review.ReviewScreen
import com.subramanya.artha.ui.rules.RulesScreen
import com.subramanya.artha.ui.search.SearchScreen
import com.subramanya.artha.ui.settings.AboutScreen
import com.subramanya.artha.ui.subscriptions.SubscriptionsScreen
import com.subramanya.artha.ui.settings.SettingsScreen
import com.subramanya.artha.ui.tags.TagsScreen
import com.subramanya.artha.ui.transactions.TransactionDetailScreen
import com.subramanya.artha.ui.transactions.TransactionsScreen

/** Sub-routes outside the bottom-nav tabs. Detail screens, More-drawer destinations land here. */
object SubRoutes {
    private const val ACCOUNT_DETAIL_BASE = "account_detail"
    const val ACCOUNT_DETAIL_ARG_ID = "accountId"
    const val ACCOUNT_DETAIL_PATTERN = "$ACCOUNT_DETAIL_BASE/{$ACCOUNT_DETAIL_ARG_ID}"
    fun accountDetail(accountId: String): String = "$ACCOUNT_DETAIL_BASE/$accountId"

    private const val CARD_DETAIL_BASE = "card_detail"
    const val CARD_DETAIL_ARG_ID = "cardId"
    const val CARD_DETAIL_PATTERN = "$CARD_DETAIL_BASE/{$CARD_DETAIL_ARG_ID}"
    fun cardDetail(cardId: String): String = "$CARD_DETAIL_BASE/$cardId"

    private const val TRANSACTION_DETAIL_BASE = "transaction_detail"
    const val TRANSACTION_DETAIL_ARG_ID = "transactionId"
    const val TRANSACTION_DETAIL_PATTERN = "$TRANSACTION_DETAIL_BASE/{$TRANSACTION_DETAIL_ARG_ID}"
    fun transactionDetail(transactionId: String): String = "$TRANSACTION_DETAIL_BASE/$transactionId"

    const val CATEGORIES = "categories"
    const val TAGS = "tags"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    // Phase 2
    const val INVESTMENTS = "investments"
    const val INSURANCES = "insurances"
    const val RULES = "rules"

    // Phase 4
    const val PEOPLE = "people"
    const val BUDGETS = "budgets"
    const val GOALS = "goals"
    const val SUBSCRIPTIONS = "subscriptions"
    const val RECURRING = "recurring"

    // Phase 5
    const val REPORTS = "reports"

    /** Global search — opened from the Dashboard header search icon. */
    const val SEARCH = "search"

    private const val INVESTMENT_DETAIL_BASE = "investment_detail"
    const val INVESTMENT_DETAIL_ARG_ID = "investmentId"
    const val INVESTMENT_DETAIL_PATTERN = "$INVESTMENT_DETAIL_BASE/{$INVESTMENT_DETAIL_ARG_ID}"
    fun investmentDetail(id: String): String = "$INVESTMENT_DETAIL_BASE/$id"

    private const val INSURANCE_DETAIL_BASE = "insurance_detail"
    const val INSURANCE_DETAIL_ARG_ID = "insuranceId"
    const val INSURANCE_DETAIL_PATTERN = "$INSURANCE_DETAIL_BASE/{$INSURANCE_DETAIL_ARG_ID}"
    fun insuranceDetail(id: String): String = "$INSURANCE_DETAIL_BASE/$id"

    private const val PERSON_DETAIL_BASE = "person_detail"
    const val PERSON_DETAIL_ARG_ID = "personId"
    const val PERSON_DETAIL_PATTERN = "$PERSON_DETAIL_BASE/{$PERSON_DETAIL_ARG_ID}"
    fun personDetail(id: String): String = "$PERSON_DETAIL_BASE/$id"
}

private const val NAV_ANIM_MS: Int = 220

@Composable
fun ArthaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ArthaDestination.Dashboard.route,
        modifier = modifier,
        // Push: slide new screen in from the right + fade. Pop: slide back to the right.
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_MS),
            ) + fadeIn(tween(NAV_ANIM_MS))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_MS),
            ) + fadeOut(tween(NAV_ANIM_MS))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_MS),
            ) + fadeIn(tween(NAV_ANIM_MS))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_MS),
            ) + fadeOut(tween(NAV_ANIM_MS))
        },
    ) {
        composable(ArthaDestination.Dashboard.route) {
            DashboardScreen(
                onOpenTransactions = {
                    navController.navigate(ArthaDestination.Transactions.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onOpenAccount = { id -> navController.navigate(SubRoutes.accountDetail(id)) },
                onOpenCard = { id -> navController.navigate(SubRoutes.cardDetail(id)) },
                onOpenTransaction = { id -> navController.navigate(SubRoutes.transactionDetail(id)) },
                onOpenInsurance = { id -> navController.navigate(SubRoutes.insuranceDetail(id)) },
                onAddAccount = {
                    navController.navigate(ArthaDestination.Accounts.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onAddCard = {
                    navController.navigate(ArthaDestination.Cards.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ArthaDestination.Transactions.route) {
            TransactionsScreen(
                onOpenTransaction = { id -> navController.navigate(SubRoutes.transactionDetail(id)) },
            )
        }
        composable(ArthaDestination.Accounts.route) {
            AccountsScreen(
                onOpenAccount = { id -> navController.navigate(SubRoutes.accountDetail(id)) },
            )
        }
        composable(ArthaDestination.Cards.route) {
            CardsScreen(
                onOpenCard = { id -> navController.navigate(SubRoutes.cardDetail(id)) },
            )
        }
        composable(ArthaDestination.Review.route) {
            ReviewScreen()
        }
        composable(
            route = SubRoutes.ACCOUNT_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.ACCOUNT_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.ACCOUNT_DETAIL_ARG_ID).orEmpty()
            AccountDetailScreen(
                accountId = id,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txnId -> navController.navigate(SubRoutes.transactionDetail(txnId)) },
            )
        }
        composable(
            route = SubRoutes.CARD_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.CARD_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.CARD_DETAIL_ARG_ID).orEmpty()
            CardDetailScreen(
                cardId = id,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txnId -> navController.navigate(SubRoutes.transactionDetail(txnId)) },
            )
        }
        composable(
            route = SubRoutes.TRANSACTION_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.TRANSACTION_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.TRANSACTION_DETAIL_ARG_ID).orEmpty()
            TransactionDetailScreen(transactionId = id, onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.CATEGORIES) {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.TAGS) {
            TagsScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(SubRoutes.ABOUT) },
            )
        }
        composable(SubRoutes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.INVESTMENTS) {
            InvestmentsScreen(
                onBack = { navController.popBackStack() },
                onOpenInvestment = { id -> navController.navigate(SubRoutes.investmentDetail(id)) },
            )
        }
        composable(
            route = SubRoutes.INVESTMENT_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.INVESTMENT_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.INVESTMENT_DETAIL_ARG_ID).orEmpty()
            InvestmentDetailScreen(
                investmentId = id,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txnId -> navController.navigate(SubRoutes.transactionDetail(txnId)) },
            )
        }
        composable(SubRoutes.INSURANCES) {
            InsurancesScreen(
                onBack = { navController.popBackStack() },
                onOpenInsurance = { id -> navController.navigate(SubRoutes.insuranceDetail(id)) },
            )
        }
        composable(
            route = SubRoutes.INSURANCE_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.INSURANCE_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.INSURANCE_DETAIL_ARG_ID).orEmpty()
            InsuranceDetailScreen(
                insuranceId = id,
                onBack = { navController.popBackStack() },
                onOpenInvestment = { invId -> navController.navigate(SubRoutes.investmentDetail(invId)) },
            )
        }
        composable(SubRoutes.RULES) {
            RulesScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.PEOPLE) {
            PeopleScreen(
                onBack = { navController.popBackStack() },
                onOpenPerson = { id -> navController.navigate(SubRoutes.personDetail(id)) },
            )
        }
        composable(
            route = SubRoutes.PERSON_DETAIL_PATTERN,
            arguments = listOf(navArgument(SubRoutes.PERSON_DETAIL_ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString(SubRoutes.PERSON_DETAIL_ARG_ID).orEmpty()
            com.subramanya.artha.ui.people.PersonDetailScreen(
                personId = id,
                onBack = { navController.popBackStack() },
                onOpenTransaction = { txnId -> navController.navigate(SubRoutes.transactionDetail(txnId)) },
            )
        }
        composable(SubRoutes.BUDGETS) {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.GOALS) {
            GoalsScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.SUBSCRIPTIONS) {
            SubscriptionsScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.RECURRING) {
            RecurringScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(SubRoutes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id -> navController.navigate(SubRoutes.transactionDetail(id)) },
                onOpenAccount = { id -> navController.navigate(SubRoutes.accountDetail(id)) },
                onOpenCard = { id -> navController.navigate(SubRoutes.cardDetail(id)) },
                onOpenInvestment = { id -> navController.navigate(SubRoutes.investmentDetail(id)) },
                onOpenInsurance = { id -> navController.navigate(SubRoutes.insuranceDetail(id)) },
                onOpenPeople = { navController.navigate(SubRoutes.PEOPLE) },
                onOpenCategories = { navController.navigate(SubRoutes.CATEGORIES) },
                onOpenTags = { navController.navigate(SubRoutes.TAGS) },
            )
        }
        // No `more` route on purpose — that tap opens a sheet, not a destination.
    }
}
