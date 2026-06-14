package com.fontlens.ui.glyph

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.databinding.ItemGlyphCellBinding

class GlyphAdapter(
    private val typeface: Typeface?,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<GlyphAdapter.VH>() {

    private var data = listOf<Int>()

    fun setData(chars: List<Int>) {
        data = chars
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemGlyphCellBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemGlyphCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cp = data[position]
        val ch = String(Character.toChars(cp))
        holder.binding.tvGlyph.text = ch
        holder.binding.tvGlyph.typeface = typeface ?: Typeface.DEFAULT
        holder.binding.tvCodepoint.text = holder.itemView.context.getString(R.string.codepoint_format, cp.toString(16).uppercase().padStart(4, '0'))
        holder.binding.root.setOnClickListener { onClick(ch) }
    }

    override fun getItemCount() = data.size
}
