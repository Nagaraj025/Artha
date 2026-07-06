# SMS Auto-Detect → Pending Transaction Review — Design

**Goal:** read incoming bank SMS, detect debit/credit alerts, turn them into draft
transactions that surface in a new "Review" tab (backed by an ongoing notification)
until the user confirms or dismisses them. This activates the SMS-parsing item that
CLAUDE.md already flagged as deferred — the Settings toggle (`SMS_AUTO_IMPORT`
DataStore key) and `TransactionSource.SMS` enum value already exist; nothing else
does (no receiver, no draft storage, no notification channel, no Review UI).

Design settled via brainstorming on 2026-07-03; decisions below are already agreed,
not open questions.

---

## Decisions

1. **Detection strategy:** generic keyword + regex heuristic, not a per-bank
   catalogue. Looks for an amount pattern (`Rs.`/`INR` + digits) + a direction
   keyword (`debited`/`credited`) + account context (`A/c`, card last-4). Messages
   containing OTP/promo keywords are excluded even if they also say "debited".
   Trade-off accepted: broad coverage across Indian banks out of the box, at the
   cost of per-bank precision — acceptable for v1, extendable to a real catalogue
   later without changing the receiver/storage layer.
2. **Scope:** new SMS only (`SMS_RECEIVED` broadcast), not a backfill scan of the
   existing inbox. No `READ_SMS` permission needed, only `RECEIVE_SMS`.
3. **Notification:** single ongoing/ ongoing-style notification showing the pending
   count ("N transactions to review"), updated in place (not stacked per-message),
   cancelled when the pending list is empty. Tapping it opens the Review tab.
4. **Nav placement:** new 6th bottom-nav destination ("Review"), badge = pending
   count. Not nested under Dashboard or More.
5. **Confirm flow:** tapping a pending item opens the existing Add Transaction
   screen pre-filled (same mechanism as AI Quick Entry's prefill) — full field
   review before it becomes a real `Transaction`. No inline one-tap confirm in v1.
6. **Dismiss/duplicates:** swipe-to-dismiss removes a pending item outright (no
   "dismissed" status kept). Duplicate bank alerts for one transaction (some banks
   send two SMS per transaction) are **not** auto-deduped in v1 — user swipes away
   the extra one manually.
7. **Permission flow:** turning the Settings toggle on triggers a runtime request
   for `RECEIVE_SMS` + (API 33+) `POST_NOTIFICATIONS`. The DataStore flag only
   flips true after both are granted. If permission is later revoked, the flag
   flips back off (matches the existing code comment in `SettingsPreferences.kt`).

---

## Architecture

A manifest-registered `BroadcastReceiver` for `android.provider.Telephony.SMS_RECEIVED`.
This broadcast is exempted from Android 8+'s implicit-broadcast background
restrictions specifically because it requires the `RECEIVE_SMS` permission — the
standard pattern used by every SMS-reading app, and it works even when the app
process isn't running.

Flow: SMS arrives → receiver checks `SettingsPreferences.smsAutoImportEnabled`
(bails immediately if off) → `BankSmsParser.parse(sender, body, receivedAt)` →
if non-null, build a provisional `Transaction` candidate and run it through the
**existing `RuleEngine.apply(...)`** to get a suggested category → persist a
`PendingSmsTransactionEntity` row → `PendingTransactionNotifier` updates the
ongoing notification with the new count.

No network/Gemini involved — purely local regex parsing, consistent with the
app's offline-first, local-only design.

### New package: `sms/`

- `BankSmsParser.kt` — pure Kotlin, no Android framework dependency:
  `parse(sender: String, body: String, receivedAt: Long): ParsedBankSms?`.
- `SmsReceiver.kt` — `BroadcastReceiver`, uses `goAsync()` + a short-lived IO
  coroutine to check the DataStore flag, parse, run the rule engine, and persist
  (Room/DataStore access is async, can't block `onReceive`).
- `PendingTransactionNotifier.kt` — owns the notification channel + the single
  reused ongoing notification.

### Data model

New Room entity `PendingSmsTransactionEntity`:

| column | type | notes |
|---|---|---|
| id | String (UUID) | PK |
| rawSmsBody | String | kept for debugging/re-parse if the parser improves later |
| sender | String | SMS sender address (e.g. `HDFCBK`) |
| receivedAt | Long | epoch millis |
| direction | String | `DEBIT` / `CREDIT` |
| amount | Double | |
| accountHint | String? | last-4 digits or account fragment, nullable |
| merchant | String? | parsed narration, nullable |
| suggestedCategoryId | String? | from the `RuleEngine` pre-run |

`PendingTransactionDao` (Room, `Flow<List<PendingSmsTransactionEntity>>`) →
`PendingTransactionRepository` (maps to a domain model, exposes a pending-count
`Flow<Int>` for the nav badge and the notifier) — same DAO → Repository → Flow
convention as the rest of the app.

Rows are deleted outright on dismiss or on successful confirm-and-save; there is
no separate "dismissed" status to track.

### UI

- `ArthaDestinations.kt`: new `Review` destination in the bottom nav enum, with a
  badge sourced from the repository's pending-count flow.
- `ui/review/ReviewScreen.kt` + `ReviewViewModel.kt`: list of pending items as
  cards (amount with existing debit/credit color convention, sender/merchant,
  suggested-category chip, relative received time). Swipe-to-dismiss deletes the
  row. Tapping navigates to the Add Transaction screen with a prefill payload
  (mirrors how `AiQuickEntryParsed` prefills that screen today); empty state
  "No pending transactions".

### Permissions & manifest additions

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<receiver
    android:name=".sms.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

`android:permission="android.permission.BROADCAST_SMS"` restricts who may send
this broadcast to the receiver to the system only — standard practice, not an
app-declared permission.

Settings screen: toggling `smsImport` on triggers
`ActivityResultContracts.RequestMultiplePermissions()` for `RECEIVE_SMS` +
(API 33+) `POST_NOTIFICATIONS` before flipping the DataStore flag.

---

## Error handling / edge cases

- Non-bank SMS (OTP, promotional) are filtered by keyword exclusion in the parser
  even if they contain "debited"/"credited".
- Malformed or unparseable amounts → `parse()` returns `null`, message is ignored
  (not stored, not surfaced).
- Permission revoked after being granted (user turns it off in system Settings) →
  detected on next app resume, `smsAutoImportEnabled` flips back to `false` and the
  Settings toggle reflects it (existing code comment in `SettingsPreferences.kt`
  already documents this intent).
- Duplicate SMS for one transaction → both surface as separate pending items;
  user manually dismisses the extra one (accepted trade-off, see Decisions §6).

---

## Testing

- `BankSmsParserTest.kt` (`app/src/test`) — table-driven unit tests over
  representative sample SMS strings: debit, credit, OTP-should-be-ignored,
  promo-should-be-ignored, malformed amount. Pure Kotlin, no Android dependency,
  same pattern as the existing `RuleEngineTest.kt`.
- `PendingTransactionDao`/Repository — Room in-memory test, same pattern as other
  DAOs.
- `SmsReceiver` itself is not unit-testable (Android framework `BroadcastReceiver`
  lifecycle) — out of scope for an instrumentation test in v1; the parsing logic
  it delegates to is fully covered instead.

---

## Out of scope (v1)

- Per-bank regex catalogue (only the generic heuristic).
- Backfill scan of existing SMS inbox (`READ_SMS`).
- Auto-dedup of duplicate bank alerts.
- Inline one-tap confirm from the Review list (always routes through the full
  Add Transaction screen).
