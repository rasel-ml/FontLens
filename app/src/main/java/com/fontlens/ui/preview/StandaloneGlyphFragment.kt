package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentGlyphBinding
import com.fontlens.ui.glyph.GlyphUiHelper
import com.fontlens.utils.TypefaceLoader
import kotlinx.coroutines.launch

class StandaloneGlyphFragment : Fragment() {

    private var _binding: FragmentGlyphBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlyphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fontId = arguments?.getString("fontId") ?: return
        val font   = FontRepository.getById(fontId)  ?: return

        fun setup() {
            GlyphUiHelper(binding, font) {
                requireActivity().supportFragmentManager.popBackStack()
            }.setup()
        }

        if (TypefaceLoader.isLoaded(font.id)) {
            setup()
        } else {
            lifecycleScope.launch {
                TypefaceLoader.loadSingle(requireContext(), font.id, font.uri)
                if (_binding != null) setup()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
