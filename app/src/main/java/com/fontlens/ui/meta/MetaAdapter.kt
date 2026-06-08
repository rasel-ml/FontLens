package com.fontlens.ui.meta

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.databinding.ItemMetaRowBinding

class MetaAdapter(private val items: List<Pair<String, String>>) :
    RecyclerView.Adapter<MetaAdapter.VH>() {

    inner class VH(val binding: ItemMetaRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMetaRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvKey.text   = items[position].first
        holder.binding.tvValue.text = items[position].second
    }

    override fun getItemCount() = items.size
}
