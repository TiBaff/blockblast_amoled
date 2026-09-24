package com.blockblast.amoled

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

val AmoledBlack = Color(0xFF000000)
val BlockGray = Color(0xFF7E7E7E)
val GridBorderColor = Color(0xFF222222)
val ButtonDarkGray = Color(0xFF3A3A3A)
val GhostBlockColor = Color(0x667E7E7E)

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val googleSansFont = GoogleFont("Google Sans")

val GoogleSans = FontFamily(
    Font(googleFont = googleSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = googleSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = googleSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

private val DarkColorScheme = darkColorScheme(
    primary = BlockGray,
    background = AmoledBlack,
    surface = AmoledBlack,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BlockBlastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
