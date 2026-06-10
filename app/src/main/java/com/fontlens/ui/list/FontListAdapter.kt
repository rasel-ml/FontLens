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
import com.fontlens.utils.FontLoader

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

    fun enterSelectionMode() { selectionMode = true; notifyDataSetChanged() }

    fun exitSelectionMode() {
        selectionMode = false
        selected.clear()
        onSelectionChanged(emptySet())
        notifyDataSetChanged()
    }

    fun selectAll(items: List<FontItem>) {
        items.forEach { selected.add(it.id) }
        onSelectionChanged(selected.toSet())
        notifyDataSetChanged()
    }

    fun getSelectedIds() = selected.toSet()

    companion object {
        const val TYPE_FONT   = 0
        const val TYPE_HEADER = 1

        val DIFF = object : DiffUtil.ItemCallback<FontListItem>() {
            override fun areItemsTheSame(a: FontListItem, b: FontListItem): Boolean = when {
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
                val b = (holder as FontVH).binding
                val m = font.effectiveMeta
                val ctx = holder.itemView.context

                b.tvFontName.text = m.family.ifEmpty { font.displayName }
                b.tvFontSub.text = buildString {
                    if (m.weightName.isNotEmpty()) append(m.weightName)
                    if (m.subfamily.isNotEmpty() && m.subfamily != "Regular") append(" · ${m.subfamily}")
                }
                b.tvFontSub.visibility = if (b.tvFontSub.text.isBlank()) View.GONE else View.VISIBLE

                val tf = FontLoader.getTypeface(font.id) ?: Typeface.DEFAULT
                b.tvPreviewLarge.text = getSample(font); b.tvPreviewLarge.typeface = tf
                b.tvPreviewSmall.text = getSample(font); b.tvPreviewSmall.typeface = tf

                // Selection highlight
                val isSelected = selected.contains(font.id)
                b.root.strokeColor = if (isSelected)
                    ctx.getColor(R.color.accent) else ctx.getColor(R.color.divider)
                b.root.strokeWidth = if (isSelected) 2 else 1

                // Favorite
                b.btnFavorite.text = if (isFavorite(font.id)) "★" else "☆"
                b.btnFavorite.setTextColor(ctx.getColor(
                    if (isFavorite(font.id)) R.color.accent else R.color.text_muted))
                b.btnFavorite.visibility = if (selectionMode) View.GONE else View.VISIBLE
                b.btnFavorite.setOnClickListener { if (!selectionMode) onFavoriteClick(font) }

                b.btnRemove.visibility = if (selectionMode) View.GONE else View.VISIBLE
                b.btnRemove.setOnClickListener { if (!selectionMode) onRemoveClick(font) }

                b.root.setOnClickListener {
                    if (selectionMode) {
                        if (selected.contains(font.id)) selected.remove(font.id)
                        else selected.add(font.id)
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
