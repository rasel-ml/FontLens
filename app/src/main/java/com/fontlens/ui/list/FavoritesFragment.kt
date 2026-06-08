package com.fontlens.ui.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontListBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = getString(R.string.favorites)
        binding.fabAdd.visibility = View.GONE
        binding.searchLayout.visibility = View.VISIBLE

        adapter = FontListAdapter(
            onFontClick = { font ->
                findNavController().navigate(
                    FavoritesFragmentDirections.actionFavToPreview(font.id)
                )
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                refresh(binding.etSearch.text?.toString() ?: "")
            },
            onRemoveClick = { font ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove from Library")
                    .setMessage("Remove \"${font.effectiveMeta.family.ifEmpty { font.displayName }}\" from the library?\n\nThe file will NOT be deleted from storage.")
                    .setPositiveButton("Remove") { _, _ ->
                        FontRepository.removeFont(font.id, requireContext())
                        refresh(binding.etSearch.text?.toString() ?: "")
                        Toast.makeText(requireContext(), "Removed from library", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onRemoveLongClick = { font ->
                AlertDialog.Builder(requireContext())
                    .setTitle("⚠ Delete from Storage")
                    .setMessage("Permanently delete \"${font.effectiveMeta.family.ifEmpty { font.displayName }}\" from your device?\n\nThis cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        val deleted = FontRepository.removeFontFromStorage(font.id, requireContext())
                        refresh(binding.etSearch.text?.toString() ?: "")
                        val msg = if (deleted) "File deleted from storage"
                                  else "Removed from library (file could not be deleted)"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            isFavorite = { FontRepository.isFavorite(it) },
            getSample  = { FontRepository.getSampleText(it) }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }
        refresh()
    }

    private fun refresh(query: String = "") {
        val favs = FontRepository.getFavorites()
        val filtered = if (query.isBlank()) favs
        else favs.filter { it.displayName.contains(query, ignoreCase = true) }
        binding.tvCount.text = filtered.size.toString()
        binding.tvEmpty.text = getString(R.string.no_favorites)
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility     = if (filtered.isEmpty()) View.GONE   else View.VISIBLE
        adapter.submitList(filtered)
    }

    override fun onResume() { super.onResume(); refresh(binding.etSearch.text?.toString() ?: "") }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
