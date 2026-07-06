# SMS Account Matching + Compulsory Last-Digits Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** auto-resolve which of the user's accounts/cards a Review-tab SMS draft belongs to (by matching the last digits the parser already extracts), make last-digits compulsory when creating an account/card, nudge existing accounts/cards missing digits, and let the user add a new account/card inline when an SMS matches nothing.

**Architecture:** a new pure-Kotlin `AccountMatcher` (suffix-match, no Android deps) feeds `ReviewViewModel`, which now combines pending items with live account/card lists to attach a `matchedFunds` endpoint (or an unmatched flag) per item. `AddTransactionViewModel.applyPendingSmsPrefill` gains a matched-source parameter so the Add Transaction sheet opens pre-selected. Form sheets make last-digits required (3–6 digits) and accept a prefill value; list screens show an "add last digits" badge on rows missing them; the Review screen shows an "add account" affordance for unmatched hints that opens the existing form sheets pre-filled.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (Material3 BOM 2024.10.01), MVVM + StateFlow, manual constructor injection (no DI framework). Branch `feat/sms-auto-detect`, worktree `d:/naags_work_space/android/.worktrees/sms-auto-detect`.

**Full design:** `docs/plans/2026-07-06-sms-account-matching-design.md` (read first — this plan implements every decision there).

## Global Constraints

- No `!!` operator anywhere. No `var` in data classes.
- All user-facing strings in `app/src/main/res/values/strings.xml` — no hardcoded strings in Composables.
- INR amounts render via `utils/IndianNumberFormat.kt`.
- One ViewModel per screen; ViewModels reach repositories via a hand-written `XyzViewModelFactory` (no OS-registered factory, no DI framework).
- `FundsEndpoint` is `data class FundsEndpoint(val kind: SourceKind, val id: String, val displayName: String, val isCreditCard: Boolean = false)` (in `ui/transaction/AddTransactionState.kt`). Build it from an Account as `FundsEndpoint(SourceKind.ACCOUNT, account.id, account.name)` and from a Card as `FundsEndpoint(SourceKind.CARD, card.id, card.name, isCreditCard = card.type == CardType.CREDIT)` — mirroring `AddTransactionViewModel.kt:93-105`.
- Last-digits fields: `Account.accountNumberLast4: String?`, `Card.cardNumberLast4: String?`. The SMS hint is `PendingSmsTransaction.accountHint: String?` (3–6 digits). Stored last-digits become 3–6 digits after this plan (were exactly 4).
- Env for Gradle: `JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"`, `ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"`.
- Run `./gradlew assembleDebug` after every task; run the relevant unit test after TDD tasks. No emulator/device required for the unit tests; UI wiring is verified by compile + manual on-device test at the end.
- After each task, per the standing instruction, the controller pushes the branch to the `fork` remote — implementers just commit.

---

