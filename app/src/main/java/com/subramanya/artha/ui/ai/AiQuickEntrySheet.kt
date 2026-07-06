package com.subramanya.artha.ui.ai

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ai.AiField
import com.subramanya.artha.ai.AiQuickEntryParsed
import com.subramanya.artha.ai.Confidence
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.theme.Danger
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface3
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Teal950
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

/**
 * HANDOFF §3.10 AI Quick Entry sheet — long-press FAB destination.
 *
 * Hero header: 48dp gradient-teal icon container, "AI Quick Entry" title,
 * "Type, dictate, or photograph…" subtitle. Composer: Surface2 multiline
 * box whose border swaps Line1→LineTeal when text is present; floating
 * mic / image / send actions; example chips visible while empty.
 * While parsing a Teal500 dot pulses next to "Reading your entry…".
 * Parsed preview surfaces as a chip card with Cancel + Save footer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiQuickEntrySheet(onDismiss: () -> Unit, onConfirmed: (AiQuickEntryParsed) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: AiQuickEntryViewModel = viewModel(
        factory = AiQuickEntryViewModelFactory(app.aiQuickEntryParser),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull()?.let(vm::onPhotoPicked)
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val transcripts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        transcripts?.firstOrNull()?.let(vm::appendTranscript)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            AiHero()
            Spacer(Modifier.height(20.dp))

            AiComposer(
                text = state.text,
                onTextChanged = vm::onTextChanged,
                photoAttached = state.photo != null,
                onClearPhoto = { vm.onPhotoPicked(null) },
                onMic = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            context.getString(R.string.ai_quick_entry_voice_prompt),
                        )
                    }
                    runCatching { voiceLauncher.launch(intent) }
                },
                onAttach = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onSend = vm::submit,
                sendEnabled = !state.isParsing && (state.text.isNotBlank() || state.photo != null),
                isParsing = state.isParsing,
            )

            // Example chips — visible only while the input is empty and we
            // haven't parsed anything yet. They hint at the kind of prose
            // Gemini understands well so the user isn't typing into the void.
            if (state.text.isBlank() && state.parsed == null && !state.isParsing) {
                Spacer(Modifier.height(16.dp))
                ExampleChips(onPick = vm::onTextChanged)
            }

            if (state.isParsing) {
                Spacer(Modifier.height(16.dp))
                ParsingIndicator()
            }

            if (state.noApiKey) {
                Spacer(Modifier.height(16.dp))
                AiAlert(
                    title = stringResource(R.string.ai_quick_entry_no_key_title),
                    body = stringResource(R.string.ai_quick_entry_no_key_body),
                )
            }
            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(16.dp))
                AiAlert(title = stringResource(R.string.ai_quick_entry_error_title), body = msg)
            }

            state.parsed?.let { parsed ->
                Spacer(Modifier.height(20.dp))
                ParsedCard(
                    parsed = parsed,
                    onCancel = vm::reset,
                    onSave = { onConfirmed(parsed) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * HANDOFF §3.10 header: 48dp gradient-teal icon container + eyebrow + display
 * title + soft subtitle. Sparkles glyph reinforces the "AI" affordance without
 * leaning on emoji.
 */
@Composable
private fun AiHero() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Teal700, Teal500),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Text1,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                text = stringResource(R.string.ai_quick_entry_eyebrow).uppercase(),
                style = EyebrowStyle,
                color = Teal300,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ai_quick_entry_title),
                style = MaterialTheme.typography.titleLarge,
                color = Text1,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.ai_quick_entry_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = Text2,
    )
}

/**
 * Surface2 multiline composer with mic / image / send floating actions. Border
 * switches from Line1 to LineTeal when the user has typed something — a quiet
 * "I see you" affordance the design calls out in §3.10.
 */
