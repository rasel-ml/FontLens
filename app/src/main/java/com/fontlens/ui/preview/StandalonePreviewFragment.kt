package com.fontlens.ui.preview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentPreviewBinding
import com.fontlens.ui.DeleteFontDialog
import com.fontlens.utils.TypefaceLoader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StandalonePreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private var isBold   = false
    private var isItalic = false
    private var fontSize = 32

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fontId = arguments?.getString("fontId") ?: run { requireActivity().finish(); return }
        val font   = FontRepository.getById(fontId)  ?: run { requireActivity().finish(); return }
        val tf = TypefaceLoader.getTypeface(font.id)

        binding.tvFontName.text = font.effectiveMeta.family.ifEmpty { font.displayName }
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        // ── Add to Library ────────────────────────────────────────────────────
        fun refreshButtons() {
            val inLibrary = FontRepository.isInLibrary(font.id)
            binding.btnAddToLibrary.visibility = if (!inLibrary) View.VISIBLE else View.GONE
            binding.btnDelete.visibility       = if (inLibrary) View.VISIBLE else View.GONE
            binding.btnFavorite.visibility     = if (inLibrary) View.VISIBLE else View.GONE
        }
        refreshButtons()

        binding.btnAddToLibrary.setOnClickListener {
            FontRepository.promoteToLibrary(font.id, requireContext())
            refreshButtons()
            Toast.makeText(requireContext(), "Added to library", Toast.LENGTH_SHORT).show()
        }

        // ── Delete (only when in library) ─────────────────────────────────────
        binding.btnDelete.setOnClickListener {
            DeleteFontDialog.show(requireContext(), font) {
                requireActivity().finish()
            }
        }

        // ── Favorite ──────────────────────────────────────────────────────────
        fun updateFav() {
            val fav = FontRepository.isFavorite(font.id)
            val p = com.fontlens.utils.ThemeManager.activePalette
            binding.btnFavorite.setImageResource(
                if (fav) R.drawable.ic_star else R.drawable.ic_star_outline)
            binding.btnFavorite.imageTintList =
                android.content.res.ColorStateList.valueOf(if (fav) p.accent else p.textMuted)
        }
        updateFav()
        binding.btnFavorite.setOnClickListener {
            FontRepository.toggleFavorite(font.id, requireContext()); updateFav()
        }

        // ── Preview text ──────────────────────────────────────────────────────
        binding.etPreview.setText(FontRepository.getSampleText(font))
        if (tf != null) binding.etPreview.typeface = tf

        // ── Size seekbar ──────────────────────────────────────────────────────
        fontSize = 32
        binding.seekbarSize.max = 152
        binding.seekbarSize.progress = fontSize - 8
        binding.tvSizeLabel.text = "${fontSize}px"
        binding.etPreview.textSize = fontSize.toFloat()
        binding.seekbarSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                fontSize = p + 8
                binding.tvSizeLabel.text = "${fontSize}px"
                binding.etPreview.textSize = fontSize.toFloat()
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        // ── Bold / Italic ─────────────────────────────────────────────────────
        fun updateStyle() {
            val style = when {
                isBold && isItalic -> android.graphics.Typeface.BOLD_ITALIC
                isBold             -> android.graphics.Typeface.BOLD
                isItalic           -> android.graphics.Typeface.ITALIC
                else               -> android.graphics.Typeface.NORMAL
            }
            binding.etPreview.setTypeface(tf, style)
            val accent = ContextCompat.getColor(requireContext(), R.color.accent)
            val muted  = ContextCompat.getColor(requireContext(), R.color.text_muted)
            binding.btnBold.setTextColor(if (isBold) accent else muted)
            binding.btnItalic.setTextColor(if (isItalic) accent else muted)
            binding.btnBold.setBackgroundResource(
                if (isBold) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
            binding.btnItalic.setBackgroundResource(
                if (isItalic) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
        }
        binding.btnBold.setOnClickListener   { isBold   = !isBold;   updateStyle() }
        binding.btnItalic.setOnClickListener { isItalic = !isItalic; updateStyle() }

        // ── Sub-screens ───────────────────────────────────────────────────────
        binding.btnGlyph.setOnClickListener {
            openSubFragment(StandaloneGlyphFragment().apply {
                arguments = Bundle().apply { putString("fontId", font.id) }
            })
        }
        binding.btnMeta.setOnClickListener {
            openSubFragment(StandaloneMetaFragment().apply {
                arguments = Bundle().apply { putString("fontId", font.id) }
            })
        }
        binding.btnInfo.setOnClickListener {
            openSubFragment(StandaloneInfoFragment().apply {
                arguments = Bundle().apply { putString("fontId", font.id) }
            })
        }
    }

    private fun openSubFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.preview_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
