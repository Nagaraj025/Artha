package com.subramanya.artha.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.subramanya.artha.R

/**
 * Four font families per the design tokens (artha/project/components/tokens.css):
 *  - Plus Jakarta Sans → UI body text & headings
 *  - Instrument Serif  → display numerals on the Net Position hero
 *  - IBM Plex Mono     → secondary numerics with lined figures
 *  - Tiro Devanagari Hindi → the अ brand glyph
 *
 * All four are downloaded via the GMS fonts provider on first launch. Falling
 * back to system fonts while in flight is automatic — Compose handles it.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun gFont(name: String, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal) =
    Font(googleFont = GoogleFont(name), fontProvider = provider, weight = weight, style = style)

val PlusJakartaSans: FontFamily = FontFamily(
    gFont("Plus Jakarta Sans", FontWeight.Light),
    gFont("Plus Jakarta Sans", FontWeight.Normal),
    gFont("Plus Jakarta Sans", FontWeight.Medium),
    gFont("Plus Jakarta Sans", FontWeight.SemiBold),
    gFont("Plus Jakarta Sans", FontWeight.Bold),
    gFont("Plus Jakarta Sans", FontWeight.ExtraBold),
)

val InstrumentSerif: FontFamily = FontFamily(
    gFont("Instrument Serif", FontWeight.Normal),
    gFont("Instrument Serif", FontWeight.Normal, style = FontStyle.Italic),
)

val IbmPlexMono: FontFamily = FontFamily(
    gFont("IBM Plex Mono", FontWeight.Light),
    gFont("IBM Plex Mono", FontWeight.Normal),
    gFont("IBM Plex Mono", FontWeight.Medium),
    gFont("IBM Plex Mono", FontWeight.SemiBold),
)

val TiroDevanagariHindi: FontFamily = FontFamily(
    gFont("Tiro Devanagari Hindi", FontWeight.Normal),
    gFont("Tiro Devanagari Hindi", FontWeight.Normal, style = FontStyle.Italic),
)
