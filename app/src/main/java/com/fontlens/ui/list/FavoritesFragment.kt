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
import com.fontlens.data.FontListItem
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontListBinding
import com.fontlens.ui.DeleteFontDialog

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
        binding.fabAdd.visibility       = View.GONE
        binding.btnHamburger.visibility = View.GONE
        binding.btnSort.visibility      = View.GONE
        binding.btnTheme.visibility     = View.GONE

        adapter = FontListAdapter(
            onFontClick = { font ->
                findNavController().navigate(FavoritesFragmentDirections.actionFavToPreview(font.id))
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                refresh(binding.etSearch.text?.toString() ?: "")
            },
            onRemoveClick = { font ->
                DeleteFontDialog.show(requireContext(), font) {
                    refresh(binding.etSearch.text?.toString() ?: "")
                }
            },
            isFavorite = { FontRepository.isFavorite(it) },
            getSample  = { FontRepository.getSampleText(it) },
            onSelectionChanged = { ids -> updateSelectionToolbar(ids) }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }

        binding.btnCancelSelection.setOnClickListener { adapter.exitSelectionMode(); showNormalToolbar() }
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll(FontRepository.getFavorites())
            updateSelectionToolbar(adapter.getSelectedIds())
        }
        binding.btnSelFavorite.setOnClickListener {
            val ids = adapter.getSelectedIds()
            ids.forEach { id -> if (FontRepository.isFavorite(id)) FontRepository.toggleFavorite(id, requireContext()) }
            Toast.makeText(requireContext(), "${ids.size} removed from favorites", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
        }
        binding.btnSelDelete.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Delete ${ids.size} font(s)?")
                .setMessage("Choose how to remove the selected fonts.")
                .setPositiveButton("🗑 Delete from Storage") { _, _ ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("⚠ Permanently delete ${ids.size} font(s)?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            ids.forEach { FontRepository.removeFontFromStorage(it, requireContext()) }
                            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
                            Toast.makeText(requireContext(), "${ids.size} file(s) deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null).show()
                }
                .setNeutralButton("Remove from Library") { _, _ ->
                    ids.forEach { FontRepository.removeFont(it, requireContext()) }
                    adapter.exitSelectionMode(); showNormalToolbar(); refresh()
                    Toast.makeText(requireContext(), "${ids.size} removed", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null).show()
        }

        refresh()
    }

    private fun showNormalToolbar() {
        binding.toolbarNormal.visibility    = View.VISIBLE
        binding.toolbarSelection.visibility = View.GONE
        binding.searchLayout.visibility     = View.VISIBLE
    }

    private fun updateSelectionToolbar(ids: Set<String>) {
        val total = FontRepository.getFavorites().size
        binding.toolbarNormal.visibility    = View.GONE
        binding.toolbarSelection.visibility = View.VISIBLE
        binding.searchLayout.visibility     = View.GONE
        binding.tvSelectedCount.text        = "${ids.size} / $total selected"
    }

    private fun refresh(query: String = "") {
        val favs = FontRepository.getFavorites()
        val filtered = if (query.isBlank()) favs
        else favs.filter { it.displayName.contains(query, ignoreCase = true) }
        binding.tvCount.text = filtered.size.toString()
        binding.tvEmpty.text = getString(R.string.no_favorites)
        val listItems: List<FontListItem> = filtered.map { FontListItem.Font(it) }
        binding.layoutEmpty.visibility = if (listItems.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility     = if (listItems.isEmpty()) View.GONE   else View.VISIBLE
        adapter.submitList(listItems)
    }

    override fun onResume() { super.onResume(); refresh(binding.etSearch.text?.toString() ?: "") }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
