package com.fontlens.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.data.FontRepository
import com.fontlens.data.defaultLangSamples
import com.fontlens.data.scriptDisplayName
import com.fontlens.databinding.FragmentSampleManagerBinding
import com.fontlens.databinding.ItemSampleScriptBinding

class SampleManagerFragment : Fragment() {

    private var _binding: FragmentSampleManagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSampleManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val adapter = ScriptSampleAdapter(buildItems()) { updatedItems ->
            // Persist: update both langSamples texts and scriptOrder
            val newSamples = updatedItems.associate { it.code to it.sampleText }
            val newOrder   = updatedItems.map { it.code }
            FontRepository.settings = FontRepository.settings.copy(
                langSamples = newSamples,
                scriptOrder = newOrder
            )
            FontRepository.saveSettings(requireContext())
        }

        binding.rvScripts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScripts.adapter = adapter

        // Attach drag-to-reorder
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.moveItem(vh.adapterPosition, target.adapterPosition)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                // Elevate card while dragging
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    vh?.itemView?.alpha = 0.85f
                    vh?.itemView?.scaleX = 1.02f
                    vh?.itemView?.scaleY = 1.02f
                }
            }
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.alpha  = 1f
                vh.itemView.scaleX = 1f
                vh.itemView.scaleY = 1f
            }
        })
        touchHelper.attachToRecyclerView(binding.rvScripts)
        adapter.touchHelper = touchHelper
    }

    /** Build ordered list from current settings, ensuring all 29 scripts are present. */
    private fun buildItems(): MutableList<ScriptItem> {
        val s = FontRepository.settings
        val defaults = defaultLangSamples()
        // Start from user's saved order, then append any missing codes
        val allCodes = (s.scriptOrder + defaults.keys).distinct()
        return allCodes.map { code ->
            ScriptItem(
                code       = code,
                sampleText = s.langSamples[code] ?: defaults[code] ?: ""
            )
        }.toMutableList()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Data ──────────────────────────────────────────────────────────────────────

data class ScriptItem(val code: String, var sampleText: String)

// ── Adapter ───────────────────────────────────────────────────────────────────

class ScriptSampleAdapter(
    private val items: MutableList<ScriptItem>,
    private val onChanged: (List<ScriptItem>) -> Unit
) : RecyclerView.Adapter<ScriptSampleAdapter.VH>() {

    var touchHelper: ItemTouchHelper? = null

    inner class VH(val b: ItemSampleScriptBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSampleScriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.b

        b.tvScriptName.text = scriptDisplayName(item.code)
        b.tvScriptCode.text = item.code.uppercase()
        b.etSampleText.setText(item.sampleText)

        // Save text on focus change
        b.etSampleText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_ID.toInt()) {
                    items[pos] = items[pos].copy(sampleText = b.etSampleText.text?.toString() ?: "")
                    onChanged(items.toList())
                }
            }
        }

        // Restore default
        b.btnRestoreDefault.setOnClickListener {
            val defaults = defaultLangSamples()
            val default  = defaults[item.code] ?: ""
            b.etSampleText.setText(default)
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) {
                items[pos] = items[pos].copy(sampleText = default)
                onChanged(items.toList())
            }
        }

        // Start drag on handle touch
        b.ivDragHandle.setOnTouchListener { _, _ ->
            touchHelper?.startDrag(holder)
            false
        }
    }

    fun moveItem(from: Int, to: Int) {
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
        onChanged(items.toList())
    }
}
