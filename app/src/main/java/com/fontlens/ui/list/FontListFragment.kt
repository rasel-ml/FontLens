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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontListBinding
import com.fontlens.ui.LoadingDialog
import com.fontlens.utils.FontLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FontListFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter
    private var initialLoadDone = false

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            FontRepository.saveFolderUri(uri, requireContext())
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
            onFontClick = { font ->
                findNavController().navigate(
                    FontListFragmentDirections.actionListToPreview(font.id)
                )
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                adapter.notifyDataSetChanged()
            },
            onRemoveClick = { font ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove from Library")
                    .setMessage("Remove \"${font.effectiveMeta.family.ifEmpty { font.displayName }}\" from the library?\n\nThe file will NOT be deleted from storage.")
                    .setPositiveButton("Remove") { _, _ ->
                        FontRepository.removeFont(font.id, requireContext())
                        refresh()
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
                        refresh()
                        val msg = if (deleted) "File deleted from storage" else "Removed from library (file could not be deleted)"
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
        binding.fabAdd.setOnClickListener { openFolderPicker() }
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }

        if (!initialLoadDone) {
            initialLoadDone = true
            reloadSavedFolders()
        } else {
            refresh()
        }
    }

    private fun reloadSavedFolders() {
        val newUris = FontRepository.getSavedFolderUris()
            .filter { !FontRepository.isFolderLoaded(it) }
        if (newUris.isEmpty()) { refresh(); return }
        lifecycleScope.launch {
            for (uri in newUris) {
                try { loadFontsFromFolder(uri, showToast = false) }
                catch (_: Exception) {}
            }
        }
    }

    private fun openFolderPicker() {
        pickFolder.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
    }

    private fun loadFontsFromFolder(folderUri: Uri, showToast: Boolean) {
        val recursive = FontRepository.settings.folderRecursive
        val loadingDialog = LoadingDialog()
        loadingDialog.show(parentFragmentManager, LoadingDialog.TAG)

        lifecycleScope.launch {
            val fontUris = withContext(Dispatchers.IO) {
                collectFontUris(folderUri, recursive)
            }
            if (fontUris.isEmpty()) {
                loadingDialog.dismissAllowingStateLoss()
                if (showToast) Toast.makeText(requireContext(), "No font files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val items = FontLoader.loadFontsFromUris(
                context = requireContext(),
                uris = fontUris,
                onProgress = { loaded, total ->
                    lifecycleScope.launch { loadingDialog.updateProgress(loaded, total) }
                }
            )
            FontRepository.addFonts(items)
            FontRepository.markFolderLoaded(folderUri)
            refresh()
            loadingDialog.dismissAllowingStateLoss()
            if (showToast) Toast.makeText(
                requireContext(), "${items.size} font(s) loaded", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun collectFontUris(folderUri: Uri, recursive: Boolean): List<Uri> {
        val cr = requireContext().contentResolver
        val fontExtensions = setOf("ttf", "otf", "woff", "woff2", "ttc")
        val result = mutableListOf<Uri>()
        fun scanFolder(treeUri: Uri, docId: String) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            cr.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ), null, null, null
            )?.use { cursor ->
                val idCol   = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idCol)   ?: continue
                    val name    = cursor.getString(nameCol) ?: continue
                    val mime    = cursor.getString(mimeCol) ?: ""
                    val ext     = name.substringAfterLast(".").lowercase()
                    when {
                        ext in fontExtensions ->
                            result.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
                        recursive && mime == DocumentsContract.Document.MIME_TYPE_DIR ->
                            scanFolder(treeUri, childId)
                    }
                }
            }
        }
        scanFolder(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
        return result
    }

    fun refresh(query: String = binding.etSearch.text?.toString() ?: "") {
        val all = FontRepository.getAll()
        val filtered = if (query.isBlank()) all
        else all.filter { it.displayName.contains(query, ignoreCase = true) }
        binding.tvCount.text = filtered.size.toString()
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility     = if (filtered.isEmpty()) View.GONE   else View.VISIBLE
        adapter.submitList(filtered)
    }

    override fun onResume() { super.onResume(); if (initialLoadDone) refresh() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