## Task 1: `AccountMatcher` pure matcher (TDD)

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/sms/AccountMatcher.kt`
- Test: `app/src/test/java/com/subramanya/artha/sms/AccountMatcherTest.kt`

**Interfaces:**
- Consumes: `domain/model/Account.kt` (`Account`), `domain/model/Card.kt` (`Card`), `ui/transaction/AddTransactionState.kt` (`FundsEndpoint`), `data/entity/enums/SourceKind.kt`, `data/entity/enums/CardType.kt`.
- Produces: `sealed interface AccountMatch { data class Matched(val funds: FundsEndpoint) : AccountMatch; data object NoMatch : AccountMatch }` and `object AccountMatcher { fun match(hint: String?, accounts: List<Account>, cards: List<Card>): AccountMatch }`. Task 2 consumes both.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.subramanya.artha.sms

import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountMatcherTest {

    private fun account(id: String, name: String, last4: String?): Account = Account(
        id = id,
        name = name,
        type = AccountType.BANK_SAVINGS,
        institution = null,
        accountNumberLast4 = last4,
        openingBalance = 0.0,
        currency = "INR",
        icon = "account_balance",
        color = 0L,
        isArchived = false,
        displayOrder = 0,
        createdAt = 0L,
    )

    private fun card(id: String, name: String, last4: String?): Card = Card(
        id = id,
        name = name,
        type = CardType.CREDIT,
        issuer = null,
        network = CardNetwork.VISA,
        cardNumberLast4 = last4,
        creditLimit = 1000.0,
        statementDayOfMonth = 1,
        dueDayOfMonth = 15,
        linkedAccountId = null,
        icon = "credit_card",
        color = 0L,
        isArchived = false,
        displayOrder = 0,
        createdAt = 0L,
    )

    @Test
    fun `exact 4-digit match on an account returns Matched account endpoint`() {
        val result = AccountMatcher.match("1234", listOf(account("a1", "HDFC", "1234")), emptyList())
        assertTrue(result is AccountMatch.Matched)
        val funds = (result as AccountMatch.Matched).funds
        assertEquals(SourceKind.ACCOUNT, funds.kind)
        assertEquals("a1", funds.id)
        assertEquals("HDFC", funds.displayName)
    }

    @Test
    fun `suffix match works when the hint is longer than stored last4`() {
        // SMS hint "X7286" -> "7286"? No — hint is 6 digits here, stored is 4; stored is a suffix of hint.
        val result = AccountMatcher.match("567286", emptyList(), listOf(card("c1", "Amazon Pay", "7286")))
        assertTrue(result is AccountMatch.Matched)
        assertEquals("c1", (result as AccountMatch.Matched).funds.id)
        assertEquals(SourceKind.CARD, result.funds.kind)
        assertTrue(result.funds.isCreditCard)
    }

    @Test
    fun `suffix match works when stored last4 is longer than the hint`() {
        val result = AccountMatcher.match("234", listOf(account("a1", "SBI", "1234")), emptyList())
        assertTrue(result is AccountMatch.Matched)
        assertEquals("a1", (result as AccountMatch.Matched).funds.id)
    }

    @Test
    fun `null hint returns NoMatch`() {
        val result = AccountMatcher.match(null, listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `blank hint returns NoMatch`() {
        val result = AccountMatcher.match("   ", listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `no candidate with a matching suffix returns NoMatch`() {
        val result = AccountMatcher.match("9999", listOf(account("a1", "SBI", "1234")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `candidates with null or blank stored last4 never match`() {
        val result = AccountMatcher.match("1234", listOf(account("a1", "SBI", null), account("a2", "Axis", "")), emptyList())
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `two accounts sharing the same suffix are ambiguous and return NoMatch`() {
        val result = AccountMatcher.match(
            "1234",
            listOf(account("a1", "SBI", "1234"), account("a2", "HDFC", "1234")),
            emptyList(),
        )
        assertEquals(AccountMatch.NoMatch, result)
    }

    @Test
    fun `an account and a card sharing the same suffix are ambiguous and return NoMatch`() {
        val result = AccountMatcher.match(
            "1234",
            listOf(account("a1", "SBI", "1234")),
            listOf(card("c1", "HDFC CC", "1234")),
        )
        assertEquals(AccountMatch.NoMatch, result)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*AccountMatcherTest"`
Expected: FAIL to compile — `AccountMatcher`/`AccountMatch` don't exist.

- [ ] **Step 3: Implement `AccountMatcher`**

```kotlin
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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*AccountMatcherTest"`
Expected: `BUILD SUCCESSFUL`, 9 tests passed. If `AccountType.BANK_SAVINGS` / `CardNetwork.VISA` enum names differ, open `data/entity/enums/AccountType.kt` / `CardNetwork.kt` and use a real value — adjust the test helpers only, not the matcher.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/sms/AccountMatcher.kt app/src/test/java/com/subramanya/artha/sms/AccountMatcherTest.kt
git commit -m "feat(sms): add AccountMatcher to resolve SMS hint to an account/card"
```

---

## Task 2: Wire matching into the Review flow + Add Transaction prefill

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/review/ReviewViewModel.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/transaction/AddTransactionViewModel.kt:166-179`
- Modify: `app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt` (VM factory call + prefill call)

**Interfaces:**
- Consumes: `AccountMatcher.match(...)`, `AccountMatch` (Task 1); `AccountRepository.observeActive(): Flow<List<Account>>`, `CardRepository.observeActive(): Flow<List<Card>>` (existing).
- Produces: extended `ReviewItem(pending, suggestedCategoryName, matchedFunds: FundsEndpoint?, hasUnmatchedHint: Boolean)`; `AddTransactionViewModel.applyPendingSmsPrefill(pending, suggestedCategoryName, matchedFunds: FundsEndpoint?)`. Task 5 consumes both.

