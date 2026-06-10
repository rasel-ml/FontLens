package com.fontlens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.fontlens.data.AppTheme
import com.fontlens.data.FontRepository
import com.fontlens.databinding.ActivityFontPreviewBinding
import com.fontlens.utils.FontLoader
import kotlinx.coroutines.launch

class FontPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFontPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved theme before showing UI
        FontRepository.load(this)
        AppCompatDelegate.setDefaultNightMode(when (FontRepository.settings.theme) {
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppTheme.DAY    -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.NIGHT  -> AppCompatDelegate.MODE_NIGHT_YES
        })

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

            supportFragmentManager.beginTransaction()
                .replace(R.id.preview_container,
                    com.fontlens.ui.preview.StandalonePreviewFragment().apply {
                        arguments = Bundle().apply { putString("fontId", font.id) }
                    })
                .commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
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
