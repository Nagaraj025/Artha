package com.subramanya.artha.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.ai.AiQuickEntryParser
import com.subramanya.artha.ai.KeyValidationResult
import com.subramanya.artha.data.backup.BackupCodec
import com.subramanya.artha.data.backup.BackupRepository
import com.subramanya.artha.data.db.AppDatabase
import com.subramanya.artha.data.db.seed.SeedCategories
import com.subramanya.artha.data.importing.BankImporter
import com.subramanya.artha.data.preferences.SettingsPreferences
import com.subramanya.artha.data.preferences.SpouseTransactionDefault
import com.subramanya.artha.data.preferences.ThemeMode
import com.subramanya.artha.security.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val spouseDefault: SpouseTransactionDefault = SpouseTransactionDefault.ASK,
    val dashboardShowMonthly: Boolean = true,
    val dashboardShowAccounts: Boolean = true,
    val dashboardShowCards: Boolean = true,
    val dashboardShowRecent: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val smsAutoImportEnabled: Boolean = false,
    val aiQuickEntryEnabled: Boolean = false,
    val showFirstResetDialog: Boolean = false,
    val showFinalResetDialog: Boolean = false,
    val showWipeImportConfirm: Boolean = false,
    val isImportingBundled: Boolean = false,
    /** Set right after Export → file is ready to be shared. */
    val pendingExportFile: File? = null,
    /** AI Quick Entry — true once the user has stored any non-blank key. We only
     *  expose presence; the raw key never leaks back to the UI. */
    val hasAiKey: Boolean = false,
    val aiKeySaveInFlight: Boolean = false,
    /** Stable string keys so the UI can map them to localized toast strings without
     *  the ViewModel taking a Context dependency. */
    val aiKeyStatus: AiKeyStatus = AiKeyStatus.Idle,
    /** True while a restore is wiping + reinserting; blocks re-triggering. */
    val isRestoring: Boolean = false,
    /** Outcome of the last restore, surfaced once then acknowledged. */
    val restoreResult: RestoreResult = RestoreResult.Idle,
    /** Set when the user picked an encrypted `.artha` file — the UI shows a password
     *  prompt and passes the password + this uri back to [SettingsViewModel.importDataEncrypted]. */
    val pendingEncryptedRestoreUri: Uri? = null,
)

/** Lifecycle of a restore-from-backup attempt. Maps 1:1 to a toast in the UI. */
sealed interface RestoreResult {
    data object Idle : RestoreResult
    data object Success : RestoreResult

    /** Wrong password / not an Artha encrypted backup. */
    data object WrongPassword : RestoreResult

    /** File couldn't be read or parsed. The DB was NOT touched (parse precedes wipe). */
    data object InvalidFile : RestoreResult
}

/** ViewModel-side enum for the "did the key save" lifecycle. Maps 1:1 to a toast. */
sealed interface AiKeyStatus {
    data object Idle : AiKeyStatus
    data object Saved : AiKeyStatus
    data object Cleared : AiKeyStatus
    data class Invalid(val message: String) : AiKeyStatus
    data class NetworkError(val message: String) : AiKeyStatus
}

/** Account names the bank-statement importer creates. Wipe-imports keys off these. */
private val IMPORTED_ACCOUNT_NAMES = setOf("Federal Bank (Jupiter)", "ICICI Bank")
private const val IMPORTED_NOTES_PREFIX = "Imported from "