- [ ] **Step 1: Extend `ReviewItem` and add matching in `ReviewViewModel.kt`**

Replace the `ReviewItem` data class (line ~16) with:

```kotlin
/**
 * One pending SMS-detected transaction, its rule-suggested category name, and the account/card
 * its last-digits hint resolved to (if exactly one matched). `hasUnmatchedHint` is true when the
 * SMS carried an account hint but nothing matched it — the Review card offers to add it.
 */
data class ReviewItem(
    val pending: PendingSmsTransaction,
    val suggestedCategoryName: String?,
    val matchedFunds: FundsEndpoint? = null,
    val hasUnmatchedHint: Boolean = false,
)
```

Add imports at the top of the file:
```kotlin
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.sms.AccountMatch
import com.subramanya.artha.sms.AccountMatcher
import com.subramanya.artha.ui.transaction.FundsEndpoint
import kotlinx.coroutines.flow.combine
```

Change the `ReviewViewModel` constructor to inject the two repositories:
```kotlin
class ReviewViewModel(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
) : ViewModel() {
```

Replace the `state` chain (`.map { ... }.stateIn(...)`) with a `combine` over pending items + live accounts + cards:
```kotlin
    val state: StateFlow<ReviewUiState> = combine(
        pendingTransactionRepository.observeAll(),
        accountRepository.observeActive(),
        cardRepository.observeActive(),
    ) { pendingList, accounts, cards ->
        ReviewUiState(
            items = pendingList.map { pending ->
                val categoryName = pending.suggestedCategoryId
                    ?.let { categoryRepository.getById(it)?.name }
                val match = AccountMatcher.match(pending.accountHint, accounts, cards)
                val matchedFunds = (match as? AccountMatch.Matched)?.funds
                val hasUnmatchedHint = !pending.accountHint.isNullOrBlank() && matchedFunds == null
                ReviewItem(
                    pending = pending,
                    suggestedCategoryName = categoryName,
                    matchedFunds = matchedFunds,
                    hasUnmatchedHint = hasUnmatchedHint,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())
```

Update `ReviewViewModelFactory` to take and pass the two new repositories:
```kotlin
class ReviewViewModelFactory(
    private val pendingTransactionRepository: PendingTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ReviewViewModel(
            pendingTransactionRepository,
            categoryRepository,
            accountRepository,
            cardRepository,
        ) as T
    }
}
```

- [ ] **Step 2: Extend `applyPendingSmsPrefill` in `AddTransactionViewModel.kt:166-179`**

