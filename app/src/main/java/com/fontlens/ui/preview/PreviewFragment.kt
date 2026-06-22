package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentPreviewBinding
import com.fontlens.ui.DeleteFontDialog
import com.fontlens.utils.StorageDeleteHelper
import com.fontlens.utils.TypefaceLoader
import kotlinx.coroutines.launch

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private val args: PreviewFragmentArgs by navArgs()

    private lateinit var storageDeleteHelper: StorageDeleteHelper
    private var pendingDeleteFontId: String? = null
    private var isBold   = false
    private var isItalic = false
    private var fontSize = 32

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageDeleteHelper = StorageDeleteHelper(this) { success ->
            if (success) {
                pendingDeleteFontId?.let { id ->
                    FontRepository.removeFont(id, requireContext())
                    findNavController().popBackStack()
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Delete cancelled or failed", android.widget.Toast.LENGTH_SHORT).show()
            }
            pendingDeleteFontId = null
        }

        val font = FontRepository.getById(args.fontId)
            ?: run { findNavController().popBackStack(); return }
        val tempMode = args.tempMode

        binding.tvFontName.text = font.effectiveMeta.family.ifEmpty { font.displayName }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // Add to Library
        if (tempMode && !FontRepository.isInLibrary(font.id)) {
            binding.btnAddToLibrary.visibility = View.VISIBLE
            binding.btnAddToLibrary.setOnClickListener {
                FontRepository.promoteToLibrary(font.id, requireContext())
                binding.btnAddToLibrary.visibility = View.GONE
                binding.btnDelete.visibility   = View.VISIBLE
                binding.btnFavorite.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Added to library", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.btnAddToLibrary.visibility = View.GONE
        }

        binding.btnDelete.visibility =
            if (!tempMode || FontRepository.isInLibrary(font.id)) View.VISIBLE else View.GONE
        binding.btnDelete.setOnClickListener {
            DeleteFontDialog.show(
                context = requireContext(),
                font = font,
                onRemoveFromLibrary = { findNavController().popBackStack() },
                onDeletePermanently = {
                    pendingDeleteFontId = font.id
                    storageDeleteHelper.requestDelete(font.uri)
                }
            )
        }

        binding.btnFavorite.visibility =
            if (!tempMode || FontRepository.isInLibrary(font.id)) View.VISIBLE else View.GONE

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

        binding.etPreview.setText(
            args.initialSampleText.ifEmpty { FontRepository.getSampleText(font) }
        )

        // Apply typeface immediately if already loaded, otherwise load on demand
        fun applyTypeface() {
            val tf = TypefaceLoader.getTypeface(font.id) ?: return
            val style = when {
                isBold && isItalic -> android.graphics.Typeface.BOLD_ITALIC
                isBold             -> android.graphics.Typeface.BOLD
                isItalic           -> android.graphics.Typeface.ITALIC
                else               -> android.graphics.Typeface.NORMAL
            }
            binding.etPreview.setTypeface(tf, style)
        }

        if (TypefaceLoader.isLoaded(font.id)) {
            applyTypeface()
        } else {
            // Load this single font immediately — doesn't wait for background queue
            lifecycleScope.launch {
                TypefaceLoader.loadSingle(requireContext(), font.id, font.uri)
                if (_binding != null) applyTypeface()
            }
        }

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

        fun updateStyle() {
            val tf = TypefaceLoader.getTypeface(font.id) ?: return
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

        binding.btnGlyph.setOnClickListener {
            findNavController().navigate(R.id.action_preview_to_glyph, Bundle().apply { putString("fontId", font.id) })
        }
        binding.btnMeta.setOnClickListener {
            findNavController().navigate(R.id.action_preview_to_meta, Bundle().apply { putString("fontId", font.id) })
        }
        binding.btnInfo.setOnClickListener {
            findNavController().navigate(R.id.action_preview_to_info, Bundle().apply { putString("fontId", font.id) })
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
