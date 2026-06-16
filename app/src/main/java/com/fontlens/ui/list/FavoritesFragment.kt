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
import com.fontlens.data.FontItem
import com.fontlens.data.FontListItem
import android.view.ContextThemeWrapper
import com.fontlens.data.FontRepository
import com.fontlens.utils.ThemeManager
import com.fontlens.databinding.FragmentFontListBinding
import com.fontlens.ui.DeleteFontDialog
import com.fontlens.utils.StorageDeleteHelper

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter
    private lateinit var storageDeleteHelper: StorageDeleteHelper
    private var pendingDeleteFontId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = getString(R.string.favorites)
        binding.fabAdd.visibility    = View.GONE
        binding.btnHamburger.visibility = View.GONE
        binding.btnSort.visibility   = View.GONE
        binding.btnTheme.visibility  = View.GONE
        binding.searchLayout.visibility = View.VISIBLE
        binding.ivEmptyIcon.setImageResource(com.fontlens.R.drawable.ic_star_sad)
        binding.ivEmptyIcon.isClickable = false

        storageDeleteHelper = StorageDeleteHelper(this) { success ->
            if (success) {
                pendingDeleteFontId?.let { id ->
                    FontRepository.removeFont(id, requireContext())
                    refresh(binding.etSearch.text?.toString() ?: "")
                    android.widget.Toast.makeText(requireContext(), "File deleted", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Delete cancelled or failed", android.widget.Toast.LENGTH_SHORT).show()
            }
            pendingDeleteFontId = null
        }

        adapter = FontListAdapter(
            onFontClick = { font ->
                findNavController().navigate(FavoritesFragmentDirections.actionFavToPreview(font.id))
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                refresh(binding.etSearch.text?.toString() ?: "")
            },
            onRemoveClick = { font ->
                DeleteFontDialog.show(
                    context = requireContext(),
                    font = font,
                    onRemoveFromLibrary = { refresh(binding.etSearch.text?.toString() ?: "") },
                    onDeletePermanently = {
                        pendingDeleteFontId = font.id
                        storageDeleteHelper.requestDelete(font.uri)
                    }
                )
            },
            isFavorite = { FontRepository.isFavorite(it) },
            getSample  = { FontRepository.getSampleText(it) },
            onSelectionChanged = { ids -> updateSelectionToolbar(ids) }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }

        // Selection toolbar actions
        binding.btnCancelSelection.setOnClickListener {
            adapter.exitSelectionMode(); showNormalToolbar()
        }
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll(FontRepository.getFavorites())
            updateSelectionToolbar(adapter.getSelectedIds())
        }
        binding.btnSelFavorite.setOnClickListener {
            // In favorites, this removes from favorites
            val ids = adapter.getSelectedIds()
            ids.forEach { id ->
                if (FontRepository.isFavorite(id))
                    FontRepository.toggleFavorite(id, requireContext())
            }
            Toast.makeText(requireContext(), "${ids.size} removed from favorites", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
        }
        binding.btnSelDelete.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(ContextThemeWrapper(requireContext(), ThemeManager.currentThemeResId(requireContext())))
                .setTitle("Delete ${ids.size} font(s)?")
                .setMessage("Choose how to remove the selected fonts.")
                .setPositiveButton("🗑 Delete from Storage") { _, _ ->
                    AlertDialog.Builder(ContextThemeWrapper(requireContext(), ThemeManager.currentThemeResId(requireContext())))
                        .setTitle("⚠ Permanently delete ${ids.size} font(s)?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            val uris = ids.mapNotNull { id -> FontRepository.getById(id)?.uri }
                            pendingDeleteFontId = null // batch mode — handled in callback
                            // Remove from library first so UI feels instant
                            ids.forEach { FontRepository.removeFont(it, requireContext()) }
                            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
                            // Then request actual storage deletion
                            storageDeleteHelper.requestDeleteMultiple(uris)
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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (adapter.selectionMode) {
                        adapter.exitSelectionMode()
                        showNormalToolbar()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun showNormalToolbar() {
        binding.toolbar.visibility          = View.VISIBLE
        binding.toolbarSelection.visibility = View.GONE
    }

    private fun updateSelectionToolbar(ids: Set<String>) {
        val total = FontRepository.getFavorites().size
        binding.toolbar.visibility          = View.GONE
        binding.toolbarSelection.visibility = View.VISIBLE
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
