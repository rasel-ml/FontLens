package com.fontforge.ui.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontforge.R
import com.fontforge.data.FontRepository
import com.fontforge.databinding.FragmentFontListBinding
import com.fontforge.utils.FontLoader
import kotlinx.coroutines.launch

class FontListFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter

    private val pickFonts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val uris = mutableListOf<android.net.Uri>()
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } ?: data.data?.let { uris.add(it) }

            lifecycleScope.launch {
                val items = FontLoader.loadFontsFromUris(requireContext(), uris)
                FontRepository.addFonts(items)
                refresh()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FontListAdapter(
            onFontClick = { font ->
                val action = FontListFragmentDirections.actionListToPreview(font.id)
                findNavController().navigate(action)
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                adapter.notifyDataSetChanged()
            },
            isFavorite = { FontRepository.isFavorite(it) },
            getSample = { FontRepository.getSampleText(it) }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter

        binding.fabAdd.setOnClickListener { openFilePicker() }

        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }

        refresh()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "font/ttf", "font/otf", "application/x-font-ttf",
                "application/x-font-otf", "application/font-woff",
                "application/font-woff2", "application/octet-stream"
            ))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickFonts.launch(intent)
    }

    fun refresh(query: String = binding.etSearch.text?.toString() ?: "") {
        val all = FontRepository.getAll()
        val filtered = if (query.isBlank()) all
        else all.filter { it.displayName.contains(query, ignoreCase = true) }

        binding.tvCount.text = filtered.size.toString()
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(filtered)
    }

    override fun onResume() { super.onResume(); refresh() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
