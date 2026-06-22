package com.fontlens.ui.list

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.data.FontListItem
import com.fontlens.data.FontRepository
import com.fontlens.data.scriptDisplayName
import com.fontlens.databinding.ItemFontCardBinding
import com.fontlens.databinding.ItemFolderHeaderBinding
import com.fontlens.utils.ThemeManager
import com.fontlens.utils.TypefaceLoader

class FontListAdapter(
    private val onFontClick: (FontItem) -> Unit,
    private val onSampleClick: (FontItem, String) -> Unit,
    private val onFavoriteClick: (FontItem) -> Unit,
    private val onRemoveClick: (FontItem) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val getSample: (FontItem) -> String,
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<FontListItem, RecyclerView.ViewHolder>(DIFF) {

    private val selected = mutableSetOf<String>()
    var selectionMode = false
        private set

    /** Active script index per font id. -1 means "Default" (embedded) is active. */
    private val activeScriptIndex = mutableMapOf<String, Int>()

    fun exitSelectionMode() {
        selectionMode = false; selected.clear()
        notifyDataSetChanged(); onSelectionChanged(emptySet())
    }

    fun selectAll(items: List<FontItem>) {
        items.forEach { selected.add(it.id) }
        onSelectionChanged(selected.toSet()); notifyDataSetChanged()
    }

    fun getSelectedIds() = selected.toSet()

    fun notifyTypefaceReady(fontId: String) {
        val idx = currentList.indexOfFirst { it is FontListItem.Font && it.font.id == fontId }
        if (idx >= 0) notifyItemChanged(idx, "typeface")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "typeface" && holder is FontVH) {
            val item = getItem(position)
            if (item is FontListItem.Font) {
                holder.binding.tvPreviewLarge.typeface =
                    TypefaceLoader.getTypeface(item.font.id) ?: Typeface.DEFAULT
                holder.binding.root.alpha = 1f
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    companion object {
        private const val TYPE_FONT   = 0
        private const val TYPE_HEADER = 1
        val DIFF = object : DiffUtil.ItemCallback<FontListItem>() {
            override fun areItemsTheSame(a: FontListItem, b: FontListItem) = when {
                a is FontListItem.Font         && b is FontListItem.Font         -> a.font.id == b.font.id
                a is FontListItem.FolderHeader && b is FontListItem.FolderHeader -> a.path == b.path
                else -> false
            }
            override fun areContentsTheSame(a: FontListItem, b: FontListItem) = a == b
        }
    }

    override fun getItemViewType(pos: Int) = when (getItem(pos)) {
        is FontListItem.Font         -> TYPE_FONT
        is FontListItem.FolderHeader -> TYPE_HEADER
    }

    inner class FontVH(val binding: ItemFontCardBinding)       : RecyclerView.ViewHolder(binding.root)
    inner class HeaderVH(val binding: ItemFolderHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_FONT)
            FontVH(ItemFontCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            HeaderVH(ItemFolderHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FontListItem.FolderHeader -> (holder as HeaderVH).binding.tvFolderHeader.text = item.path
            is FontListItem.Font         -> bindFont(holder as FontVH, item.font)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Main bind
    // ─────────────────────────────────────────────────────────────────────

    private fun bindFont(holder: FontVH, font: FontItem) {
        val b   = holder.binding
        val m   = font.effectiveMeta
        val p   = ThemeManager.activePalette
        val ctx = holder.itemView.context

        // ── Family name · Subfamily on one line ──────────────────────────
        // Name priority: family → fullName → display name (file name without extension)
        val familyName = m.family.ifEmpty { m.fullName.ifEmpty { font.displayName } }
        // Subfamily priority: subfamily field → weightName → inferred from weight → blank
        val subfamilyRaw = when {
            m.subfamily.isNotEmpty() -> m.subfamily
            m.weightName.isNotEmpty() -> m.weightName
            else -> inferSubfamilyFromWeight(m.weight)
        }
        val ssb = SpannableStringBuilder()
        ssb.append(familyName)
        ssb.setSpan(StyleSpan(Typeface.BOLD), 0, familyName.length, 0)
        ssb.setSpan(ForegroundColorSpan(p.textPrimary), 0, familyName.length, 0)
        if (subfamilyRaw.isNotEmpty()) {
            val dot = " · $subfamilyRaw"
            val start = ssb.length
            ssb.append(dot)
            ssb.setSpan(StyleSpan(Typeface.NORMAL), start, ssb.length, 0)
            ssb.setSpan(ForegroundColorSpan(p.textMuted), start, ssb.length, 0)
            ssb.setSpan(RelativeSizeSpan(0.82f), start, ssb.length, 0)  // ~12.3sp vs 15sp
        }
        b.tvFontName.text = ssb
        b.tvFontSub.visibility = android.view.View.GONE   // no longer used

        // ── Font type badge (TTF / OTF / WOFF …) ─────────────────────────
        val ext = font.uri.path?.substringAfterLast('.')?.uppercase() ?: "TTF"
        b.tvFontType.text = ext
        val badgeBg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 7f * ctx.resources.displayMetrics.density
            setColor(android.graphics.Color.argb(25,
                android.graphics.Color.red(p.accent),
                android.graphics.Color.green(p.accent),
                android.graphics.Color.blue(p.accent)))
            setStroke((1f * ctx.resources.displayMetrics.density).toInt(), p.accent)
        }
        b.tvFontType.background = badgeBg
        b.tvFontType.setTextColor(p.accent)

        // ── Favorite / Delete button styling ─────────────────────────────
        val dp = ctx.resources.displayMetrics.density
        fun iconBg() = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 7f * dp
            setColor(android.graphics.Color.argb(25,
                android.graphics.Color.red(p.accent),
                android.graphics.Color.green(p.accent),
                android.graphics.Color.blue(p.accent)))
        }
        b.btnFavorite.background = iconBg()
        b.btnRemove.background   = iconBg()

        // ── Script label row ──────────────────────────────────────────────
        val s            = FontRepository.settings
        val orderedCodes = FontRepository.orderedScriptCodesForFont(font)
        val hasEmbedded  = s.preferMetaSample && m.sampleText.isNotEmpty()

        // Build the label list: "Default" first (if embedded available), then script names
        // INDEX MAP: -1 = Default, 0..n-1 = orderedCodes
        val showDefault  = hasEmbedded

        // Initialise active index
        val storedIdx = activeScriptIndex.getOrDefault(font.id, if (showDefault) -1 else 0)
        val safeIdx   = when {
            storedIdx == -1 && showDefault              -> -1
            storedIdx == -1 && !showDefault             -> 0
            storedIdx >= orderedCodes.size              -> 0
            else                                        -> storedIdx
        }.let { if (orderedCodes.isEmpty() && it != -1) -1 else it }
        activeScriptIndex[font.id] = safeIdx

        buildLabelRow(ctx, b, font, orderedCodes, showDefault, safeIdx, p)

        // ── Typeface + preview ────────────────────────────────────────────
        val tf = TypefaceLoader.getTypeface(font.id) ?: Typeface.DEFAULT
        b.root.alpha = if (TypefaceLoader.isLoaded(font.id)) 1f else 0.6f
        applyPreview(b, font, orderedCodes, showDefault, safeIdx, tf)

        // Cycle button hidden — interaction is via label taps
        b.btnCycleScript.visibility = View.GONE

        // ── Selection ─────────────────────────────────────────────────────
        val isSelected = selected.contains(font.id)
        b.root.strokeColor = if (isSelected) p.accent else p.divider
        b.root.strokeWidth = if (isSelected) 2 else 1

        // ── Favorite / Remove ─────────────────────────────────────────────
        val isFav = isFavorite(font.id)
        b.btnFavorite.setImageResource(if (isFav) R.drawable.ic_star else R.drawable.ic_star_outline)
        b.btnFavorite.imageTintList = android.content.res.ColorStateList.valueOf(
            if (isFav) p.accent else p.textMuted)
        b.btnRemove.imageTintList = android.content.res.ColorStateList.valueOf(p.accent)
        val btnVisible = if (selectionMode) View.GONE else View.VISIBLE
        b.tvFontType.visibility  = btnVisible
        b.btnFavorite.visibility = btnVisible
        b.btnRemove.visibility   = btnVisible
        b.btnFavorite.setOnClickListener { if (!selectionMode) onFavoriteClick(font) }
        b.btnRemove.setOnClickListener   { if (!selectionMode) onRemoveClick(font) }

        // ── Card click / long-press ───────────────────────────────────────
        b.root.setOnClickListener {
            if (selectionMode) {
                if (selected.contains(font.id)) selected.remove(font.id) else selected.add(font.id)
                onSelectionChanged(selected.toSet()); notifyItemChanged(holder.adapterPosition)
            }
            // Non-selection tap on card body does nothing — tap sample text to preview
        }
        b.root.setOnLongClickListener {
            if (!selectionMode) {
                selectionMode = true; selected.add(font.id)
                onSelectionChanged(selected.toSet()); notifyDataSetChanged()
            }; true
        }

        // ── Sample text click → open preview with current sample ──────────
        b.tvPreviewLarge.isClickable = true
        b.tvPreviewLarge.isFocusable = true
        b.tvPreviewLarge.setOnClickListener {
            if (!selectionMode) onSampleClick(font, b.tvPreviewLarge.text.toString())
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Label row  —  "Default | English | Bengali | Arabic …"
    // Each word is a tappable span; no bg, no border.
    // Active: accent color, bold, subtle shadow.
    // Inactive: gray / pale, normal weight.
    // ─────────────────────────────────────────────────────────────────────

    private fun buildLabelRow(
        ctx: Context,
        b: ItemFontCardBinding,
        font: FontItem,
        codes: List<String>,
        showDefault: Boolean,
        activeIdx: Int,       // -1 = Default, 0..n-1 = script code
        p: ThemeManager.Palette
    ) {
        val container = b.llScriptChips   // reuse existing LinearLayout inside HorizontalScrollView
        container.removeAllViews()

        val totalLabels = (if (showDefault) 1 else 0) + codes.size
        if (font.effectiveMeta.isAnsiLegacy) {
            // ANSI legacy: show a single non-interactive "ANSI" badge
            b.hsvScriptChips.visibility = View.VISIBLE
            container.removeAllViews()
            val dp = ctx.resources.displayMetrics.density
            val tv = TextView(ctx)
            tv.text     = "ANSI"
            tv.textSize = 12.5f
            tv.setTextColor(p.accent)
            tv.setTypeface(tv.typeface, Typeface.BOLD)
            tv.setPadding((4f * dp).toInt(), (2f * dp).toInt(), (4f * dp).toInt(), (2f * dp).toInt())
            tv.layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            container.addView(tv)
            return
        }
        if (totalLabels == 0) {
            b.hsvScriptChips.visibility = View.GONE
            return
        }
        b.hsvScriptChips.visibility = View.VISIBLE

        val dp = ctx.resources.displayMetrics.density

        // Helper: build one label TextView
        fun makeLabel(text: String, labelIdx: Int, hasSample: Boolean): TextView {
            val isActive = labelIdx == activeIdx
            val tv = TextView(ctx)
            tv.text     = text
            tv.textSize = 12.5f

            // Style
            styleLabel(tv, isActive, hasSample, p)

            // Padding — tight, no background
            val padH = (4f * dp).toInt()
            val padV = (2f * dp).toInt()
            tv.setPadding(padH, padV, padH, padV)

            // Layout: wrap content, small end margin handled by separator
            tv.layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tv.setOnClickListener {
                val cur = activeScriptIndex[font.id]
                if (cur == labelIdx) return@setOnClickListener   // already active
                activeScriptIndex[font.id] = labelIdx
                val tf = TypefaceLoader.getTypeface(font.id) ?: Typeface.DEFAULT
                val newShowDefault = FontRepository.settings.preferMetaSample &&
                        font.effectiveMeta.sampleText.isNotEmpty()
                applyPreview(b, font, codes, newShowDefault, labelIdx, tf)
                refreshLabelStates(b, codes, newShowDefault, labelIdx, p)
            }
            return tv
        }

        // Helper: separator "|"
        fun makeSep(): TextView {
            val sep = TextView(ctx)
            sep.text      = " | "
            sep.textSize  = 12f
            sep.setTextColor(Color.argb(80, Color.red(p.textMuted), Color.green(p.textMuted), Color.blue(p.textMuted)))
            sep.setPadding(0, 0, 0, 0)
            sep.layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            return sep
        }

        var labelIdx = -1   // starts at Default

        if (showDefault) {
            container.addView(makeLabel("Default", -1, true))
            labelIdx = -1
        }

        codes.forEachIndexed { i, code ->
            val idx       = i                               // script index
            val hasSample = FontRepository.getSampleForLang(code) != null
            val name      = scriptDisplayName(code)

            if (showDefault || i > 0) container.addView(makeSep())
            container.addView(makeLabel(name, idx, hasSample))
        }
    }

    private fun styleLabel(tv: TextView, isActive: Boolean, hasSample: Boolean, p: ThemeManager.Palette) {
        when {
            isActive && hasSample -> {
                tv.setTextColor(p.accent)
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.setShadowLayer(6f, 0f, 1f,
                    Color.argb(60, Color.red(p.accent), Color.green(p.accent), Color.blue(p.accent)))
                tv.alpha = 1f
            }
            isActive && !hasSample -> {
                // Active but no sample — pale accent, still bold
                tv.setTextColor(Color.argb(140, Color.red(p.accent), Color.green(p.accent), Color.blue(p.accent)))
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.setShadowLayer(0f, 0f, 0f, 0)
                tv.alpha = 0.7f
            }
            !isActive && hasSample -> {
                tv.setTextColor(p.textMuted)
                tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                tv.setShadowLayer(0f, 0f, 0f, 0)
                tv.alpha = 1f
            }
            else -> { // inactive + no sample
                tv.setTextColor(p.textMuted)
                tv.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                tv.setShadowLayer(0f, 0f, 0f, 0)
                tv.alpha = 0.35f
            }
        }
        tv.background = null
    }

    private fun refreshLabelStates(
        b: ItemFontCardBinding,
        codes: List<String>,
        showDefault: Boolean,
        activeIdx: Int,
        p: ThemeManager.Palette
    ) {
        val container = b.llScriptChips
        var viewPos   = 0

        // Each label is at positions: 0, 2, 4 … (separators at 1, 3, 5 …)
        // Or if no default: 0, 2, 4 … same pattern
        fun nextLabel(): TextView? {
            while (viewPos < container.childCount) {
                val v = container.getChildAt(viewPos++)
                if (v is TextView && v.text != " | ") return v
            }
            return null
        }

        if (showDefault) {
            nextLabel()?.let { tv ->
                val hasSample = true
                styleLabel(tv, activeIdx == -1, hasSample, p)
                animateLabel(tv, activeIdx == -1)
            }
        }
        codes.forEachIndexed { idx, code ->
            nextLabel()?.let { tv ->
                val hasSample = FontRepository.getSampleForLang(code) != null
                styleLabel(tv, idx == activeIdx, hasSample, p)
                animateLabel(tv, idx == activeIdx)
            }
        }
    }

    private fun animateLabel(tv: TextView, isActive: Boolean) {
        tv.animate().cancel()
        tv.animate().scaleX(if (isActive) 1.08f else 1f)
                    .scaleY(if (isActive) 1.08f else 1f)
                    .setDuration(150).start()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Weight → subfamily inference
    // ─────────────────────────────────────────────────────────────────────

    private fun inferSubfamilyFromWeight(weight: Int): String = when {
        weight <= 100 -> "Thin"
        weight <= 200 -> "ExtraLight"
        weight <= 300 -> "Light"
        weight in 301..449 -> "Regular"          // Regular — omit, not useful
        weight in 450..549 -> "Medium"
        weight in 550..649 -> "SemiBold"
        weight in 650..749 -> "Bold"
        weight in 750..849 -> "ExtraBold"
        weight >= 850      -> "Black"
        else -> ""
    }

    // ─────────────────────────────────────────────────────────────────────
    // Preview text
    // ─────────────────────────────────────────────────────────────────────

    private fun applyPreview(
        b: ItemFontCardBinding,
        font: FontItem,
        codes: List<String>,
        showDefault: Boolean,
        activeIdx: Int,   // -1 = Default
        tf: Typeface
    ) {
        val m = font.effectiveMeta
        val text = when {
            // ANSI legacy: embedded sample text highest priority always, else "ANSI" label
            m.isAnsiLegacy && m.sampleText.isNotEmpty() -> m.sampleText
            m.isAnsiLegacy -> FontRepository.getSampleForLang("ansi") ?: "ANSI"
            activeIdx == -1 && showDefault -> m.sampleText
            codes.isNotEmpty() && activeIdx in codes.indices -> {
                val code = codes[activeIdx]
                FontRepository.getSampleForLang(code) ?: getSample(font)
            }
            else -> getSample(font)
        }
        b.tvPreviewLarge.text     = text.replace("\n", "  ").replace("\r", "")
        b.tvPreviewLarge.typeface = tf
    }
}
