package com.fontlens.ui.list

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontListBinding
import com.fontlens.utils.FontLoader
import kotlinx.coroutines.launch

class FontListFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadFontsFromFolder(uri)
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
        binding.fabAdd.setOnClickListener { openFolderPicker() }
        binding.etSearch.addTextChangedListener { refresh(it?.toString() ?: "") }
        refresh()
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        pickFolder.launch(intent)
    }

    private fun loadFontsFromFolder(folderUri: Uri) {
        val cr = requireContext().contentResolver
        val docId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId)

        val fontUris = mutableListOf<Uri>()
        val fontExtensions = setOf("ttf", "otf", "woff", "woff2", "ttc")

        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                val ext  = name.substringAfterLast(".").lowercase()
                if (ext in fontExtensions) {
                    val childDocId = cursor.getString(idCol)
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, childDocId)
                    fontUris.add(fileUri)
                }
            }
        }

        if (fontUris.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "No font files found in this folder",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val items = FontLoader.loadFontsFromUris(requireContext(), fontUris)
            FontRepository.addFonts(items)
            refresh()
            android.widget.Toast.makeText(
                requireContext(),
                "${items.size} font(s) loaded",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
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

    override fun onResume() { super.onResume(); refresh() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
