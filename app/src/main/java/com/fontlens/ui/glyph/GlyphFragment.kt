package com.fontlens.ui.glyph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentGlyphBinding

class GlyphFragment : Fragment() {

    private var _binding: FragmentGlyphBinding? = null
    private val binding get() = _binding!!
    private val args: GlyphFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlyphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val font = FontRepository.getById(args.fontId) ?: run {
            findNavController().popBackStack(); return
        }
        GlyphUiHelper(binding, font) { findNavController().popBackStack() }.setup()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