Replace the function with (adds a `matchedFunds` param that seeds `source`; note for INCOME the received account is conceptually the destination in this app's model, but the existing prefill sets neither today — set `source` for EXPENSE-tab and `destination` for INCOME-tab so the pre-selected account lands in the field the sheet actually shows):

```kotlin
    fun applyPendingSmsPrefill(
        pending: com.subramanya.artha.domain.model.PendingSmsTransaction,
        suggestedCategoryName: String?,
        matchedFunds: FundsEndpoint? = null,
    ) {
        val isDebit = pending.direction == com.subramanya.artha.domain.model.SmsDirection.DEBIT
        _state.value = AddTransactionUiState(
            tab = if (isDebit) TransactionTab.EXPENSE else TransactionTab.INCOME,
            amountText = pending.amount.toString(),
            description = pending.merchant ?: pending.sender,
            dateTimeMillis = pending.receivedAt,
            categoryId = pending.suggestedCategoryId,
            categoryDisplay = suggestedCategoryName,
            source = if (isDebit) matchedFunds else null,
            destination = if (isDebit) null else matchedFunds,
        )
    }
```

Confirm `FundsEndpoint` is imported in `AddTransactionViewModel.kt` (it is used pervasively already; if not, add `import com.subramanya.artha.ui.transaction.FundsEndpoint` — but since the VM is in the same package `ui.transaction`, no import is needed).

- [ ] **Step 3: Update `ReviewScreen.kt` to pass the new factory args and the matched funds**

In `ReviewScreen.kt`, find the `ReviewViewModelFactory(app.pendingTransactionRepository, app.categoryRepository)` call (line ~53) and change it to:
```kotlin
        factory = ReviewViewModelFactory(
            app.pendingTransactionRepository,
            app.categoryRepository,
            app.accountRepository,
            app.cardRepository,
        ),
```

Find the prefill `LaunchedEffect` (line ~76-80) and pass the matched funds:
```kotlin
        pendingPrefill?.let { item ->
            LaunchedEffect(item) {
                txnVm.applyPendingSmsPrefill(item.pending, item.suggestedCategoryName, item.matchedFunds)
            }
        }
```

- [ ] **Step 4: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. (If `AccountRepository.observeActive()` doesn't exist, use `observeAll()` — check `AccountRepository.kt`; recon confirms both exist, prefer `observeActive()` to exclude archived accounts from matching.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/review/ReviewViewModel.kt \
        app/src/main/java/com/subramanya/artha/ui/transaction/AddTransactionViewModel.kt \
        app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt
git commit -m "feat(sms): match Review items to accounts and pre-select the source on prefill"
```

---

## Task 3: Compulsory 3–6 digit last-digits + prefill param in form sheets

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/accounts/AccountFormSheet.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/cards/CardFormSheet.kt`
- Modify: `app/src/main/res/values/strings.xml` (validation copy, if the existing message says "4 digits")

**Interfaces:**
- Produces: `AccountFormSheet(editing: Account?, onDismiss: () -> Unit, prefillLast4: String? = null)` and `CardFormSheet(editing: Card?, onDismiss: () -> Unit, prefillLast4: String? = null)`. Task 5 consumes both.

- [ ] **Step 1: Make last-digits required (3–6 digits) + accept prefill in `AccountFormSheet.kt`**

Change the composable signature (line ~61):
```kotlin
@Composable
fun AccountFormSheet(editing: Account?, onDismiss: () -> Unit, prefillLast4: String? = null) {
```

Change the `last4` state var (line ~70) to seed from the prefill when creating:
```kotlin
    var last4 by remember(editing) {
        mutableStateOf(editing?.accountNumberLast4 ?: prefillLast4.orEmpty())
    }
```

Change `last4Valid` (line ~81) from optional to required 3–6 digits:
```kotlin
    val last4Valid = last4.length in 3..6 && last4.all { it.isDigit() }
```

Change the `.take(4)` cap to `.take(6)` in the field's `onValueChange` (line ~155):
```kotlin
        onValueChange = { v -> last4 = v.filter { it.isDigit() }.take(6) },
```

Remove `optional = true` from the `FieldRow` for last4 (line ~150) so it reads:
```kotlin
    FieldRow(
        label = stringResource(R.string.account_form_last4_label),
    ) {
```

Change the save write (line ~214) from the exactly-4 filter to the 3–6 range:
```kotlin
                accountNumberLast4 = last4.takeIf { it.length in 3..6 },
```

- [ ] **Step 2: Update the validation string if it mentions "4 digits"**

Open `app/src/main/res/values/strings.xml`, find `account_form_validation_last4`. If it says "4 digits", change the text to "Enter the last 3–6 digits". Do the same for any `card_form_validation_last4` if present. (Leave the string keys unchanged.)

- [ ] **Step 3: Make last-digits required (3–6 digits) + accept prefill in `CardFormSheet.kt`**

Change the composable signature (line ~61):
```kotlin
@Composable
fun CardFormSheet(editing: Card?, onDismiss: () -> Unit, prefillLast4: String? = null) {
```

Change the `last4` state var (line ~74):
```kotlin
    var last4 by remember(editing) {
        mutableStateOf(editing?.cardNumberLast4 ?: prefillLast4.orEmpty())
    }
```

Change `last4Valid` (line ~91):
```kotlin
    val last4Valid = last4.length in 3..6 && last4.all { it.isDigit() }
```

Change the `.take(4)` cap to `.take(6)` (line ~177):
```kotlin
        onValueChange = { v -> last4 = v.filter { it.isDigit() }.take(6) },
```

Remove `optional = true` from the card's last4 `FieldRow` (line ~170):
```kotlin
    FieldRow(
        label = stringResource(R.string.card_form_last4_label),
    ) {
```

Change the save write (line ~277):
```kotlin
            cardNumberLast4 = last4.takeIf { it.length in 3..6 },
```

- [ ] **Step 4: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/accounts/AccountFormSheet.kt \
        app/src/main/java/com/subramanya/artha/ui/cards/CardFormSheet.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(accounts): require 3-6 digit last-digits and accept a prefill value"
```

---

## Task 4: "Add last digits" backfill nudge on list rows

**Files:**
- Modify: `app/src/main/java/com/subramanya/artha/ui/accounts/AccountsScreen.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/cards/CardsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` (new badge string)

**Interfaces:**
- Consumes: existing `FormMode.Edit(account/card)` sheet-open mechanism in both screens.
- Produces: no new symbol other consumers rely on (leaf UI change).

- [ ] **Step 1: Add the badge string**

Add to `app/src/main/res/values/strings.xml` (near the other account/card strings):
```xml
    <string name="account_add_last_digits_badge">Add last digits</string>
```

- [ ] **Step 2: Show the badge on account rows missing last-digits in `AccountsScreen.kt`**

In `ActiveAccountRow`'s content `Column` (line ~246-263, where the name + subtitle render), after the subtitle, add a conditional badge. The row already receives `onEdit: () -> Unit` (wired to `formMode = FormMode.Edit(row.account)` at the call site). Add a small clickable pill when `row.account.accountNumberLast4.isNullOrBlank()`:

```kotlin
                if (row.account.accountNumberLast4.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_add_last_digits_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onEdit),
                    )
                }
