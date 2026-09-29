package com.fontlens

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fontlens.data.FontRepository
import com.fontlens.databinding.ActivityFontPreviewBinding
import com.fontlens.utils.FontLoader
import com.fontlens.utils.ThemeManager
import kotlinx.coroutines.launch

class FontPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFontPreviewBinding

    override fun attachBaseContext(newBase: Context) {
        FontRepository.load(newBase)
        val s = FontRepository.settings
        ThemeManager.applyTheme(s)
        ThemeManager.applyNightMode(s)
        super.attachBaseContext(android.view.ContextThemeWrapper(newBase, ThemeManager.themeResId(s)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.themeResId(FontRepository.settings))
        super.onCreate(savedInstanceState)

        binding = ActivityFontPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = intent.data
        if (uri == null) { finish(); return }

        val name     = getFileNameFromUri(uri)
        val ext      = name.substringAfterLast(".").lowercase()
        val fontExts = setOf("ttf", "otf", "woff", "woff2", "ttc")
        val mimeType = intent.type ?: contentResolver.getType(uri) ?: ""
        val isFontMime = mimeType.startsWith("font/") || mimeType.contains("font") ||
                (mimeType == "application/octet-stream" && ext in fontExts)

        if (!isFontMime && ext !in fontExts) {
            Toast.makeText(this, "Not a supported font file", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        lifecycleScope.launch {
            val items = FontLoader.loadFontsFromUris(this@FontPreviewActivity, listOf(uri))
            if (items.isEmpty()) {
                Toast.makeText(this@FontPreviewActivity, "Could not load font", Toast.LENGTH_SHORT).show()
                finish(); return@launch
            }
            val font = items.first()
            FontRepository.addTempFont(font)

            // Load typeface into cache NOW so PreviewFragment and GlyphUiHelper find it
            com.fontlens.utils.TypefaceLoader.loadSingle(this@FontPreviewActivity, font.id, font.uri)

            supportFragmentManager.beginTransaction()
                .replace(R.id.preview_container,
                    com.fontlens.ui.preview.StandalonePreviewFragment().apply {
                        arguments = Bundle().apply { putString("fontId", font.id) }
                    })
                .commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
        else super.onBackPressed()
    }

    private fun getFileNameFromUri(uri: android.net.Uri): String {
        var name = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx) ?: ""
        }
        return name.ifEmpty { uri.lastPathSegment ?: "" }
    }
}
