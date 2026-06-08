package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentPreviewBinding
import com.fontlens.utils.FontLoader

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private val args: PreviewFragmentArgs by navArgs()

    private var isBold = false
    private var isItalic = false
    private var fontSize = 32

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId) ?: run {
            findNavController().popBackStack(); return
        }
        val m = font.effectiveMeta
        val tf = FontLoader.getTypeface(font.id)

        // Toolbar
        binding.tvFontName.text = m.family.ifEmpty { font.displayName }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // Favorite
        fun updateFav() {
            val fav = FontRepository.isFavorite(font.id)
            binding.btnFavorite.text = if (fav) "★" else "☆"
            binding.btnFavorite.setTextColor(
                ContextCompat.getColor(requireContext(), if (fav) R.color.accent else R.color.text_muted)
            )
        }
        updateFav()
        binding.btnFavorite.setOnClickListener {
            FontRepository.toggleFavorite(font.id, requireContext()); updateFav()
        }

        // Preview text + typeface
        val sampleText = FontRepository.getSampleText(font)
        binding.etPreview.setText(sampleText)
        if (tf != null) binding.etPreview.typeface = tf

        // Size seekbar (range 8..160, stored as offset from 8)
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

        // Bold / italic toggles
        fun updateStyle() {
            val style = when {
                isBold && isItalic -> android.graphics.Typeface.BOLD_ITALIC
                isBold             -> android.graphics.Typeface.BOLD
                isItalic           -> android.graphics.Typeface.ITALIC
                else               -> android.graphics.Typeface.NORMAL
            }
            binding.etPreview.setTypeface(tf, style)
            val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
            val mutedColor  = ContextCompat.getColor(requireContext(), R.color.text_muted)
            binding.btnBold.setTextColor(if (isBold) accentColor else mutedColor)
            binding.btnItalic.setTextColor(if (isItalic) accentColor else mutedColor)
            binding.btnBold.setBackgroundResource(if (isBold) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
            binding.btnItalic.setBackgroundResource(if (isItalic) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
        }
        binding.btnBold.setOnClickListener   { isBold   = !isBold;   updateStyle() }
        binding.btnItalic.setOnClickListener { isItalic = !isItalic; updateStyle() }

        // Navigation to sub-screens
        binding.btnGlyph.setOnClickListener {
            findNavController().navigate(PreviewFragmentDirections.actionPreviewToGlyph(font.id))
        }
        binding.btnMeta.setOnClickListener {
            findNavController().navigate(PreviewFragmentDirections.actionPreviewToMeta(font.id))
        }
        binding.btnInfo.setOnClickListener {
            findNavController().navigate(PreviewFragmentDirections.actionPreviewToInfo(font.id))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