```

Ensure `androidx.compose.foundation.clickable`, `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.height`, and `androidx.compose.ui.unit.dp` are imported (most already are — add any missing).

- [ ] **Step 3: Show the badge on card rows missing last-digits in `CardsScreen.kt`**

Two render paths. For the **ListItem path** (debit/prepaid, line ~236-307), the `supportingContent` uses `CardRowSupport(row)`; add the badge inside that composable (or alongside it in the ListItem's `supportingContent` lambda) when `row.card.cardNumberLast4.isNullOrBlank()`, using the same clickable-Text pattern and calling the row's `onEdit`. For the **`CreditCardTile` path** (line ~474-484 where last4 renders via `?.let`), add an `else`/fallback that shows the badge when null:

```kotlin
                if (row.card.cardNumberLast4.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.account_add_last_digits_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onEdit),
                    )
                }
```

If `CreditCardTile`/`CardRowSupport` don't currently receive an `onEdit` lambda, thread it through from the `ActiveCardRow` call site (which has `onEdit = { formMode = FormMode.Edit(row.card) }`). Keep the change minimal — only add the `onEdit` param where the badge needs it.

- [ ] **Step 4: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/accounts/AccountsScreen.kt \
        app/src/main/java/com/subramanya/artha/ui/cards/CardsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(accounts): nudge to add last digits on account/card rows missing them"
```

---

## Task 5: Unmatched-hint "add account/card" prompt in the Review screen

**Files:**
- Create: `app/src/main/java/com/subramanya/artha/ui/review/AddAccountKindChooser.kt`
- Modify: `app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ReviewItem.hasUnmatchedHint` + `ReviewItem.pending.accountHint` (Task 2); `AccountFormSheet(editing, onDismiss, prefillLast4)` / `CardFormSheet(editing, onDismiss, prefillLast4)` (Task 3).
- Produces: `AddAccountKindChooser` composable (leaf).

- [ ] **Step 1: Add strings**

Add to `app/src/main/res/values/strings.xml`:
```xml
    <string name="review_unmatched_account">Unknown account ending in %1$s — tap to add it</string>
    <string name="review_add_kind_title">Add as account or card?</string>
    <string name="review_add_kind_account">Bank account</string>
    <string name="review_add_kind_card">Card</string>
```

- [ ] **Step 2: Create the Account-vs-Card chooser sheet**

```kotlin
package com.subramanya.artha.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R

/** The two account kinds the user can add for an unmatched SMS hint. */
enum class AddAccountKind { ACCOUNT, CARD }

/** Small chooser asking whether an unmatched SMS account belongs to a bank account or a card,
 *  since the SMS wording doesn't reliably disambiguate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountKindChooser(onChosen: (AddAccountKind) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.review_add_kind_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.review_add_kind_account)) },
                modifier = Modifier.clickable { onChosen(AddAccountKind.ACCOUNT) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.review_add_kind_card)) },
                modifier = Modifier.clickable { onChosen(AddAccountKind.CARD) },
            )
        }
    }
}
```

- [ ] **Step 3: Wire the unmatched affordance + chooser + form sheets into `ReviewScreen.kt`**

