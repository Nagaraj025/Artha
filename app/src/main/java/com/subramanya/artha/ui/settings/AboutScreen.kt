package com.subramanya.artha.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.BuildConfig
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.BrandMark
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.TiroDevanagariHindi

/**
 * HANDOFF §3.7 About — 88dp BrandMark (22dp radius), Devanagari "अर्थ"
 * caption in Tiro 18sp teal-300, followed by the four-puruṣārthas essay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            com.subramanya.artha.ui.common.InlineTopBar(
                title = stringResource(R.string.about_title),
                onBack = onBack,
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                // Eyebrow over the editorial body.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .background(LineTeal)
                            .padding(end = 14.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_eyebrow).uppercase(),
                        style = EyebrowStyle,
                        color = Teal300,
                    )
                }
                Spacer(Modifier.height(16.dp))

                // 88dp BrandMark, 22dp corner radius (matches design spec).
                BrandMark(
                    size = 88.dp,
                    cornerRadiusDp = 22.dp,
                )
                Spacer(Modifier.height(16.dp))

                // Devanagari title: "अर्थ" in Tiro Devanagari Hindi 18sp teal-300.
                Text(
                    text = stringResource(R.string.about_devanagari),
                    style = TextStyle(
                        fontFamily = TiroDevanagariHindi,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                    ),
                    color = Teal300,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_artha_caption),
                    style = TextStyle(
                        fontFamily = InstrumentSerif,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                    color = Text1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_one_of_four),
                    style = MaterialTheme.typography.bodySmall,
                    color = Text3,
                )

                Spacer(Modifier.height(20.dp))
                // The essay body, in a soft Surface2 card to set it apart.
                Surface(
                    color = Surface2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line1, RoundedCornerShape(16.dp)),
                ) {
                    Text(
                        text = stringResource(R.string.about_essay),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = Text2,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                AboutMetaRow(
                    eyebrow = stringResource(R.string.about_version_eyebrow),
                    value = BuildConfig.VERSION_NAME,
                )
                Spacer(Modifier.height(10.dp))
                AboutMetaRow(
                    eyebrow = stringResource(R.string.about_credit_title),
                    value = stringResource(R.string.about_credit_value),
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.about_built_with),
                    style = MaterialTheme.typography.bodySmall,
                    color = Text3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AboutMetaRow(eyebrow: String, value: String) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line1, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = EyebrowStyle,
                color = Text3,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = IbmPlexMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Text1,
                    fontFeatureSettings = "tnum, lnum",
                ),
            )
        }
    }
}

@Suppress("unused")
private val keepTextStyleReference = Text1
