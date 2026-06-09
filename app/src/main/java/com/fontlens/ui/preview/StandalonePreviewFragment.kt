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
import com.fontlens.utils.FontLoader

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
        val font   = FontRepository.getById(fontId) ?: run { requireActivity().finish(); return }
        val tf     = FontLoader.getTypeface(font.id)

        binding.tvFontName.text = font.effectiveMeta.family.ifEmpty { font.displayName }

        // Back closes the activity
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        // Add to Library button
        if (!FontRepository.isInLibrary(font.id)) {
            binding.btnAddToLibrary.visibility = View.VISIBLE
            binding.btnAddToLibrary.setOnClickListener {
                FontRepository.promoteToLibrary(font.id, requireContext())
                binding.btnAddToLibrary.visibility = View.GONE
                Toast.makeText(requireContext(), "Added to library", Toast.LENGTH_SHORT).show()
                // Open main app so user can see it in library
                startActivity(
                    Intent(requireContext(), com.fontlens.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } else {
            binding.btnAddToLibrary.visibility = View.GONE
        }

        // Hide fav/meta/glyph/info — no nav controller here, open full app instead
        binding.btnFavorite.visibility = View.GONE

        // Keep info/meta/glyph buttons but open main app for them
        binding.btnGlyph.setOnClickListener {
            FontRepository.promoteToLibrary(font.id, requireContext())
            startActivity(Intent(requireContext(), com.fontlens.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(requireContext(), "Opening in FontLens…", Toast.LENGTH_SHORT).show()
        }
        binding.btnMeta.setOnClickListener {
            FontRepository.promoteToLibrary(font.id, requireContext())
            startActivity(Intent(requireContext(), com.fontlens.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(requireContext(), "Opening in FontLens…", Toast.LENGTH_SHORT).show()
        }
        binding.btnInfo.setOnClickListener {
            FontRepository.promoteToLibrary(font.id, requireContext())
            startActivity(Intent(requireContext(), com.fontlens.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(requireContext(), "Opening in FontLens…", Toast.LENGTH_SHORT).show()
        }

        // Preview text
        binding.etPreview.setText(FontRepository.getSampleText(font))
        if (tf != null) binding.etPreview.typeface = tf

        // Size seekbar
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

        // Bold / Italic
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
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
