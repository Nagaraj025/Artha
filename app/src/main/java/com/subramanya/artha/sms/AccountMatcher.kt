package com.subramanya.artha.sms

import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.ui.transaction.FundsEndpoint

/** Result of matching an SMS account hint against the user's accounts/cards. */
sealed interface AccountMatch {
    data class Matched(val funds: FundsEndpoint) : AccountMatch
    data object NoMatch : AccountMatch
}

/**
 * Resolves an SMS-extracted account hint (last 3–6 digits) to exactly one of the user's
 * accounts/cards. Pure — no Android/Room dependency. Suffix match (not exact) because banks
 * report differing digit counts for the same underlying account, and stored last-digits and the
 * hint can be different lengths. Ambiguity (2+ candidates) deliberately yields [NoMatch] rather
 * than guessing — see docs/plans/2026-07-06-sms-account-matching-design.md, Decision 2.
 */
object AccountMatcher {

    fun match(hint: String?, accounts: List<Account>, cards: List<Card>): AccountMatch {
        val cleanHint = hint?.trim().orEmpty()
        if (cleanHint.isBlank()) return AccountMatch.NoMatch

        val candidates = buildList {
            accounts.forEach { account ->
                if (suffixMatches(cleanHint, account.accountNumberLast4)) {
                    add(FundsEndpoint(kind = SourceKind.ACCOUNT, id = account.id, displayName = account.name))
                }
            }
            cards.forEach { card ->
                if (suffixMatches(cleanHint, card.cardNumberLast4)) {
                    add(
                        FundsEndpoint(
                            kind = SourceKind.CARD,
                            id = card.id,
                            displayName = card.name,
                            isCreditCard = card.type == CardType.CREDIT,
                        ),
                    )
                }
            }
        }

        return if (candidates.size == 1) AccountMatch.Matched(candidates.first()) else AccountMatch.NoMatch
    }

    private fun suffixMatches(hint: String, stored: String?): Boolean {
        val cleanStored = stored?.trim().orEmpty()
        if (cleanStored.isBlank()) return false
        return hint.endsWith(cleanStored) || cleanStored.endsWith(hint)
    }
}
