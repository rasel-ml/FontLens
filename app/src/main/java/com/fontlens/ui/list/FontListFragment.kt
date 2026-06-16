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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import android.view.ContextThemeWrapper
import com.fontlens.utils.FontLoader
import com.fontlens.utils.ThemeManager
import com.fontlens.utils.StorageDeleteHelper
import com.fontlens.utils.TypefaceLoader
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FontListFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter
    private lateinit var storageDeleteHelper: StorageDeleteHelper

    private var initialLoadDone    = false
    private var currentSort        = SortOrder.NAME_ASC
    private var typefaceJobRunning = false
    private var pendingDeleteId: String? = null

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
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

        storageDeleteHelper = StorageDeleteHelper(this) { success ->
            val id = pendingDeleteId
            pendingDeleteId = null
            if (success && id != null) {
                FontRepository.removeFont(id, requireContext())
                refresh()
                Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
            } else if (!success) {
                Toast.makeText(requireContext(), "Delete cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = FontListAdapter(
            onFontClick     = { font -> findNavController().navigate(FontListFragmentDirections.actionListToPreview(font.id)) },
            onFavoriteClick = { font -> FontRepository.toggleFavorite(font.id, requireContext()); adapter.notifyDataSetChanged() },
            onRemoveClick   = { font ->
                DeleteFontDialog.show(
                    context = requireContext(),
                    font = font,
                    onRemoveFromLibrary = { refresh() },
                    onDeletePermanently = {
                        pendingDeleteId = font.id
                        storageDeleteHelper.requestDelete(font.uri)
                    }
                )
            },
            isFavorite      = { FontRepository.isFavorite(it) },
            getSample       = { FontRepository.getSampleText(it) },
            onSelectionChanged = { ids ->
                if (ids.isEmpty() && adapter.selectionMode) {
                    adapter.exitSelectionMode()
                    showNormalToolbar()
                } else {
                    updateSelectionToolbar(ids)
                }
            }
        )

        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter

        binding.btnHamburger.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
        binding.fabAdd.setOnClickListener { openFolderPicker() }
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }
        binding.btnSort.setOnClickListener { showSortSheet() }

        // ── Theme toggle button ─────────────────────────────────────────────
        // Synced to persisted darkMode; hidden when followSystem is enabled
        applyThemeButtonState()
        binding.btnTheme.setOnClickListener {
            val newDark = !FontRepository.settings.darkMode
            FontRepository.settings = FontRepository.settings.copy(darkMode = newDark)
            FontRepository.saveSettings(requireContext())
            // Recreate to apply new theme
            requireActivity().recreate()
        }

        // Selection toolbar buttons
        binding.btnCancelSelection.setOnClickListener {
            adapter.exitSelectionMode()
            showNormalToolbar()
        }
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll(FontRepository.getAll())
            updateSelectionToolbar(adapter.getSelectedIds())
        }
        binding.btnSelFavorite.setOnClickListener {
            val ids = adapter.getSelectedIds()
            if (ids.isEmpty()) return@setOnClickListener
            ids.forEach { id ->
                if (!FontRepository.isFavorite(id)) FontRepository.toggleFavorite(id, requireContext())
            }
            Toast.makeText(requireContext(), "${ids.size} added to favorites", Toast.LENGTH_SHORT).show()
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
                            val uris = ids.mapNotNull { FontRepository.getById(it)?.uri }
                            ids.forEach { FontRepository.removeFont(it, requireContext()) }
                            adapter.exitSelectionMode(); showNormalToolbar(); refresh()
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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
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

        if (!initialLoadDone) {
            initialLoadDone = true
            val cached = FontRepository.getAll()
            if (cached.isNotEmpty()) {
                refresh()
                startBackgroundTypefaceLoading(cached)
                scanFoldersForNewFonts()
            } else {
                reloadSavedFolders()
            }
        } else {
            refresh()
        }
    }

    // ── Theme button state ─────────────────────────────────────────────────────

    private fun applyThemeButtonState() {
        val s = FontRepository.settings
        if (s.followSystem) {
            // Hide the button entirely — system handles dark/light
            binding.btnTheme.visibility = View.GONE
        } else {
            binding.btnTheme.visibility = View.VISIBLE
            binding.btnTheme.text = if (s.darkMode) "🌙" else "☀"
        }
    }

    // ── Toolbar helpers ────────────────────────────────────────────────────────

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

    // ── Sort ───────────────────────────────────────────────────────────────────

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

    // ── Font loading ───────────────────────────────────────────────────────────

    private fun reloadSavedFolders() {
        val newUris = FontRepository.getSavedFolderUris().filter { !FontRepository.isFolderLoaded(it) }
        if (newUris.isEmpty()) { refresh(); return }
        lifecycleScope.launch {
            for (uri in newUris) try { loadFontsFromFolder(uri, showToast = false) } catch (_: Exception) {}
        }
    }

    private fun scanFoldersForNewFonts() {
        val uris = FontRepository.getSavedFolderUris()
        if (uris.isEmpty()) return
        lifecycleScope.launch {
            val recursive = FontRepository.settings.folderRecursive
            for (folderUri in uris) {
                try {
                    val folderLabel = "/" + (folderUri.lastPathSegment ?: "").substringAfter(":")
                    val fontUris = withContext(Dispatchers.IO) { collectFontUris(folderUri, recursive) }
                    val items = FontLoader.loadFontsFromUris(requireContext(), fontUris, folderLabel)
                    val before = FontRepository.getAll().size
                    FontRepository.addFontsAndSave(items, requireContext())
                    if (FontRepository.getAll().size > before) {
                        refresh()
                        startBackgroundTypefaceLoading(FontRepository.getAll())
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun startBackgroundTypefaceLoading(fonts: List<FontItem>) {
        if (typefaceJobRunning) return
        typefaceJobRunning = true
        var count = 0
        lifecycleScope.launch {
            TypefaceLoader.loadSequentially(
                requireContext(),
                fonts.filter { !TypefaceLoader.isLoaded(it.id) }.map { it.id to it.uri }
            ) { fontId ->
                if (_binding == null) return@loadSequentially
                count++
                adapter.notifyTypefaceReady(fontId)
                if (count % 10 == 0) adapter.notifyDataSetChanged()
            }
            if (_binding != null) adapter.notifyDataSetChanged()
            typefaceJobRunning = false
        }
    }

    fun reloadFolder(uri: Uri) { loadFontsFromFolder(uri, showToast = true) }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        pickFolder.launch(intent)
    }

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
            startBackgroundTypefaceLoading(FontRepository.getAll())
        }
    }

    private fun collectFontUris(folderUri: Uri, recursive: Boolean): List<Uri> {
        val cr = requireContext().contentResolver
        val fontExts = setOf("ttf", "otf", "woff", "woff2", "ttc")
        val result = mutableListOf<Uri>()
        fun scan(treeUri: Uri, docId: String) {
            val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            cr.query(childUri, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null)?.use { cursor ->
                val idCol   = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idCol)   ?: continue
                    val name    = cursor.getString(nameCol) ?: continue
                    val mime    = cursor.getString(mimeCol) ?: ""
                    val ext     = name.substringAfterLast(".").lowercase()
                    when {
                        ext in fontExts -> result.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
                        recursive && mime == DocumentsContract.Document.MIME_TYPE_DIR -> scan(treeUri, childId)
                    }
                }
            }
        }
        scan(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
        return result
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

    override fun onResume() {
        super.onResume()
        if (initialLoadDone) {
            applyThemeButtonState() // re-check in case settings changed in Settings tab
            refresh()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
