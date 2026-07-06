package com.subramanya.artha.data.db.seed

import com.subramanya.artha.data.entity.CategoryEntity
import com.subramanya.artha.data.entity.enums.CategoryType

/**
 * Stable, deterministic IDs (no UUIDs for seeded rows) so future versions of the app
 * can reference these by id — Rules Engine pre-seeded rules, default budgets, etc.
 *
 * Mirrors PRD §9 (Categories — Pre-Seeded) exactly. Every row has isSystem = true.
 *
 * Color is a Long ARGB value; Material 3 will tint properly. Icon strings are
 * Material icon names (extended icons enabled in build.gradle).
 */
internal object SeedCategories {

    private const val EXPENSE_COLOR: Long = 0xFFEF4444 // red-500
    private const val INCOME_COLOR: Long = 0xFF10B981 // emerald-500
    private const val TRANSFER_COLOR: Long = 0xFF6366F1 // indigo-500
    private const val INVESTMENT_COLOR: Long = 0xFFF59E0B // amber-500

    private data class ParentSpec(
        val id: String,
        val name: String,
        val icon: String,
        val children: List<Pair<String, String>>, // id-suffix to displayName
    )

    private val EXPENSE_PARENTS: List<ParentSpec> =
        listOf(
            ParentSpec(
                "cat_food_drink",
                "Food & Drink",
                "restaurant",
                listOf(
                    "groceries" to "Groceries",
                    "restaurants" to "Restaurants",
                    "cafes_coffee" to "Cafés & Coffee",
                    "food_delivery" to "Food Delivery",
                    "snacks" to "Snacks",
                    "office_lunch" to "Office Lunch",
                ),
            ),
            ParentSpec(
                "cat_transport",
                "Transport",
                "directions_car",
                listOf(
                    "fuel" to "Fuel",
                    "public_transit" to "Public Transit",
                    "ride_hailing" to "Ride-hailing",
                    "auto_taxi" to "Auto/Taxi",
                    "parking" to "Parking",
                    "tolls" to "Tolls (FASTag)",
                    "vehicle_maintenance" to "Vehicle Maintenance",
                ),
            ),
            ParentSpec(
                "cat_bills_utilities",
                "Bills & Utilities",
                "receipt_long",
                listOf(
                    "mobile" to "Mobile",
                    "internet" to "Internet",
                    "electricity" to "Electricity",
                    "water" to "Water",
                    "gas" to "Gas",
                    "dth_cable" to "DTH/Cable",
                    "maintenance_society" to "Maintenance/Society",
                ),
            ),
            ParentSpec(
                "cat_shopping",
                "Shopping",
                "shopping_bag",
                listOf(
                    "clothing" to "Clothing",
                    "electronics" to "Electronics",
                    "home_goods" to "Home Goods",
                    "personal_care" to "Personal Care",
                    "books_stationery" to "Books & Stationery",
                    "gifts" to "Gifts",
                ),
            ),
            ParentSpec(
                "cat_health",
                "Health",
                "medical_services",
                listOf(
                    "doctor" to "Doctor",
                    "pharmacy" to "Pharmacy",
                    "hospital" to "Hospital",
                    "diagnostics" to "Diagnostics",
                    "gym_fitness" to "Gym & Fitness",
                    "dental" to "Dental",
                    "eye_care" to "Eye Care",
                ),
            ),
            ParentSpec(
                "cat_entertainment",
                "Entertainment",
                "movie",
                listOf(
                    "movies" to "Movies",
                    "events_concerts" to "Events & Concerts",
                    "streaming" to "Streaming",
                    "games" to "Games",
                    "hobbies" to "Hobbies",
                ),
            ),
            ParentSpec(
                "cat_travel",
                "Travel",
                "flight",
                listOf(
                    "flights" to "Flights",
                    "trains" to "Trains",
                    "buses" to "Buses",
                    "hotels" to "Hotels",
                    "local_transport" to "Local Transport",
                    "vacation" to "Vacation",
                    "travel_insurance" to "Travel Insurance",
                ),
            ),
            ParentSpec(
                "cat_home",
                "Home",
                "home",
                listOf(
                    "rent" to "Rent",
                    "home_maintenance" to "Home Maintenance",
                    "repairs" to "Repairs",
                    "furniture" to "Furniture",
                    "appliances" to "Appliances",
                    "domestic_help" to "Domestic Help",
                ),
            ),
            ParentSpec(
                "cat_family",
                "Family",
                "family_restroom",
                listOf(
                    "money_to_parents" to "Money to Parents",
                    "money_to_spouse" to "Money to Spouse",
                    "money_to_children" to "Money to Children",
                    "family_gifts" to "Family Gifts",
                    "family_events" to "Family Events",
                ),
            ),
            ParentSpec(
                "cat_friends",
                "Friends",
                "group",
                listOf(
                    "outings" to "Outings",
                    "friend_gifts" to "Gifts",
                    "lending" to "Lending",
                ),
            ),
            ParentSpec(
                "cat_religious_spiritual",
                "Religious & Spiritual",
                "self_improvement",
                listOf(
                    "temple_donations" to "Temple Donations",
                    "sevas_pujas" to "Sevas & Pujas",
                    "prasadam" to "Prasadam",
                    "pilgrimage" to "Pilgrimage",
                    "religious_books_items" to "Religious Books/Items",
                    "charity_religious_orgs" to "Charity to Religious Orgs",
                ),
            ),
            ParentSpec(
                "cat_festivals",
                "Festivals",
                "celebration",
                listOf(
                    "diwali" to "Diwali",
                    "holi" to "Holi",
                    "ganesh_chaturthi" to "Ganesh Chaturthi",
                    "eid" to "Eid",
                    "christmas" to "Christmas",
                    "other_festivals" to "Other Festivals",
                    "festival_gifts" to "Festival Gifts",
                    "sweets_snacks" to "Sweets & Snacks",
                ),
            ),
            ParentSpec(
                "cat_education",
                "Education",
                "school",
                listOf(
                    "courses" to "Courses",
                    "education_books" to "Books",
                    "tuition" to "Tuition",
                    "certifications" to "Certifications",
                    "online_learning" to "Online Learning",
                ),
            ),
            ParentSpec(
                "cat_personal_care",
                "Personal Care",
                "spa",
                listOf(
                    "salon_barber" to "Salon/Barber",
                    "spa" to "Spa",
                    "cosmetics" to "Cosmetics",
                    "accessories" to "Accessories",
                ),
            ),
            ParentSpec(
                "cat_charity_donations",
                "Charity & Donations",
                "volunteer_activism",
                listOf(
                    "ngo" to "NGO",
                    "crowdfunding" to "Crowdfunding",
                    "disaster_relief" to "Disaster Relief",
                    "other_charity" to "Other",
                ),
            ),
            ParentSpec(
                "cat_fees_charges",
                "Fees & Charges",
                "request_quote",
                listOf(
                    "bank_fees" to "Bank Fees",
                    "card_fees" to "Card Fees",
                    "late_payment" to "Late Payment",
                    "government_fees" to "Government Fees",
                    "legal_ca_fees" to "Legal/CA Fees",
                ),
            ),
            ParentSpec(
                "cat_taxes",
                "Taxes",
                "account_balance",
                listOf(
                    "income_tax" to "Income Tax",
                    "advance_tax" to "Advance Tax",
                    "property_tax" to "Property Tax",
                    "gst" to "GST (business)",
                ),
            ),
            ParentSpec(
                "cat_loan_emi",
                "Loan EMI",
                "credit_score",
                listOf(
                    "home_loan" to "Home Loan",
                    "personal_loan" to "Personal Loan",
                    "vehicle_loan" to "Vehicle Loan",
                    "education_loan" to "Education Loan",
                    "credit_card_late" to "Credit Card Late",
                ),
            ),
            ParentSpec(
                "cat_insurance_premium",
                "Insurance Premium",
                "shield",
                listOf(
                    "health_premium" to "Health",
                    "vehicle_premium" to "Vehicle",
                    "term_life_premium" to "Term Life",
                    "travel_premium" to "Travel",
                    "home_premium" to "Home",
                ),
            ),
            ParentSpec(
                "cat_pets",
                "Pets",
                "pets",
                listOf(
                    "pet_food" to "Food",
                    "vet" to "Vet",
                    "grooming" to "Grooming",
                    "pet_accessories" to "Accessories",
                ),
            ),
            ParentSpec(
                "cat_miscellaneous",
                "Miscellaneous",
                "more_horiz",
                listOf("other" to "Other"),
            ),
        )

