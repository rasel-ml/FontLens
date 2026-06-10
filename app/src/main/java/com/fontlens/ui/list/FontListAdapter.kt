package com.fontlens.ui.list

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.databinding.ItemFontCardBinding
import com.fontlens.utils.FontLoader

class FontListAdapter(
    private val onFontClick: (FontItem) -> Unit,
    private val onFavoriteClick: (FontItem) -> Unit,
    private val onRemoveClick: (FontItem) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val getSample: (FontItem) -> String
) : ListAdapter<FontItem, FontListAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemFontCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemFontCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val font = getItem(position)
        val b = holder.binding
        val m = font.effectiveMeta

        b.tvFontName.text = m.family.ifEmpty { font.displayName }
        b.tvFontSub.text = buildString {
            if (m.weightName.isNotEmpty()) append(m.weightName)
            if (m.subfamily.isNotEmpty() && m.subfamily != "Regular") append(" · ${m.subfamily}")
        }
        b.tvFontSub.visibility = if (b.tvFontSub.text.isBlank())
            android.view.View.GONE else android.view.View.VISIBLE

        val sample = getSample(font)
        val tf = FontLoader.getTypeface(font.id) ?: Typeface.DEFAULT
        b.tvPreviewLarge.text = sample
        b.tvPreviewLarge.typeface = tf
        b.tvPreviewSmall.text = sample
        b.tvPreviewSmall.typeface = tf

        b.btnFavorite.text = if (isFavorite(font.id)) "★" else "☆"
        b.btnFavorite.setTextColor(
            holder.itemView.context.getColor(
                if (isFavorite(font.id)) R.color.accent else R.color.text_muted
            )
        )
        b.btnFavorite.setOnClickListener { onFavoriteClick(font) }

        // Single tap only — opens bottom sheet style dialog
        b.btnRemove.setOnClickListener { onRemoveClick(font) }
        b.btnRemove.setOnLongClickListener(null)

        b.root.setOnClickListener { onFontClick(font) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FontItem>() {
            override fun areItemsTheSame(a: FontItem, b: FontItem) = a.id == b.id
            override fun areContentsTheSame(a: FontItem, b: FontItem) = a == b
        }
    }
}
