package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentGlyphBinding
import com.fontlens.ui.glyph.GlyphUiHelper

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
        GlyphUiHelper(binding, font) {
            requireActivity().supportFragmentManager.popBackStack()
        }.setup()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
