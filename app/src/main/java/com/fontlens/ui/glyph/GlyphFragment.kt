package com.fontlens.ui.glyph

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentGlyphBinding
import com.fontlens.utils.TypefaceLoader

class GlyphFragment : Fragment() {

    private var _binding: FragmentGlyphBinding? = null
    private val binding get() = _binding!!
    private val args: GlyphFragmentArgs by navArgs()

    private val PAGE_SIZE = 200
    private var currentPage = 0
    private var allChars = listOf<Int>()
    private var filteredChars = listOf<Int>()
    private lateinit var adapter: GlyphAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGlyphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId) ?: run { findNavController().popBackStack(); return }
        val tf = TypefaceLoader.getTypeface(font.id)
        val showAll = FontRepository.settings.glyphShowAll

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // Build char list
        allChars = if (showAll) {
            (32 until 65536).toList()
        } else {
            font.meta.supportedChars.filter { it >= 32 }.ifEmpty { (32 until 127).toList() }
        }
        filteredChars = allChars

        adapter = GlyphAdapter(tf) { /* glyph tap — could navigate back or show detail */ }

        binding.rvGlyphs.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.rvGlyphs.adapter = adapter

        binding.etSearch.addTextChangedListener { q ->
            val text = q?.toString() ?: ""
            filteredChars = if (text.isBlank()) allChars
            else allChars.filter {
                it.toString(16).contains(text, ignoreCase = true) ||
                String(Character.toChars(it)).contains(text)
            }
            currentPage = 0
            renderPage()
        }

        binding.btnPrev.setOnClickListener { if (currentPage > 0) { currentPage--; renderPage() } }
        binding.btnNext.setOnClickListener {
            val maxPage = ((filteredChars.size - 1) / PAGE_SIZE)
            if (currentPage < maxPage) { currentPage++; renderPage() }
        }

        renderPage()
    }

    private fun renderPage() {
        val start = currentPage * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, filteredChars.size)
        val slice = filteredChars.subList(start, end)

        val totalPages = maxOf(1, (filteredChars.size + PAGE_SIZE - 1) / PAGE_SIZE)
        binding.tvPage.text = getString(R.string.progress_format, currentPage + 1, totalPages)
        binding.tvGlyphCount.text = getString(com.fontlens.R.string.glyph_count, filteredChars.size)

        binding.btnPrev.alpha = if (currentPage > 0) 1f else 0.3f
        binding.btnNext.alpha = if (currentPage < totalPages - 1) 1f else 0.3f

        adapter.setData(slice)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
