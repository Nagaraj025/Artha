package com.subramanya.artha.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.ui.graphics.vector.ImageVector
import com.subramanya.artha.R

/**
 * Top-level navigation destinations. `More` is a bottom-nav target but its tap intent
 * is to OPEN the More sheet rather than to navigate — MainActivity routes that intent
 * accordingly.
 */
enum class ArthaDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Dashboard(route = "dashboard", labelRes = R.string.nav_dashboard, icon = Icons.Filled.Dashboard),
    Transactions(route = "transactions", labelRes = R.string.nav_transactions, icon = Icons.AutoMirrored.Filled.ReceiptLong),
    Accounts(route = "accounts", labelRes = R.string.nav_accounts, icon = Icons.Filled.AccountBalance),
    Cards(route = "cards", labelRes = R.string.nav_cards, icon = Icons.Filled.CreditCard),
    Review(route = "review", labelRes = R.string.nav_review, icon = Icons.Filled.PendingActions),
    More(route = "more", labelRes = R.string.nav_more, icon = Icons.Filled.MoreHoriz),
    ;

    companion object {
        /** Destinations to show in the bottom nav, in display order. */
        val bottomNav: List<ArthaDestination> = entries.toList()

        fun fromRoute(route: String?): ArthaDestination? = entries.firstOrNull { it.route == route }
    }
}
