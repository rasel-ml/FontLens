package com.fontlens.ui.settings

import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.R
import com.fontlens.data.ALL_LANGUAGES
import com.fontlens.data.FontRepository
import com.fontlens.data.defaultLanguageSamples
import com.fontlens.data.isSingleNameScript
import com.fontlens.data.scriptDisplayName
import com.fontlens.databinding.FragmentSampleManagerBinding
import com.fontlens.databinding.ItemSampleScriptBinding

// ── List item types ───────────────────────────────────────────────────────────

sealed class SampleListItem {
    data class Lang(
        val isoCode: String,
        var sampleText: String,
        val label: String,
        val isoLabel: String
    ) : SampleListItem()

    object Divider : SampleListItem()
}

// ── Fragment ──────────────────────────────────────────────────────────────────

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

        val adapter = LangSampleAdapter(buildItems()) { updatedItems ->
            // Separate visible (above divider) from hidden (below)
            val dividerIdx = updatedItems.indexOfFirst { it is SampleListItem.Divider }
            val allLangs = updatedItems.filterIsInstance<SampleListItem.Lang>()
            val newOrder = allLangs.map { it.isoCode }
            val newDividerPos = if (dividerIdx < 0) -1 else
                updatedItems.take(dividerIdx).filterIsInstance<SampleListItem.Lang>().size

            FontRepository.settings = FontRepository.settings.copy(
                langOrder        = newOrder,
                langSamplesByIso = allLangs.associate { it.isoCode to it.sampleText },
                dividerPosition  = newDividerPos
            )
            FontRepository.saveSettings(requireContext())
        }

        binding.rvScripts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScripts.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                // Divider row is not draggable itself
                return if (vh is LangSampleAdapter.DividerVH) 0
                       else makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                if (vh is LangSampleAdapter.DividerVH) return false
                adapter.moveItem(vh.adapterPosition, target.adapterPosition)
                // Update alpha on all visible views immediately during drag
                for (i in 0 until rv.childCount) {
                    val child = rv.getChildAt(i)
                    val pos   = rv.getChildAdapterPosition(child)
                    if (pos == RecyclerView.NO_ID.toInt()) continue
                    val divPos = adapter.dividerPosition()
                    val shouldFade = divPos >= 0 && pos > divPos
                    if (child != vh.itemView) child.alpha = if (shouldFade) 0.38f else 1f
                }
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && vh is LangSampleAdapter.LangVH) {
                    vh.itemView.alpha  = 0.85f
                    vh.itemView.scaleX = 1.02f
                    vh.itemView.scaleY = 1.02f
                }
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.alpha  = 1f
                vh.itemView.scaleX = 1f
                vh.itemView.scaleY = 1f
            }

            // Draw drag shadow over divider during drag so user sees they're crossing it
            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        })
        touchHelper.attachToRecyclerView(binding.rvScripts)
        adapter.touchHelper = touchHelper
    }

    private fun buildItems(): MutableList<SampleListItem> {
        val s        = FontRepository.settings
        val defaults = defaultLanguageSamples()

        val seen = mutableSetOf<String>()
        val allLangs = ALL_LANGUAGES.filter { seen.add(it.isoCode) }
        val orderedIsos = (s.langOrder + allLangs.map { it.isoCode }).distinct()

        val langItems = orderedIsos.mapNotNull { iso ->
            val lang = allLangs.find { it.isoCode == iso } ?: return@mapNotNull null
            val label = if (isSingleNameScript(lang.scriptCode)) lang.name
                        else "${lang.name} · ${scriptDisplayName(lang.scriptCode)}"
            SampleListItem.Lang(
                isoCode    = iso,
                sampleText = s.langSamplesByIso[iso] ?: defaults[iso] ?: "",
                label      = label,
                isoLabel   = iso.uppercase()
            )
        }

        val result = mutableListOf<SampleListItem>()
        val div = s.dividerPosition

        if (div < 0 || div >= langItems.size) {
            // No divider yet — put it at the very end
            result.addAll(langItems)
            result.add(SampleListItem.Divider)
        } else {
            result.addAll(langItems.take(div))
            result.add(SampleListItem.Divider)
            result.addAll(langItems.drop(div))
        }
        return result
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class LangSampleAdapter(
    val items: MutableList<SampleListItem>,
    private val onChanged: (List<SampleListItem>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var touchHelper: ItemTouchHelper? = null

    companion object {
        private const val TYPE_LANG    = 0
        private const val TYPE_DIVIDER = 1
    }

    inner class LangVH(val b: ItemSampleScriptBinding) : RecyclerView.ViewHolder(b.root)
    inner class DividerVH(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int) =
        if (items[position] is SampleListItem.Divider) TYPE_DIVIDER else TYPE_LANG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DIVIDER) {
            val v = buildDividerView(parent)
            DividerVH(v)
        } else {
            LangVH(ItemSampleScriptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    private fun buildDividerView(parent: ViewGroup): View {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        val container = android.widget.LinearLayout(ctx)
        container.orientation = android.widget.LinearLayout.HORIZONTAL
        container.gravity = android.view.Gravity.CENTER_VERTICAL
        container.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, (8 * dp).toInt(), 0, (8 * dp).toInt()) }

        // Left line
        val lineLeft = View(ctx)
        lineLeft.layoutParams = android.widget.LinearLayout.LayoutParams(0, (1.5f * dp).toInt(), 1f)
        lineLeft.setBackgroundColor(0xFFE05252.toInt()) // red-ish warning color

        // Label
        val label = TextView(ctx)
        label.text = "  Hidden below  "
        label.textSize = 11f
        label.setTextColor(0xFFE05252.toInt())
        label.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Right line
        val lineRight = View(ctx)
        lineRight.layoutParams = android.widget.LinearLayout.LayoutParams(0, (1.5f * dp).toInt(), 1f)
        lineRight.setBackgroundColor(0xFFE05252.toInt())

        container.addView(lineLeft)
        container.addView(label)
        container.addView(lineRight)
        return container
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DividerVH) return  // static, nothing to bind

        val item = items[position] as SampleListItem.Lang
        val b    = (holder as LangVH).b

        // Determine if this item is below the divider
        val divPos = items.indexOfFirst { it is SampleListItem.Divider }
        val isHidden = divPos >= 0 && position > divPos

        // Visual: grayed out when hidden
        val alpha = if (isHidden) 0.38f else 1f
        b.root.alpha = alpha
        b.root.isEnabled = !isHidden

        b.tvScriptName.text = item.label
        b.tvScriptCode.text = item.isoLabel

        b.etSampleText.setText(item.sampleText)
        b.etSampleText.isEnabled = !isHidden

        b.etSampleText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_ID.toInt() && items[pos] is SampleListItem.Lang) {
                    (items[pos] as SampleListItem.Lang).sampleText =
                        b.etSampleText.text?.toString() ?: ""
                    onChanged(items.toList())
                }
            }
        }

        b.btnRestoreDefault.setOnClickListener {
            val default = defaultLanguageSamples()[item.isoCode] ?: ""
            b.etSampleText.setText(default)
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_ID.toInt() && items[pos] is SampleListItem.Lang) {
                (items[pos] as SampleListItem.Lang).sampleText = default
                onChanged(items.toList())
            }
        }

        b.ivDragHandle.setOnTouchListener { _, _ ->
            touchHelper?.startDrag(holder)
            false
        }
    }

    fun dividerPosition() = items.indexOfFirst { it is SampleListItem.Divider }

    fun moveItem(from: Int, to: Int) {
        if (items[from] is SampleListItem.Divider) return
        val item = items.removeAt(from)
        items.add(to, item)
        val start = minOf(from, to)
        val end   = maxOf(from, to)
        notifyItemMoved(from, to)
        // Notify full affected range so hidden/visible alpha updates instantly
        notifyItemRangeChanged(start, end - start + 2)
        onChanged(items.toList())
    }
}