    private val INCOME_PARENTS: List<ParentSpec> =
        listOf(
            ParentSpec(
                "cat_salary",
                "Salary",
                "payments",
                listOf(
                    "base" to "Base",
                    "bonus" to "Bonus",
                    "variable" to "Variable",
                    "reimbursement" to "Reimbursement",
                ),
            ),
            ParentSpec("cat_freelance", "Freelance / Business", "work", emptyList()),
            ParentSpec(
                "cat_interest",
                "Interest",
                "savings",
                listOf(
                    "savings_acct" to "Savings Account",
                    "fd_interest" to "FD",
                    "rd_interest" to "RD",
                    "bonds_interest" to "Bonds",
                ),
            ),
            ParentSpec("cat_dividends", "Dividends", "trending_up", emptyList()),
            ParentSpec("cat_rental_income", "Rental Income", "apartment", emptyList()),
            ParentSpec(
                "cat_capital_gains",
                "Capital Gains",
                "show_chart",
                listOf(
                    "mf_gains" to "Mutual Funds",
                    "stock_gains" to "Stocks",
                    "gold_gains" to "Gold",
                ),
            ),
            ParentSpec(
                "cat_refunds",
                "Refunds",
                "undo",
                listOf(
                    "purchase_refund" to "Purchase Refund",
                    "tax_refund" to "Tax Refund",
                ),
            ),
            ParentSpec(
                "cat_cashback_rewards",
                "Cashback & Rewards",
                "redeem",
                listOf(
                    "card_cashback" to "Credit Card Cashback",
                    "upi_rewards" to "UPI Rewards",
                    "cred_coins" to "CRED Coins",
                ),
            ),
            ParentSpec("cat_gifts_received", "Gifts Received", "card_giftcard", emptyList()),
            ParentSpec("cat_money_from_family", "Money from Family", "diversity_3", emptyList()),
            ParentSpec("cat_other_income", "Other Income", "more_horiz", emptyList()),
        )

