# SMS Account Matching + Compulsory Last-Digits — Design

**Goal:** when a bank SMS is parsed into a Review-tab draft, automatically resolve
which of the user's existing accounts/cards it belongs to (by matching the last
digits already extracted from the SMS), and make entering last-digits compulsory
when creating a new account/card so this matching actually has something to match
against.

Design settled via brainstorming on 2026-07-06; decisions below are already agreed,
not open questions. Builds directly on the SMS auto-detect feature
(`docs/plans/2026-07-03-sms-auto-detect-design.md`), on branch `feat/sms-auto-detect`.

---

## Current state (confirmed, not assumed)

- `Account.accountNumberLast4: String?` and `Card.cardNumberLast4: String?` **already
  exist** as optional fields (`domain/model/Account.kt`, `domain/model/Card.kt`,
  mirrored in `AccountEntity`/`CardEntity` via `@ColumnInfo`). **No schema migration
  needed** — this is a validation/UX change, not a new column.
- `AccountFormSheet.kt`/`CardFormSheet.kt` already collect last-digits, but as
  *optional*, capped at exactly 4 digits (`last4.length == 4`).
- `BankSmsParser.parse()` already extracts `accountHint: String?` (3–6 digits, via
  `ACCOUNT_REGEX`) into `ParsedBankSms`, which flows through to
  `PendingSmsTransaction.accountHint` — but nothing downstream reads it back. It's
  stored and ignored today.
- `AccountRepository.observeAll(): Flow<List<Account>>` and
  `CardRepository.observeAll(): Flow<List<Card>>` already exist for querying all
  accounts/cards to match against.
- All accounts/cards are user-created (no seeded defaults), so every existing
  install has some rows with `accountNumberLast4`/`cardNumberLast4 == null`.

---

## Decisions

1. **Matching rule: suffix match, not exact match.** Given a pending item's
   `accountHint` and a candidate account/card's stored last-digits, they match if
   one is a trailing substring of the other (handles different banks reporting
   different digit counts for the same underlying account, e.g. hint `"1234"` vs
   stored `"234"`). Case: both empty/blank never match.
