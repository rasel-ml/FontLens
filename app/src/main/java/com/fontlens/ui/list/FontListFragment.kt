package com.fontlens.ui.list

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.MainActivity
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.data.FontListItem
import com.fontlens.data.FontRepository
import com.fontlens.data.SortOrder
import com.fontlens.databinding.BottomSheetSortBinding
import com.fontlens.databinding.FragmentFontListBinding
import com.fontlens.ui.DeleteFontDialog
import com.fontlens.ui.LoadingDialog
import com.fontlens.utils.FontLoader
import com.fontlens.utils.TypefaceLoader
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FontListFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter
    private var initialLoadDone = false
    private var currentSort = SortOrder.NAME_ASC
    private var isNightMode = false
    private var typefaceJobRunning = false

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FontRepository.saveFolderUri(uri, requireContext())
            (activity as? MainActivity)?.refreshDrawer()
            loadFontsFromFolder(uri, showToast = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FontListAdapter(
            onFontClick     = { font -> findNavController().navigate(FontListFragmentDirections.actionListToPreview(font.id)) },
            onFavoriteClick = { font -> FontRepository.toggleFavorite(font.id, requireContext()); adapter.notifyDataSetChanged() },
            onRemoveClick   = { font -> DeleteFontDialog.show(requireContext(), font) { refresh() } },
            isFavorite      = { FontRepository.isFavorite(it) },
            getSample       = { FontRepository.getSampleText(it) },
            onSelectionChanged = { ids -> updateSelectionToolbar(ids) }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter

        binding.btnHamburger.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
        binding.fabAdd.setOnClickListener { openFolderPicker() }
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }
        binding.btnSort.setOnClickListener { showSortSheet() }
        binding.btnTheme.setOnClickListener {
            isNightMode = !isNightMode
            AppCompatDelegate.setDefaultNightMode(
                if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            binding.btnTheme.text = if (isNightMode) "🌙" else "☀"
        }

        binding.btnCancelSelection.setOnClickListener { adapter.exitSelectionMode(); showNormalToolbar() }
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll(FontRepository.getAll())
            updateSelectionToolbar(adapter.getSelectedIds())
        }
        binding.btnSelFavorite.setOnClickListener {
            val ids = adapter.getSelectedIds()
            ids.forEach { id -> if (!FontRepository.isFavorite(id)) FontRepository.toggleFavorite(id, requireContext()) }
            Toast.makeText(requireContext(), "${ids.size} added to favorites", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
        }
        binding.btnSelDelete.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Delete ${ids.size} font(s)?")
                .setPositiveButton("🗑 Delete from Storage") { _, _ ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("⚠ Permanently delete ${ids.size} font(s)?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            ids.forEach { FontRepository.removeFontFromStorage(it, requireContext()) }
                            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
                        }
                        .setNegativeButton("Cancel", null).show()
                }
                .setNeutralButton("Remove from Library") { _, _ ->
                    ids.forEach { FontRepository.removeFont(it, requireContext()) }
                    adapter.exitSelectionMode(); showNormalToolbar(); refresh()
                }
                .setNegativeButton("Cancel", null).show()
        }

        if (!initialLoadDone) {
            initialLoadDone = true
            val cached = FontRepository.getAll()
            if (cached.isNotEmpty()) {
                // Instant list from cache — no loading popup
                refresh()
                startBackgroundTypefaceLoading(cached)
                // Still scan folders for new fonts in background (silently)
                scanFoldersForNewFonts()
            } else {
                // First ever launch — scan folders with loading popup
                scanFoldersWithPopup()
            }
        } else {
            refresh()
        }
    }

    /**
     * First launch or explicit reload — show loading popup, scan folders
     */
    private fun scanFoldersWithPopup() {
        val newUris = FontRepository.getSavedFolderUris().filter { !FontRepository.isFolderLoaded(it) }
        if (newUris.isEmpty()) { refresh(); return }
        lifecycleScope.launch {
            for (uri in newUris) try { loadFontsFromFolder(uri, showToast = false) } catch (_: Exception) {}
        }
    }

    /**
     * Subsequent launches — silently check for new fonts without blocking UI
     */
    private fun scanFoldersForNewFonts() {
        val uris = FontRepository.getSavedFolderUris()
        if (uris.isEmpty()) return
        lifecycleScope.launch {
            val recursive = FontRepository.settings.folderRecursive
            for (folderUri in uris) {
                try {
                    val folderLabel = "/" + (folderUri.lastPathSegment ?: "").substringAfter(":")
                    val fontUris = withContext(Dispatchers.IO) { collectFontUris(folderUri, recursive) }
                    val items = FontLoader.loadFontsFromUris(
                        context = requireContext(), uris = fontUris, folderPath = folderLabel)
                    val before = FontRepository.getAll().size
                    FontRepository.addFontsAndSave(items, requireContext())
                    val after = FontRepository.getAll().size
                    if (after > before) {
                        // New fonts found — refresh list and start loading their typefaces
                        refresh()
                        startBackgroundTypefaceLoading(FontRepository.getAll())
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Load typefaces one by one in background. Each completion updates that card.
     */
    private fun startBackgroundTypefaceLoading(fonts: List<FontItem>) {
        if (typefaceJobRunning) return
        typefaceJobRunning = true
        lifecycleScope.launch {
            val toLoad = fonts.filter { !TypefaceLoader.isLoaded(it.id) }
                .map { it.id to it.uri }
            TypefaceLoader.loadSequentially(requireContext(), toLoad) { fontId ->
                // Called on main thread after each font's typeface is ready
                if (_binding != null) adapter.notifyTypefaceReady(fontId)
            }
            typefaceJobRunning = false
        }
    }

    fun reloadFolder(uri: Uri) { loadFontsFromFolder(uri, showToast = true) }

    private fun openFolderPicker() { pickFolder.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)) }

    private fun loadFontsFromFolder(folderUri: Uri, showToast: Boolean) {
        val recursive   = FontRepository.settings.folderRecursive
        val folderLabel = "/" + (folderUri.lastPathSegment ?: "").substringAfter(":")
        val loadingDialog = LoadingDialog()
        loadingDialog.show(parentFragmentManager, LoadingDialog.TAG)
        lifecycleScope.launch {
            val fontUris = withContext(Dispatchers.IO) { collectFontUris(folderUri, recursive) }
            if (fontUris.isEmpty()) {
                loadingDialog.dismissAllowingStateLoss()
                if (showToast) Toast.makeText(requireContext(), "No font files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val items = FontLoader.loadFontsFromUris(
                context = requireContext(), uris = fontUris, folderPath = folderLabel,
                onProgress = { loaded, total -> lifecycleScope.launch { loadingDialog.updateProgress(loaded, total) } }
            )
            FontRepository.addFontsAndSave(items, requireContext())
            FontRepository.markFolderLoaded(folderUri)
            refresh()
            loadingDialog.dismissAllowingStateLoss()
            if (showToast) Toast.makeText(requireContext(), "${items.size} font(s) loaded", Toast.LENGTH_SHORT).show()
            // Start loading typefaces for new fonts
            startBackgroundTypefaceLoading(FontRepository.getAll())
        }
    }

    private fun collectFontUris(folderUri: Uri, recursive: Boolean): List<Uri> {
        val cr = requireContext().contentResolver
        val fontExtensions = setOf("ttf", "otf", "woff", "woff2", "ttc")
        val result = mutableListOf<Uri>()
        fun scanFolder(treeUri: Uri, docId: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            cr.query(childrenUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null)?.use { cursor ->
                val idCol   = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idCol) ?: continue
                    val name    = cursor.getString(nameCol) ?: continue
                    val mime    = cursor.getString(mimeCol) ?: ""
                    val ext     = name.substringAfterLast(".").lowercase()
                    when {
                        ext in fontExtensions -> result.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
                        recursive && mime == DocumentsContract.Document.MIME_TYPE_DIR -> scanFolder(treeUri, childId)
                    }
                }
            }
        }
        scanFolder(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
        return result
    }

    private fun showNormalToolbar() {
        binding.toolbarNormal.visibility    = View.VISIBLE
        binding.toolbarSelection.visibility = View.GONE
        binding.searchLayout.visibility     = View.VISIBLE
    }

    private fun updateSelectionToolbar(ids: Set<String>) {
        val total = FontRepository.getAll().size
        binding.toolbarNormal.visibility    = View.GONE
        binding.toolbarSelection.visibility = View.VISIBLE
        binding.searchLayout.visibility     = View.GONE
        binding.tvSelectedCount.text        = "${ids.size} / $total selected"
    }

    private fun showSortSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_FontLens_BottomSheet)
        val sheetBinding = BottomSheetSortBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(sheetBinding.root)
        when (currentSort) {
            SortOrder.NAME_ASC  -> sheetBinding.rbNameAsc
            SortOrder.NAME_DESC -> sheetBinding.rbNameDesc
            SortOrder.DATE_ASC  -> sheetBinding.rbDateAsc
            SortOrder.DATE_DESC -> sheetBinding.rbDateDesc
            SortOrder.FOLDER    -> sheetBinding.rbFolder
        }.isChecked = true
        sheetBinding.rgSort.setOnCheckedChangeListener { _, id ->
            currentSort = when (id) {
                R.id.rb_name_asc  -> SortOrder.NAME_ASC
                R.id.rb_name_desc -> SortOrder.NAME_DESC
                R.id.rb_date_asc  -> SortOrder.DATE_ASC
                R.id.rb_date_desc -> SortOrder.DATE_DESC
                R.id.rb_folder    -> SortOrder.FOLDER
                else              -> SortOrder.NAME_ASC
            }
            dialog.dismiss(); refresh()
        }
        dialog.show()
    }

    fun refresh(query: String = binding.etSearch.text?.toString() ?: "") {
        val all = FontRepository.getAll()
        val filtered = if (query.isBlank()) all
        else all.filter { it.displayName.contains(query, ignoreCase = true) }
        binding.tvCount.text = filtered.size.toString()
        val listItems = buildListItems(filtered)
        binding.layoutEmpty.visibility = if (listItems.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility     = if (listItems.isEmpty()) View.GONE   else View.VISIBLE
        adapter.submitList(listItems)
    }

    private fun buildListItems(fonts: List<FontItem>): List<FontListItem> {
        val sorted = when (currentSort) {
            SortOrder.NAME_ASC  -> fonts.sortedBy { it.effectiveMeta.family.ifEmpty { it.displayName }.lowercase() }
            SortOrder.NAME_DESC -> fonts.sortedByDescending { it.effectiveMeta.family.ifEmpty { it.displayName }.lowercase() }
            SortOrder.DATE_ASC  -> fonts.sortedBy { it.addedAt }
            SortOrder.DATE_DESC -> fonts.sortedByDescending { it.addedAt }
            SortOrder.FOLDER    -> fonts.sortedBy { it.folderPath }
        }
        if (currentSort != SortOrder.FOLDER) return sorted.map { FontListItem.Font(it) }
        val result = mutableListOf<FontListItem>()
        var lastFolder = ""
        sorted.forEach { font ->
            val folder = font.folderPath.ifEmpty { "/ (root)" }
            if (folder != lastFolder) { result.add(FontListItem.FolderHeader(folder)); lastFolder = folder }
            result.add(FontListItem.Font(font))
        }
        return result
    }

    override fun onResume() { super.onResume(); if (initialLoadDone) refresh() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