    private val INVESTMENT_PARENTS: List<ParentSpec> =
        listOf(
            ParentSpec("cat_sip_contribution", "SIP Contribution", "savings", emptyList()),
            ParentSpec("cat_fd_booking", "FD Booking", "lock", emptyList()),
            ParentSpec("cat_rd_installment", "RD Installment", "schedule", emptyList()),
            ParentSpec("cat_mf_purchase", "MF Purchase", "show_chart", emptyList()),
            ParentSpec("cat_stock_purchase", "Stock Purchase", "trending_up", emptyList()),
            ParentSpec("cat_gold_purchase", "Gold Purchase", "star", emptyList()),
            ParentSpec("cat_ppf_deposit", "PPF Deposit", "account_balance", emptyList()),
            ParentSpec("cat_nps_contribution", "NPS Contribution", "account_balance", emptyList()),
            ParentSpec("cat_ulip_endowment_premium", "ULIP/Endowment Premium", "shield", emptyList()),
            ParentSpec("cat_bond_purchase", "Bond Purchase", "description", emptyList()),
        )

    private val TRANSFER_PARENTS: List<ParentSpec> =
        listOf(
            ParentSpec("cat_between_my_accounts", "Between My Accounts", "swap_horiz", emptyList()),
            ParentSpec("cat_credit_card_payment", "Credit Card Payment", "credit_card", emptyList()),
            ParentSpec("cat_cash_withdrawal", "Cash Withdrawal", "atm", emptyList()),
            ParentSpec("cat_cash_deposit", "Cash Deposit", "savings", emptyList()),
        )

    fun all(): List<CategoryEntity> {
        val out = ArrayList<CategoryEntity>(256)
        appendGroup(out, EXPENSE_PARENTS, CategoryType.EXPENSE, EXPENSE_COLOR, startOrder = 0)
        appendGroup(out, INCOME_PARENTS, CategoryType.INCOME, INCOME_COLOR, startOrder = 1000)
        appendGroup(out, INVESTMENT_PARENTS, CategoryType.INVESTMENT, INVESTMENT_COLOR, startOrder = 2000)
        appendGroup(out, TRANSFER_PARENTS, CategoryType.TRANSFER, TRANSFER_COLOR, startOrder = 3000)
        return out
    }

    private fun appendGroup(
        sink: MutableList<CategoryEntity>,
        parents: List<ParentSpec>,
        type: CategoryType,
        color: Long,
        startOrder: Int,
    ) {
        var parentOrder = startOrder
        for (parent in parents) {
            sink.add(
                CategoryEntity(
                    id = parent.id,
                    name = parent.name,
                    parentId = null,
                    type = type,
                    icon = parent.icon,
                    color = color,
                    isSystem = true,
                    displayOrder = parentOrder,
                ),
            )
            var childOrder = 0
            for ((suffix, childName) in parent.children) {
                sink.add(
                    CategoryEntity(
                        id = "${parent.id}_$suffix",
                        name = childName,
                        parentId = parent.id,
                        type = type,
                        icon = parent.icon,
                        color = color,
                        isSystem = true,
                        displayOrder = childOrder,
                    ),
                )
                childOrder += 1
            }
            parentOrder += 1
        }
    }
}
