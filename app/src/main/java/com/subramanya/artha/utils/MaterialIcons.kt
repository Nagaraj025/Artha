package com.subramanya.artha.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the Material icon name strings stored in [com.subramanya.artha.data.entity.CategoryEntity.icon]
 * to actual Compose [ImageVector]s. The seed catalogue (`SeedCategories`) hands out
 * about thirty distinct names; unknown values fall back to a generic Category icon so
 * user-created categories with arbitrary strings still render.
 *
 * Keeping the lookup in one place means the seed data, the Category form sheet, and
 * the various avatar renderers (transactions list, categories list, picker sheet)
 * all stay in sync — change a glyph here and the whole app picks it up.
 */
object MaterialIcons {

    fun resolve(name: String?): ImageVector {
        if (name.isNullOrBlank()) return Icons.Filled.Category
        return registry[name.lowercase().trim()] ?: Icons.Filled.Category
    }

    /** All known icon keys — the catalogue the "add icon" picker offers. */
    val keys: List<String> get() = registry.keys.toList()

    private val registry: Map<String, ImageVector> = mapOf(
        // Expense parents
        "restaurant" to Icons.Filled.Restaurant,
        "directions_car" to Icons.Filled.DirectionsCar,
        "receipt_long" to Icons.Filled.ReceiptLong,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "medical_services" to Icons.Filled.MedicalServices,
        "movie" to Icons.Filled.Movie,
        "flight" to Icons.Filled.Flight,
        "home" to Icons.Filled.Home,
        "family_restroom" to Icons.Filled.FamilyRestroom,
        "group" to Icons.Filled.Group,
        "self_improvement" to Icons.Filled.SelfImprovement,
        "celebration" to Icons.Filled.Celebration,
        "school" to Icons.Filled.School,
        "spa" to Icons.Filled.Spa,
        "volunteer_activism" to Icons.Filled.VolunteerActivism,
        "request_quote" to Icons.Filled.RequestQuote,
        "account_balance" to Icons.Filled.AccountBalance,
        "credit_score" to Icons.Filled.CreditScore,
        "shield" to Icons.Filled.Shield,
        "pets" to Icons.Filled.Pets,
        "more_horiz" to Icons.Filled.MoreHoriz,
        // Income parents
        "payments" to Icons.Filled.Payments,
        "work" to Icons.Filled.Work,
        "savings" to Icons.Filled.Savings,
        "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
        "apartment" to Icons.Filled.Apartment,
        "show_chart" to Icons.Filled.ShowChart,
        "undo" to Icons.Filled.Undo,
        "redeem" to Icons.Filled.Redeem,
        "card_giftcard" to Icons.Filled.CardGiftcard,
        "diversity_3" to Icons.Filled.Diversity3,
        // Investment parents
        "lock" to Icons.Filled.Lock,
        "schedule" to Icons.Filled.Schedule,
        "star" to Icons.Filled.Star,
        "description" to Icons.Filled.Description,
        // Transfer parents
        "swap_horiz" to Icons.Filled.SwapHoriz,
        "credit_card" to Icons.Filled.CreditCard,
        "atm" to Icons.Filled.Atm,
        // Catch-alls used by older seed rows / user picks
        "category" to Icons.Filled.Category,
    )
}