Add state near the existing `showSheet`/`pendingPrefill` vars (line ~58):
```kotlin
    var chooserForHint: String? by remember { mutableStateOf(null) }
    var addAccountLast4: String? by remember { mutableStateOf(null) }
    var addCardLast4: String? by remember { mutableStateOf(null) }
```

Inside the pending-item `Card`'s content `Column` (line ~139-146), after the merchant/category texts, add the unmatched affordance. Because the outer `Card` has its own `onClick`, use a nested `clickable` Text (a nested clickable inside a `Card(onClick=…)` wins for taps on itself):
```kotlin
                if (item.hasUnmatchedHint) {
                    val hint = item.pending.accountHint.orEmpty()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.review_unmatched_account, hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { chooserForHint = hint },
                    )
                }
```

Add the chooser + form sheets after the existing `AddTransactionSheet` block (after line ~100):
```kotlin
    chooserForHint?.let { hint ->
        AddAccountKindChooser(
            onChosen = { kind ->
                when (kind) {
                    AddAccountKind.ACCOUNT -> addAccountLast4 = hint
                    AddAccountKind.CARD -> addCardLast4 = hint
                }
                chooserForHint = null
            },
            onDismiss = { chooserForHint = null },
        )
    }

    addAccountLast4?.let { last4 ->
        com.subramanya.artha.ui.accounts.AccountFormSheet(
            editing = null,
            onDismiss = { addAccountLast4 = null },
            prefillLast4 = last4,
        )
    }

    addCardLast4?.let { last4 ->
        com.subramanya.artha.ui.cards.CardFormSheet(
            editing = null,
            onDismiss = { addCardLast4 = null },
            prefillLast4 = last4,
        )
    }
```

Ensure imports for `Spacer`, `height`, `dp`, `clickable`, `MaterialTheme` are present in `ReviewScreen.kt` (add any missing).

- [ ] **Step 4: Build to confirm everything compiles**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Full unit test + build sweep**

Run: `./gradlew test assembleDebug`
Expected: `BUILD SUCCESSFUL`, all unit tests pass (including `AccountMatcherTest` from Task 1 and the untouched `BankSmsParserTest`).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/subramanya/artha/ui/review/AddAccountKindChooser.kt \
        app/src/main/java/com/subramanya/artha/ui/review/ReviewScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(sms): offer to add a new account/card for an unmatched SMS hint"
```

---

## Manual verification (after all tasks, on-device)

1. Create a new account — confirm the last-digits field is now required (can't save without 3–6 digits).
2. Confirm an existing account/card with no last-digits shows the "Add last digits" badge; tapping it opens the edit form.
3. Send a bank SMS whose account digits match an account's last-digits → open the Review item → confirm the source/destination account is pre-selected in the Add Transaction sheet.
4. Send a bank SMS whose digits match nothing → confirm the "Unknown account ending in NNNN — tap to add it" affordance appears; tapping it → chooser → form pre-filled with those digits.
5. Two accounts with the same last-digits → confirm no auto-selection (ambiguous), item opens with source unset.

## Self-Review Notes

- **Spec coverage:** Decision 1 (suffix match) → Task 1; Decision 2 (ambiguity → NoMatch) → Task 1 (`candidates.size == 1`); Decision 3 (unmatched → inline add) → Task 5; Decision 4 (backfill nudge) → Task 4; Decision 5 (compulsory 3–6 digits) → Task 3. Auto-select on prefill → Task 2. Out-of-scope items (no re-matching saved txns, no `ACCOUNT_REGEX` change, no bulk screen) are correctly absent.
- **Type consistency:** `AccountMatch`/`AccountMatcher.match` (Task 1) used identically in Task 2; `ReviewItem` 4-field shape (Task 2) consumed in Task 5; `applyPendingSmsPrefill(pending, suggestedCategoryName, matchedFunds)` (Task 2) matches the call in Task 5's ReviewScreen (via Task 2's own ReviewScreen edit); `prefillLast4` param (Task 3) consumed in Task 5. `FundsEndpoint` construction identical to `AddTransactionViewModel.kt:93-105`.
- **No emulator caveat:** `AccountMatcherTest` is pure JVM (runs here). All UI wiring (Tasks 2–5) is compile-verified; behavioral confirmation is the on-device manual checklist above.