@Composable
private fun AiComposer(
    text: String,
    onTextChanged: (String) -> Unit,
    photoAttached: Boolean,
    onClearPhoto: () -> Unit,
    onMic: () -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    sendEnabled: Boolean,
    isParsing: Boolean,
) {
    val borderColor = if (text.isNotBlank() || photoAttached) LineTeal else Line1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            cursorBrush = SolidColor(Teal300),
            textStyle = TextStyle(
                color = Text1,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ai_quick_entry_input_placeholder),
                            color = Text3,
                            style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ComposerIconButton(onClick = onMic) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(R.string.ai_quick_entry_voice),
                    tint = Text2,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            ComposerIconButton(onClick = onAttach) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = stringResource(R.string.ai_quick_entry_photo),
                    tint = Text2,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (photoAttached) {
                Spacer(Modifier.size(8.dp))
                Surface(
                    color = Teal950,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable(onClick = onClearPhoto),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_quick_entry_photo_attached),
                            style = MaterialTheme.typography.labelSmall,
                            color = Teal300,
                        )
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Teal300,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // Send button — solid teal when armed, muted Surface4 otherwise.
            val container = if (sendEnabled) Teal700 else Surface4
            val tint = if (sendEnabled) Text1 else Text3
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(container)
                    .clickable(enabled = sendEnabled && !isParsing, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ai_quick_entry_submit),
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ComposerIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface3)
            .border(1.dp, Line1, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExampleChips(onPick: (String) -> Unit) {
    val examples = listOf(
        stringResource(R.string.ai_quick_entry_example_1),
        stringResource(R.string.ai_quick_entry_example_2),
        stringResource(R.string.ai_quick_entry_example_3),
    )
    Column {
        Text(
            text = stringResource(R.string.ai_quick_entry_examples_label).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            examples.forEach { example ->
                Surface(
                    color = Surface2,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .clickable { onPick(example) }
                        .border(1.dp, Line1, RoundedCornerShape(999.dp)),
                ) {
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum, lnum",
                        ),
                        color = Text2,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * HANDOFF §6 — pulsing Teal500 dot beside "Reading your entry…" while the
 * Gemini parser is in flight. 1.2s breathing animation so it feels alive but
 * doesn't strobe.
 */
@Composable
private fun ParsingIndicator() {
    val transition = rememberInfiniteTransition(label = "ai-parse-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai-parse-alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Teal500)
                .alpha(alpha),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.ai_quick_entry_parsing_label),
            style = MaterialTheme.typography.bodySmall,
            color = Text2,
        )
    }
}

/**
 * Parsed preview rendered as a chip card. Per HANDOFF §3.10 the user should
 * be able to scan the result without reading prose — values become pills,
 * low-confidence ones get a Danger ring and Text2 label, high-confidence
 * ones go Surface4 + Text1.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedCard(parsed: AiQuickEntryParsed, onCancel: () -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, LineTeal, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_quick_entry_preview_title),
            style = EyebrowStyle,
            color = Teal300,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_amount),
                field = parsed.amount,
                render = { it?.let { v -> "₹${IndianNumberFormat.format(v)}" } ?: "—" },
                mono = true,
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_type),
                field = parsed.type,
                render = { it?.name ?: "—" },
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_description),
                field = parsed.description,
                render = { it ?: "—" },
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_category),
                field = parsed.categoryHint,
                render = { it ?: "—" },
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_payment_app),
                field = parsed.paymentApp,
                render = { it?.name ?: "—" },
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_date),
                field = parsed.dateMillis,
                render = { it?.let(DateFormatter::longDate) ?: "—" },
                mono = true,
            )
            chipFor(
                label = stringResource(R.string.ai_quick_entry_preview_place),
                field = parsed.place,
                render = { it ?: "—" },
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Text2,
                ),
            ) {
                Text(stringResource(R.string.ai_quick_entry_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Income,
                    contentColor = Color(0xFF06281C),
                ),
            ) {
                Text(
                    text = stringResource(R.string.ai_quick_entry_save_confirm),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/** Single value pill — label eyebrow + value, tinted by confidence band. */
@Composable
private fun <T> chipFor(label: String, field: AiField<T>, render: (T?) -> String, mono: Boolean = false) {
    val (border, valueColor) = when (field.confidence) {
        Confidence.LOW -> Danger to Danger
        Confidence.MEDIUM -> Line1 to Text2
        Confidence.HIGH -> LineTeal to Text1
    }
    val rendered = render(field.value)
    Surface(
        color = Surface3,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, border, RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label.uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = rendered,
                color = valueColor,
                style = if (mono) {
                    TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 13.sp,
                        fontFeatureSettings = "tnum, lnum",
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                },
            )
        }
    }
}

@Composable
private fun AiAlert(title: String, body: String) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Danger,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = Text2)
        }
    }
}

@Suppress("unused")
private val keepTeal900Reference = Teal900
