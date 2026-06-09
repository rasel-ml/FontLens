package com.fontlens.ui.preview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private var isBold   = false
    private var isItalic = false
    private var fontSize = 32

    // True when this fragment lives inside FontPreviewActivity (standalone)
    private val isStandalone get() =
        findNavController().graph.id == R.id.nav_preview

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId)
            ?: run { findNavController().popBackStack(); return }
        val tf       = FontLoader.getTypeface(font.id)
        val tempMode = args.tempMode

        binding.tvFontName.text = font.effectiveMeta.family.ifEmpty { font.displayName }

        // Back — if standalone, finish the activity; otherwise pop back stack
        binding.toolbar.setNavigationOnClickListener {
            if (isStandalone) requireActivity().finish()
            else findNavController().popBackStack()
        }

        // Add to Library button
        if (tempMode && !FontRepository.isInLibrary(font.id)) {
            binding.btnAddToLibrary.visibility = View.VISIBLE
            binding.btnAddToLibrary.setOnClickListener {
                FontRepository.promoteToLibrary(font.id, requireContext())
                binding.btnAddToLibrary.visibility = View.GONE
                // If standalone, offer to open in main app
                if (isStandalone) {
                    Toast.makeText(requireContext(), "Added to library", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), com.fontlens.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Added to library", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnAddToLibrary.visibility = View.GONE
        }

        // Hide fav in temp mode
        binding.btnFavorite.visibility = if (tempMode) View.GONE else View.VISIBLE

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
            binding.btnBold.setBackgroundResource(if (isBold) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
            binding.btnItalic.setBackgroundResource(if (isItalic) R.drawable.bg_style_btn_active else R.drawable.bg_style_btn)
        }
        binding.btnBold.setOnClickListener   { isBold   = !isBold;   updateStyle() }
        binding.btnItalic.setOnClickListener { isItalic = !isItalic; updateStyle() }

        // Sub-screen navigation — use correct action IDs based on which graph we're in
        binding.btnGlyph.setOnClickListener {
            if (isStandalone)
                findNavController().navigate(R.id.action_standalone_to_glyph,
                    Bundle().apply { putString("fontId", font.id) })
            else
                findNavController().navigate(PreviewFragmentDirections.actionPreviewToGlyph(font.id))
        }
        binding.btnMeta.setOnClickListener {
            if (isStandalone)
                findNavController().navigate(R.id.action_standalone_to_meta,
                    Bundle().apply { putString("fontId", font.id) })
            else
                findNavController().navigate(PreviewFragmentDirections.actionPreviewToMeta(font.id))
        }
        binding.btnInfo.setOnClickListener {
            if (isStandalone)
                findNavController().navigate(R.id.action_standalone_to_info,
                    Bundle().apply { putString("fontId", font.id) })
            else
                findNavController().navigate(PreviewFragmentDirections.actionPreviewToInfo(font.id))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