class SettingsViewModel(
    private val settingsPreferences: SettingsPreferences,
    private val database: AppDatabase,
    private val aiParser: AiQuickEntryParser,
) : ViewModel() {

    private val firstResetDialog = MutableStateFlow(false)
    private val finalResetDialog = MutableStateFlow(false)
    private val wipeImportConfirm = MutableStateFlow(false)
    private val importingBundled = MutableStateFlow(false)
    private val pendingExport = MutableStateFlow<File?>(null)
    private val aiSaveInFlight = MutableStateFlow(false)
    private val aiKeyStatus = MutableStateFlow<AiKeyStatus>(AiKeyStatus.Idle)
    private val restoring = MutableStateFlow(false)
    private val restoreResult = MutableStateFlow<RestoreResult>(RestoreResult.Idle)
    private val pendingEncryptedRestoreUri = MutableStateFlow<Uri?>(null)

    private val backupRepository = BackupRepository(database)

    private val dashboardPrefs = combine(
        settingsPreferences.dashboardShowMonthly,
        settingsPreferences.dashboardShowAccounts,
        settingsPreferences.dashboardShowCards,
        settingsPreferences.dashboardShowRecent,
    ) { monthly, accounts, cards, recent -> DashboardVisibility(monthly, accounts, cards, recent) }

    private val securityPrefs = combine(
        settingsPreferences.biometricLockEnabled,
        settingsPreferences.smsAutoImportEnabled,
    ) { lock, sms -> SecurityPrefs(lock, sms) }

    private val dialogFlags = combine(
        firstResetDialog,
        finalResetDialog,
        wipeImportConfirm,
        pendingExport,
        dashboardPrefs,
    ) { first, final, wipe, export, vis ->
        DialogsAndVisibility(first, final, wipe, export, vis, importing = false, security = SecurityPrefs(false, false))
    }.combine(importingBundled) { bag, importing ->
        bag.copy(importing = importing)
    }.combine(securityPrefs) { bag, security ->
        bag.copy(security = security)
    }

    // The restore sub-state (result + pending-password uri) folded into one flow so the
    // aiBag combine below stays inside the 5-arg arity cap.
    private val restoreBag = combine(
        restoring,
        restoreResult,
        pendingEncryptedRestoreUri,
    ) { isRestoring, result, pendingUri -> RestoreBag(isRestoring, result, pendingUri) }

    // Fold AI + restore flags into a single secondary source so the primary combine
    // stays inside the 5-arg arity cap.
    private val aiBag = combine(
        settingsPreferences.geminiApiKey,
        aiSaveInFlight,
        aiKeyStatus,
        restoreBag,
        settingsPreferences.aiQuickEntryEnabled,
    ) { key, inFlight, status, restore, enabled ->
        AiBag(
            hasKey = key.isNotBlank(),
            inFlight = inFlight,
            status = status,
            restore = restore,
            enabled = enabled,
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        settingsPreferences.userName,
        settingsPreferences.themeMode,
        settingsPreferences.useDynamicColor,
        settingsPreferences.spouseTransactionDefault,
        combine(dialogFlags, aiBag) { bag, ai -> bag to ai },
    ) { name, theme, dynamic, spouse, (bag, ai) ->
        SettingsUiState(
            userName = name,
            themeMode = theme,
            useDynamicColor = dynamic,
            spouseDefault = spouse,
            dashboardShowMonthly = bag.visibility.monthly,
            dashboardShowAccounts = bag.visibility.accounts,
            dashboardShowCards = bag.visibility.cards,
            dashboardShowRecent = bag.visibility.recent,
            showFirstResetDialog = bag.firstReset,
            showFinalResetDialog = bag.finalReset,
            showWipeImportConfirm = bag.wipeImport,
            isImportingBundled = bag.importing,
            pendingExportFile = bag.export,
            biometricLockEnabled = bag.security.biometric,
            smsAutoImportEnabled = bag.security.smsImport,
            hasAiKey = ai.hasKey,
            aiQuickEntryEnabled = ai.enabled,
            aiKeySaveInFlight = ai.inFlight,
            aiKeyStatus = ai.status,
            isRestoring = ai.restore.isRestoring,
            restoreResult = ai.restore.result,
            pendingEncryptedRestoreUri = ai.restore.pendingEncryptedUri,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private data class RestoreBag(val isRestoring: Boolean, val result: RestoreResult, val pendingEncryptedUri: Uri?)

    private data class AiBag(
        val hasKey: Boolean,
        val inFlight: Boolean,
        val status: AiKeyStatus,
        val restore: RestoreBag,
        val enabled: Boolean,
    )

    private data class DashboardVisibility(val monthly: Boolean, val accounts: Boolean, val cards: Boolean, val recent: Boolean)
    private data class SecurityPrefs(val biometric: Boolean, val smsImport: Boolean)
    private data class DialogsAndVisibility(
        val firstReset: Boolean,
        val finalReset: Boolean,
        val wipeImport: Boolean,
        val export: File?,
        val visibility: DashboardVisibility,
        val importing: Boolean,
        val security: SecurityPrefs,
    )

    fun onNameChanged(value: String) {
        viewModelScope.launch { settingsPreferences.setUserName(value) }
    }

    fun onThemeChanged(mode: ThemeMode) {
        viewModelScope.launch { settingsPreferences.setThemeMode(mode) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setUseDynamicColor(enabled) }
    }

    fun onSpouseDefaultChanged(value: SpouseTransactionDefault) {
        viewModelScope.launch { settingsPreferences.setSpouseTransactionDefault(value) }
    }

    fun resetSpousePrompt() {
        viewModelScope.launch { settingsPreferences.resetSpouseTransactionDefault() }
    }

    fun onDashboardShowMonthlyChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowMonthly(enabled) }
    }
    fun onDashboardShowAccountsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowAccounts(enabled) }
    }
    fun onDashboardShowCardsChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowCards(enabled) }
    }
    fun onDashboardShowRecentChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setDashboardShowRecent(enabled) }
    }

    fun onBiometricLockChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setBiometricLockEnabled(enabled) }
    }

    fun onAiQuickEntryEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setAiQuickEntryEnabled(enabled) }
    }

    fun onSmsAutoImportChanged(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setSmsAutoImportEnabled(enabled) }
    }

    /**
     * Persist a new Gemini key only after a live test call succeeds. We never store
     * an unvalidated key — that's the point of this flow. NetworkError is surfaced
     * to the user (so they can decide to retry on better signal) but also blocks
     * the save, since we can't be sure the key is good.
     */
    fun saveAiKey(candidate: String) {
        val trimmed = candidate.trim()
        if (trimmed.isBlank()) {
            aiKeyStatus.update { AiKeyStatus.Invalid("Key is empty") }
            return
        }
        if (aiSaveInFlight.value) return
        viewModelScope.launch {
            aiSaveInFlight.update { true }
            aiKeyStatus.update { AiKeyStatus.Idle }
            when (val result = aiParser.validateKey(trimmed)) {
                KeyValidationResult.Ok -> {
                    settingsPreferences.setGeminiApiKey(trimmed)
                    aiKeyStatus.update { AiKeyStatus.Saved }
                }
                is KeyValidationResult.Invalid ->
                    aiKeyStatus.update { AiKeyStatus.Invalid(result.message) }
                is KeyValidationResult.NetworkError ->
                    aiKeyStatus.update { AiKeyStatus.NetworkError(result.message) }
            }
            aiSaveInFlight.update { false }
        }
    }

    fun clearAiKey() {
        viewModelScope.launch {
            settingsPreferences.clearGeminiApiKey()
            aiKeyStatus.update { AiKeyStatus.Cleared }
        }
    }

    /** Call after the UI has surfaced the latest status to the user. */
    fun acknowledgeAiKeyStatus() {
        aiKeyStatus.update { AiKeyStatus.Idle }
    }

    // ----- export -----

    /**
     * Writes a COMPLETE JSON snapshot of every Room table to `context.cacheDir` and flips
     * state to `pendingExportFile`, which the host turns into an ACTION_SEND chooser.
     *
     * Snapshot + serialization go through [BackupRepository.snapshot] + [BackupCodec.encode]
     * — the SAME path the encrypted export uses, so the two can never drift (that drift was
     * the D3 audit bug). Every table incl. cross-refs is covered.
     */
    fun exportData(context: Context) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val json = BackupCodec.encode(backupRepository.snapshot(), System.currentTimeMillis())
                val out = File(context.cacheDir, "artha_export_${System.currentTimeMillis()}.json")
                out.writeText(json)
                out
            }
            pendingExport.update { file }
        }
    }

    /**
     * Same as [exportData] but PBKDF2-AES-GCM encrypts the JSON with [password] first.
     * Caller passes an in-memory CharArray (not a String) so the secret can be wiped
     * after use — best practice for passwords.
     */
    fun exportDataEncrypted(context: Context, password: CharArray) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val json = BackupCodec.encode(backupRepository.snapshot(), System.currentTimeMillis())
                val cipherText = BackupCrypto.encrypt(json, password)
                password.fill(' ') // wipe in-memory copy
                val out = File(context.cacheDir, "artha_backup_${System.currentTimeMillis()}.artha")
                out.writeText(cipherText)
                out
            }
            pendingExport.update { file }
        }
    }

    fun acknowledgeExport() {
        pendingExport.update { null }
    }

    // ----- restore (import) -----

    /**
     * Restores from a plain `.json` backup at [uri], REPLACING all current data. The file
     * is read and fully decoded BEFORE any wipe; [BackupRepository.restore] then does the
     * wipe + reinsert in a single Room transaction, so a corrupt file reports an error and
     * leaves existing data untouched. Outcome lands on [SettingsUiState.restoreResult].
     */
    fun importData(context: Context, uri: Uri) {
        if (restoring.value) return
        viewModelScope.launch {
            restoring.update { true }
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = readText(context, uri)
                    backupRepository.restore(BackupCodec.decode(raw))
                }
            }
            restoring.update { false }
            restoreResult.update {
                if (outcome.isSuccess) RestoreResult.Success else RestoreResult.InvalidFile
            }
        }
    }

    /**
     * Routes a just-picked backup file: peeks the first line to tell an encrypted `.artha`
     * (magic-prefixed) from a plain `.json`. Encrypted -> stash the uri and let the UI prompt
     * for a password; plain -> restore immediately. Keeps the file-format sniff out of the UI.
     */
    fun prepareRestore(context: Context, uri: Uri) {
        if (restoring.value) return
        viewModelScope.launch {
            val encrypted = withContext(Dispatchers.IO) {
                runCatching { BackupCrypto.isEncrypted(readText(context, uri)) }.getOrDefault(false)
            }
            if (encrypted) {
                pendingEncryptedRestoreUri.update { uri }
            } else {
                importData(context, uri)
            }
        }
    }

    /** User dismissed the encrypted-backup password prompt without restoring. */
    fun cancelEncryptedRestore() {
        pendingEncryptedRestoreUri.update { null }
    }

    /**
     * Restores from an encrypted `.artha` backup at [uri] using [password]: decrypt ->
     * decode -> restore. A wrong password (AES-GCM tag mismatch) or a non-Artha file reports
     * [RestoreResult.WrongPassword] WITHOUT touching the database; a decrypted-but-corrupt
     * payload reports [RestoreResult.InvalidFile]. The CharArray is wiped after use.
     */
    fun importDataEncrypted(context: Context, uri: Uri, password: CharArray) {
        if (restoring.value) return
        viewModelScope.launch {
            pendingEncryptedRestoreUri.update { null }
            restoring.update { true }
            val outcome = withContext(Dispatchers.IO) {
                val raw = runCatching { readText(context, uri) }.getOrNull()
                if (raw == null) {
                    password.fill(' ')
                    return@withContext RestoreResult.InvalidFile
                }
                val decrypted = BackupCrypto.decrypt(raw, password)
                password.fill(' ')
                if (decrypted.isFailure) {
                    return@withContext RestoreResult.WrongPassword
                }
                runCatching {
                    backupRepository.restore(BackupCodec.decode(decrypted.getOrThrow()))
                }.fold(
                    onSuccess = { RestoreResult.Success },
                    onFailure = { RestoreResult.InvalidFile },
                )
            }
            restoring.update { false }
            restoreResult.update { outcome }
        }
    }

    fun acknowledgeRestore() {
        restoreResult.update { RestoreResult.Idle }
    }

    private fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalStateException("Could not open backup file")

    // ----- reset -----

    fun requestReset() = firstResetDialog.update { true }
    fun dismissFirstReset() = firstResetDialog.update { false }
    fun proceedToFinalReset() {
        firstResetDialog.update { false }
        finalResetDialog.update { true }
    }
    fun dismissFinalReset() = finalResetDialog.update { false }

    fun confirmReset(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                // Re-seed the categories the way DatabaseProvider's callback would on first launch.
                database.categoryDao().upsertAll(SeedCategories.all())
            }
            settingsPreferences.resetSpouseTransactionDefault()
            finalResetDialog.update { false }
            onDone()
        }
    }

    // ----- import bundled bank-statement data (APK asset) -----

    /**
     * Reads `assets/seed/bank_import.json` (generated offline by the Python
     * script) and writes the 3,913 transactions + their two source accounts
     * into Room. Idempotent — calling twice is a no-op thanks to deterministic
     * UUID5 transaction IDs + IGNORE-on-conflict.
     *
     * Callers get the result via [onDone] so they can surface a toast.
     */
    fun importBundledBankData(context: Context, onDone: (BankImporter.Result) -> Unit) {
        if (importingBundled.value) return
        viewModelScope.launch {
            importingBundled.update { true }
            val result = withContext(Dispatchers.IO) {
                BankImporter(context.applicationContext, database).importBundled()
            }
            importingBundled.update { false }
            onDone(result)
        }
    }

    // ----- wipe imported bank-statement data -----

    fun requestWipeImport() = wipeImportConfirm.update { true }
    fun dismissWipeImport() = wipeImportConfirm.update { false }

    /**
     * Removes only the data the external bank-statement importer added:
     *   - every transaction whose notes start with "Imported from "
     *   - every account whose name matches one of [IMPORTED_ACCOUNT_NAMES]
     *
     * Manual entries stay untouched. Returns counts via [onDone] so the host can
     * surface a toast.
     */
    fun wipeImportedData(onDone: (deletedTxns: Int, deletedAccounts: Int) -> Unit) {
        viewModelScope.launch {
            val (txnCount, acctCount) = withContext(Dispatchers.IO) {
                val txnDao = database.transactionDao()
                val acctDao = database.accountDao()
                val allTxns = txnDao.observeAll().first()
                val importedTxnIds = allTxns
                    .filter { (it.notes ?: "").startsWith(IMPORTED_NOTES_PREFIX) }
                    .map { it.id }
                if (importedTxnIds.isNotEmpty()) txnDao.deleteByIds(importedTxnIds)

                val allAccts = acctDao.observeAll().first()
                val importedAccts = allAccts.filter { it.name in IMPORTED_ACCOUNT_NAMES }
                importedAccts.forEach { acctDao.delete(it) }

                importedTxnIds.size to importedAccts.size
            }
            wipeImportConfirm.update { false }
            onDone(txnCount, acctCount)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsPreferences: SettingsPreferences,
    private val database: AppDatabase,
    private val aiParser: AiQuickEntryParser,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return SettingsViewModel(settingsPreferences, database, aiParser) as T
    }
}