2. **Ambiguity → no auto-selection.** If a hint matches exactly one account/card,
   pre-select it as the transaction's source/destination. If it matches zero or
   more than one, leave it unselected (today's behavior) — guessing on ambiguity
   would be worse than asking the user to pick.
3. **Unmatched hint → inline "add new account/card" prompt**, not silent
   unmatch-and-forget. The Review item shows a small affordance ("Unknown account
   ending in 1234 — tap to add it"). Tapping it asks Account-vs-Card (a two-button
   choice — the SMS wording doesn't reliably disambiguate), then opens the
   **existing** `AccountFormSheet`/`CardFormSheet` pre-filled with the detected
   digits. Dismissible — skipping leaves the item unmatched, same as today.
4. **Existing accounts/cards backfill: soft nudge, not a blocking gate.** Any
   account/card with a null last-digits field shows a small "Add last digits"
   badge/indicator in the Accounts/Cards list screens. Tapping it opens that
   item's existing edit form, focused on the digits field. The app remains fully
   usable with accounts missing this field — they just won't participate in
   SMS matching until filled in.
5. **New compulsory field: 3–6 digits, user's choice of length**, matching the
   full range `BankSmsParser.ACCOUNT_REGEX` can capture (not a fixed 4). Applies
   to *new* accounts/cards created from now on (Decision 4 covers pre-existing
   ones separately, via nudge not a hard block at creation-review time).

---

## Architecture

### New pure-Kotlin matcher: `sms/AccountMatcher.kt`

```kotlin
fun matchAccount(hint: String?, accounts: List<Account>, cards: List<Card>): MatchResult
```
Where `MatchResult` is a sealed result: `Matched(FundsEndpoint)` (exactly one
suffix match across the combined account+card pool) or `NoMatch` (hint null/blank,
zero matches, or 2+ matches). Suffix-match helper:
```kotlin
private fun suffixMatches(a: String, b: String): Boolean =
    a.isNotBlank() && b.isNotBlank() && (a.endsWith(b) || b.endsWith(a))
```
Pure function, no Android/Room dependency — unit-testable the same way
`BankSmsParser`/`SmsRuleSuggestion` already are.

### Wiring into the existing Review flow

`ReviewViewModel` already loads `pendingTransactionRepository.observeAll()` and
resolves a suggested category per item (`ReviewItem`). Extend `ReviewItem` with a
`matchedFunds: FundsEndpoint?` (from `AccountMatcher`, using
`accountRepository.observeAll()`/`cardRepository.observeAll()` already injected
into the ViewModel — no new repository methods needed) and a
`hasUnmatchedHint: Boolean` flag (hint present but no match) for the "tap to add
it" affordance.

`AddTransactionViewModel.applyPendingSmsPrefill` (already exists) gets one more
parameter, `matchedFunds: FundsEndpoint?`, and sets `state.source`/`state.destination`
from it when non-null (mirroring how `tab`/`amountText`/etc. are already set from
the pending item) — this is the "auto-selected source/destination" behavior.

### New-account-from-SMS flow

`ReviewScreen`'s "tap to add it" affordance opens a small two-option chooser
(Account vs Card) reusing the app's existing sheet-chooser pattern, then launches
the corresponding **existing** `AccountFormSheet`/`CardFormSheet` with its
last-digits field pre-filled from the hint. No new form is built — this is
strictly a new entry point into forms that already exist and already validate.

### Form + backfill-nudge changes

- `AccountFormSheet.kt`/`CardFormSheet.kt`: last-digits validation changes from
  `last4.isEmpty() || (last4.length == 4 && last4.all { it.isDigit() })` to
  `last4.length in 3..6 && last4.all { it.isDigit() }` (no longer allowing empty),
  and the field's `optional = true` flag is removed, mirroring the existing
  required-`name`-field error-display pattern.
- `AccountsScreen.kt`/`CardsScreen.kt`: each list item conditionally shows a small
  badge when `accountNumberLast4`/`cardNumberLast4 == null`, tapping it opens that
  item's existing edit form.

---

## Error handling / edge cases

- Hint is null (SMS had no parseable account reference) → `NoMatch`, no "add new
  account" prompt either (nothing to offer digits for).
- Multiple accounts share overlapping last digits (e.g. two accounts both ending
  `1234`) → ambiguous, `NoMatch`, user picks manually — never silently pick one.
- User dismisses the "add new account" prompt → item stays unmatched permanently
  for that pending row (no re-prompt loop); they can still manually pick an
  account when they open Add Transaction.
- A newly-added account/card (from the inline prompt) immediately becomes
  eligible for matching future SMS with the same digits — no extra wiring needed,
  since matching re-queries `observeAll()` live.

---

## Testing

- `AccountMatcherTest.kt` (`app/src/test`) — pure unit tests: exact match, suffix
  match both directions, no match (empty hint), ambiguous match (2+ candidates)
  returns `NoMatch`, mirrors the existing `BankSmsParserTest`/
  `SmsRuleSuggestionTest` style (no Android dependency).
- Form validation tests for the loosened 3–6 digit range, mirroring however the
  existing (if any) `AccountFormSheet`/`CardFormSheet` validation is tested today
  — if no existing test file covers this validation, this doesn't newly require
  one beyond what the implementation plan's task scoping decides.

## Out of scope (this update)

- Retroactively re-matching already-created (and already-confirmed/saved) real
  transactions against newly backfilled last-digits — this only affects
  Review-tab items going forward.
- Any change to `BankSmsParser`'s `ACCOUNT_REGEX` capture range itself (still
  3–6 digits, unchanged) — this update only consumes that existing hint.
- A dedicated "manage all accounts missing digits" bulk screen — the nudge is
  per-item in the existing list screens, not a separate aggregated view.
