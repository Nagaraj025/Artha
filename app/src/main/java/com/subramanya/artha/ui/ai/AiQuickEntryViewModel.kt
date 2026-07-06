package com.subramanya.artha.ui.ai

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.ai.AiQuickEntryInput
import com.subramanya.artha.ai.AiQuickEntryParsed
import com.subramanya.artha.ai.AiQuickEntryParser
import com.subramanya.artha.ai.AiQuickEntryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiQuickEntryUiState(
    val text: String = "",
    val photo: Bitmap? = null,
    val isParsing: Boolean = false,
    val parsed: AiQuickEntryParsed? = null,
    val noApiKey: Boolean = false,
    val errorMessage: String? = null,
)

class AiQuickEntryViewModel(private val parser: AiQuickEntryParser) : ViewModel() {

    private val _state = MutableStateFlow(AiQuickEntryUiState())
    val state: StateFlow<AiQuickEntryUiState> = _state.asStateFlow()

    fun onTextChanged(v: String) = _state.update { it.copy(text = v) }
    fun onPhotoPicked(bitmap: Bitmap?) = _state.update { it.copy(photo = bitmap) }
    fun appendTranscript(transcript: String) = _state.update {
        val joiner = if (it.text.isBlank() || it.text.endsWith(" ")) "" else " "
        it.copy(text = it.text + joiner + transcript)
    }
    fun reset() = _state.update { AiQuickEntryUiState() }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.isParsing) return
        if (snapshot.text.isBlank() && snapshot.photo == null) return
        _state.update { it.copy(isParsing = true, errorMessage = null, noApiKey = false, parsed = null) }
        viewModelScope.launch {
            val result = parser.parse(AiQuickEntryInput(text = snapshot.text, photo = snapshot.photo))
            _state.update {
                when (result) {
                    is AiQuickEntryResult.Success -> it.copy(isParsing = false, parsed = result.parsed)
                    AiQuickEntryResult.NoApiKey -> it.copy(isParsing = false, noApiKey = true)
                    is AiQuickEntryResult.Error -> it.copy(isParsing = false, errorMessage = result.message)
                }
            }
        }
    }
}

class AiQuickEntryViewModelFactory(private val parser: AiQuickEntryParser) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AiQuickEntryViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AiQuickEntryViewModel(parser) as T
    }
}
