package com.fontlens.ui.list

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.data.FontListItem
import com.fontlens.databinding.ItemFontCardBinding
import com.fontlens.databinding.ItemFolderHeaderBinding
import com.fontlens.utils.TypefaceLoader

class FontListAdapter(
    private val onFontClick: (FontItem) -> Unit,
    private val onFavoriteClick: (FontItem) -> Unit,
    private val onRemoveClick: (FontItem) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val getSample: (FontItem) -> String,
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<FontListItem, RecyclerView.ViewHolder>(DIFF) {

    private val selected = mutableSetOf<String>()
    var selectionMode = false
        private set

    fun exitSelectionMode() {
        selectionMode = false
        selected.clear()
        notifyDataSetChanged()
        // Notify AFTER clearing so updateSelectionToolbar is not called with empty set from adapter
        onSelectionChanged(emptySet())
    }

    fun selectAll(items: List<FontItem>) {
        items.forEach { selected.add(it.id) }
        onSelectionChanged(selected.toSet())
        notifyDataSetChanged()
    }

    fun getSelectedIds() = selected.toSet()

    fun notifyTypefaceReady(fontId: String) {
        val index = currentList.indexOfFirst {
            it is FontListItem.Font && it.font.id == fontId
        }
        if (index >= 0) notifyItemChanged(index, "typeface")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "typeface" && holder is FontVH) {
            val item = getItem(position)
            if (item is FontListItem.Font) {
                val tf = TypefaceLoader.getTypeface(item.font.id) ?: Typeface.DEFAULT
                holder.binding.tvPreviewLarge.typeface = tf
                holder.binding.root.alpha = 1f
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    companion object {
        const val TYPE_FONT   = 0
        const val TYPE_HEADER = 1

        val DIFF = object : DiffUtil.ItemCallback<FontListItem>() {
            override fun areItemsTheSame(a: FontListItem, b: FontListItem) = when {
                a is FontListItem.Font && b is FontListItem.Font -> a.font.id == b.font.id
                a is FontListItem.FolderHeader && b is FontListItem.FolderHeader -> a.path == b.path
                else -> false
            }
            override fun areContentsTheSame(a: FontListItem, b: FontListItem) = a == b
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FontListItem.Font         -> TYPE_FONT
        is FontListItem.FolderHeader -> TYPE_HEADER
    }

    inner class FontVH(val binding: ItemFontCardBinding) : RecyclerView.ViewHolder(binding.root)
    inner class HeaderVH(val binding: ItemFolderHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_FONT)
            FontVH(ItemFontCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            HeaderVH(ItemFolderHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FontListItem.FolderHeader -> {
                (holder as HeaderVH).binding.tvFolderHeader.text = item.path
            }
            is FontListItem.Font -> {
                val font = item.font
                val b    = (holder as FontVH).binding
                val m    = font.effectiveMeta
                val ctx  = holder.itemView.context

                b.tvFontName.text = m.family.ifEmpty { font.displayName }
                b.tvFontSub.text = buildString {
                    if (m.weightName.isNotEmpty()) append(m.weightName)
                    if (m.subfamily.isNotEmpty() && m.subfamily != "Regular") append(" · ${m.subfamily}")
                }
                b.tvFontSub.visibility = if (b.tvFontSub.text.isBlank()) View.GONE else View.VISIBLE

                val tf = TypefaceLoader.getTypeface(font.id) ?: Typeface.DEFAULT
                val sample = getSample(font).replace("\n", "  ").replace("\r", "")
                b.tvPreviewLarge.text     = sample
                b.tvPreviewLarge.typeface = tf
                b.root.alpha = if (TypefaceLoader.isLoaded(font.id)) 1f else 0.6f

                val isSelected = selected.contains(font.id)
                val p = com.fontlens.utils.ThemeManager.activePalette
                b.root.strokeColor = if (isSelected) p.accent else p.divider
                b.root.strokeWidth = if (isSelected) 2 else 1

                val favIcon = if (isFavorite(font.id)) R.drawable.ic_star else R.drawable.ic_star_outline
                val favTint = if (isFavorite(font.id)) p.accent else p.textMuted
                b.btnFavorite.setImageResource(favIcon)
                b.btnFavorite.imageTintList = android.content.res.ColorStateList.valueOf(favTint)
                b.btnFavorite.visibility = if (selectionMode) View.GONE else View.VISIBLE
                b.btnFavorite.setOnClickListener { if (!selectionMode) onFavoriteClick(font) }

                b.btnRemove.visibility = if (selectionMode) View.GONE else View.VISIBLE
                b.btnRemove.setOnClickListener { if (!selectionMode) onRemoveClick(font) }

                b.root.setOnClickListener {
                    if (selectionMode) {
                        if (selected.contains(font.id)) selected.remove(font.id)
                        else selected.add(font.id)
                        // Notify fragment — empty set triggers auto-exit in fragment
                        onSelectionChanged(selected.toSet())
                        notifyItemChanged(position)
                    } else {
                        onFontClick(font)
                    }
                }
                b.root.setOnLongClickListener {
                    if (!selectionMode) {
                        selectionMode = true
                        selected.add(font.id)
                        onSelectionChanged(selected.toSet())
                        notifyDataSetChanged()
                    }
                    true
                }
            }
        }
    }
}
