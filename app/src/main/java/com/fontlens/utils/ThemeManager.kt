package com.fontlens.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import com.fontlens.R
import com.fontlens.data.AppSettings
import com.fontlens.data.ColorTheme
import com.fontlens.data.ThemeMode

object ThemeManager {

    data class Palette(
        val bgPrimary: Int,
        val bgSurface: Int,
        val bgElevated: Int,
        val bgSidebar: Int,
        val accent: Int,
        val accentDim: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val textMuted: Int,
        val divider: Int,
        val border: Int
    )

    fun getPalette(settings: AppSettings): Palette {
        val dark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemDark()
            ThemeMode.DAY    -> false
            ThemeMode.NIGHT  -> true
        }
        return when (settings.colorTheme) {
            ColorTheme.GREEN  -> if (dark) greenDark  else greenLight
            ColorTheme.BLUE   -> if (dark) blueDark   else blueLight
            ColorTheme.RED    -> if (dark) redDark    else redLight
            ColorTheme.YELLOW -> if (dark) yellowDark else yellowLight
        }
    }

    fun currentThemeResId(context: android.content.Context): Int =
        themeResId(com.fontlens.data.FontRepository.settings)

    fun themeResId(settings: AppSettings): Int {
        val dark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemDark()
            ThemeMode.DAY    -> false
            ThemeMode.NIGHT  -> true
        }
        return when (settings.colorTheme) {
            ColorTheme.GREEN  -> if (dark) R.style.Theme_FontLens_Green_Dark  else R.style.Theme_FontLens_Green_Light
            ColorTheme.BLUE   -> if (dark) R.style.Theme_FontLens_Blue_Dark   else R.style.Theme_FontLens_Blue_Light
            ColorTheme.RED    -> if (dark) R.style.Theme_FontLens_Red_Dark    else R.style.Theme_FontLens_Red_Light
            ColorTheme.YELLOW -> if (dark) R.style.Theme_FontLens_Yellow_Dark else R.style.Theme_FontLens_Yellow_Light
        }
    }

    fun applyNightMode(settings: AppSettings) {
        AppCompatDelegate.setDefaultNightMode(when (settings.themeMode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.DAY    -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.NIGHT  -> AppCompatDelegate.MODE_NIGHT_YES
        })
    }

    /**
     * Updates the app's color resources at runtime so that @color/sem_* references
     * in drawables reflect the active theme. Call this in MainActivity.onCreate()
     * after setTheme(), and the drawables will use the right colors.
     *
     * We do this by updating the application-level resources via reflection on
     * the resource table — but that's fragile. Instead, we inject via a custom
     * Resources wrapper stored in the Application context.
     *
     * SIMPLER APPROACH used here: we store the active palette in a companion object
     * and fragments/activities call applyPaletteToViews() on their root views.
     */
    var activePalette: Palette = Palette(
        bgPrimary=0xFFF4F6F4.toInt(), bgSurface=0xFFFFFFFF.toInt(), bgElevated=0xFFEAEEEA.toInt(),
        bgSidebar=0xFFDDE5DD.toInt(), accent=0xFF2E7D32.toInt(), accentDim=0xFFC8E6C9.toInt(),
        textPrimary=0xFF1B1B1B.toInt(), textSecondary=0xFF4A4A4A.toInt(), textMuted=0xFF888888.toInt(),
        divider=0xFFD0D9D0.toInt(), border=0xFFB8C8B8.toInt()
    )

    fun applyTheme(settings: AppSettings) {
        activePalette = getPalette(settings)
    }

    fun bulletColor(theme: ColorTheme): Int = when (theme) {
        ColorTheme.GREEN  -> Color.parseColor("#2E7D32")
        ColorTheme.BLUE   -> Color.parseColor("#1565C0")
        ColorTheme.RED    -> Color.parseColor("#B71C1C")
        ColorTheme.YELLOW -> Color.parseColor("#E65100")
    }

    private fun isSystemDark(): Boolean =
        (android.content.res.Configuration.UI_MODE_NIGHT_YES ==
            (android.content.res.Resources.getSystem().configuration.uiMode
                and android.content.res.Configuration.UI_MODE_NIGHT_MASK))

    // ── Palettes ─────────────────────────────────────────────────────────
    val greenLight = Palette(
        bgPrimary   = Color.parseColor("#F4F6F4"),
        bgSurface   = Color.parseColor("#FFFFFF"),
        bgElevated  = Color.parseColor("#EAEEEA"),
        bgSidebar   = Color.parseColor("#DDE5DD"),
        accent      = Color.parseColor("#2E7D32"),
        accentDim   = Color.parseColor("#C8E6C9"),
        textPrimary = Color.parseColor("#1B1B1B"),
        textSecondary = Color.parseColor("#4A4A4A"),
        textMuted   = Color.parseColor("#888888"),
        divider     = Color.parseColor("#D0D9D0"),
        border      = Color.parseColor("#B8C8B8")
    )
    val greenDark = Palette(
        bgPrimary   = Color.parseColor("#0D1410"),
        bgSurface   = Color.parseColor("#111A13"),
        bgElevated  = Color.parseColor("#172019"),
        bgSidebar   = Color.parseColor("#0A100C"),
        accent      = Color.parseColor("#66BB6A"),
        accentDim   = Color.parseColor("#1B2E1C"),
        textPrimary = Color.parseColor("#E0EBE1"),
        textSecondary = Color.parseColor("#9EB59F"),
        textMuted   = Color.parseColor("#557055"),
        divider     = Color.parseColor("#1E2E1F"),
        border      = Color.parseColor("#2A3D2B")
    )
    val blueLight = Palette(
        bgPrimary   = Color.parseColor("#F3F5F8"),
        bgSurface   = Color.parseColor("#FFFFFF"),
        bgElevated  = Color.parseColor("#E8EDF4"),
        bgSidebar   = Color.parseColor("#D8E2EF"),
        accent      = Color.parseColor("#1565C0"),
        accentDim   = Color.parseColor("#BBDEFB"),
        textPrimary = Color.parseColor("#1A1A2E"),
        textSecondary = Color.parseColor("#3A4A5C"),
        textMuted   = Color.parseColor("#7A8A9A"),
        divider     = Color.parseColor("#CBD5E0"),
        border      = Color.parseColor("#B0C0D5")
    )
    val blueDark = Palette(
        bgPrimary   = Color.parseColor("#0A0E18"),
        bgSurface   = Color.parseColor("#0E1420"),
        bgElevated  = Color.parseColor("#141C2A"),
        bgSidebar   = Color.parseColor("#080C16"),
        accent      = Color.parseColor("#5C9CE6"),
        accentDim   = Color.parseColor("#162040"),
        textPrimary = Color.parseColor("#DCE8F5"),
        textSecondary = Color.parseColor("#8AAAC8"),
        textMuted   = Color.parseColor("#446080"),
        divider     = Color.parseColor("#1A2438"),
        border      = Color.parseColor("#243048")
    )
    val redLight = Palette(
        bgPrimary   = Color.parseColor("#F8F3F3"),
        bgSurface   = Color.parseColor("#FFFFFF"),
        bgElevated  = Color.parseColor("#F0E8E8"),
        bgSidebar   = Color.parseColor("#EAD8D8"),
        accent      = Color.parseColor("#B71C1C"),
        accentDim   = Color.parseColor("#FFCDD2"),
        textPrimary = Color.parseColor("#1E0E0E"),
        textSecondary = Color.parseColor("#5C3A3A"),
        textMuted   = Color.parseColor("#9A6A6A"),
        divider     = Color.parseColor("#E0CECE"),
        border      = Color.parseColor("#D0B8B8")
    )
    val redDark = Palette(
        bgPrimary   = Color.parseColor("#160A0A"),
        bgSurface   = Color.parseColor("#1E0E0E"),
        bgElevated  = Color.parseColor("#261414"),
        bgSidebar   = Color.parseColor("#100606"),
        accent      = Color.parseColor("#EF5350"),
        accentDim   = Color.parseColor("#3E1414"),
        textPrimary = Color.parseColor("#F5E0E0"),
        textSecondary = Color.parseColor("#C09090"),
        textMuted   = Color.parseColor("#805050"),
        divider     = Color.parseColor("#2E1818"),
        border      = Color.parseColor("#3E2020")
    )
    val yellowLight = Palette(
        bgPrimary   = Color.parseColor("#F8F6EE"),
        bgSurface   = Color.parseColor("#FFFFFF"),
        bgElevated  = Color.parseColor("#F2EDD8"),
        bgSidebar   = Color.parseColor("#EAE4CC"),
        accent      = Color.parseColor("#E65100"),
        accentDim   = Color.parseColor("#FFF8E1"),
        textPrimary = Color.parseColor("#1E1800"),
        textSecondary = Color.parseColor("#5C4A10"),
        textMuted   = Color.parseColor("#9A8050"),
        divider     = Color.parseColor("#E0D8BC"),
        border      = Color.parseColor("#D0C8A4")
    )
    val yellowDark = Palette(
        bgPrimary   = Color.parseColor("#141000"),
        bgSurface   = Color.parseColor("#1C1600"),
        bgElevated  = Color.parseColor("#221C00"),
        bgSidebar   = Color.parseColor("#0E0C00"),
        accent      = Color.parseColor("#FFB300"),
        accentDim   = Color.parseColor("#2E2200"),
        textPrimary = Color.parseColor("#F5EDD0"),
        textSecondary = Color.parseColor("#C0A860"),
        textMuted   = Color.parseColor("#806830"),
        divider     = Color.parseColor("#2A2000"),
        border      = Color.parseColor("#3A2E00")
    )
}
