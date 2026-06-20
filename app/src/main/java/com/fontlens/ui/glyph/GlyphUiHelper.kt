package com.fontlens.ui.glyph

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.fontlens.data.FontItem
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentGlyphBinding
import com.fontlens.utils.ThemeManager
import com.fontlens.utils.TypefaceLoader

/**
 * Shared logic used by both GlyphFragment and StandaloneGlyphFragment.
 * Holds all state and wires up the UI — caller just provides the binding and font.
 */
class GlyphUiHelper(
    private val binding: FragmentGlyphBinding,
    private val font: FontItem,
    private val onNavigateBack: () -> Unit
) {
    private var allPages       = listOf<GlyphScriptDefs.BlockPage>()
    private var searchPages    = listOf<GlyphScriptDefs.BlockPage>()
    private var currentPageIdx = 0
    private var searchQuery    = ""
    private var searchMode     = "char"   // "char" | "unicode"
    private var lastNavDir     = 0         // +1 forward, -1 backward, 0 initial

    private lateinit var adapter: GlyphAdapter

    fun setup() {
        val tf      = TypefaceLoader.getTypeface(font.id)
        val showAll = FontRepository.settings.glyphShowAll
        val p       = ThemeManager.activePalette
        val dp      = binding.root.context.resources.displayMetrics.density
        val ctx     = binding.root.context

        binding.toolbar.setNavigationOnClickListener { onNavigateBack() }

        // ── Build pages sorted by available glyph count descending ────────
        val supportedSet = font.meta.supportedChars.toSet()
        allPages = GlyphScriptDefs.buildPages(supportedSet, showAll)
            .sortedByDescending { page ->
                val totalInBlock = page.block.range.count()
                if (totalInBlock == 0) 0.0
                else page.presentSet.size.toDouble() / totalInBlock
            }

        // ── Total glyph count badge ────────────────────────────────────────
        binding.tvGlyphCount.text = font.meta.supportedChars.size.toString()
        val badgeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * dp
            setColor(p.accent)
        }
        binding.tvGlyphCount.background = badgeBg
        binding.tvGlyphCount.setTextColor(Color.WHITE)

        // ── Single toggle button Aa ↔ UN ──────────────────────────────────
        styleToggleBtn(p, dp)
        binding.btnSearchToggle.setOnClickListener {
            searchMode = if (searchMode == "char") "unicode" else "char"
            styleToggleBtn(p, dp)
            reapplySearch()
        }

        // ── Search text ───────────────────────────────────────────────────
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(e: Editable?) {
                searchQuery = e?.toString() ?: ""
                currentPageIdx = 0
                reapplySearch()
            }
        })

        // ── Adapter + grid ────────────────────────────────────────────────
        adapter = GlyphAdapter(tf, supportedSet, showAll)
        // Auto column count: target cell size ~68dp, 4dp gap each side = 76dp per slot
        val dm          = ctx.resources.displayMetrics
        val screenWidthDp = dm.widthPixels / dm.density
        val cellSlotDp  = 76f   // cell width (68dp) + margins (4dp each side)
        val columns     = (screenWidthDp / cellSlotDp).toInt().coerceAtLeast(3)
        binding.rvGlyphs.layoutManager = GridLayoutManager(ctx, columns)
        // 4dp spacing on every side of every cell → 8dp gap between cells, 4dp at edges
        val spacingPx = (4f * dm.density).toInt()
        binding.rvGlyphs.addItemDecoration(GridSpacingDecoration(spacingPx))
        binding.rvGlyphs.adapter = adapter

        // ── Pagination ────────────────────────────────────────────────────
        binding.btnPrev.setOnClickListener {
            if (currentPageIdx > 0) { lastNavDir = -1; currentPageIdx--; renderPage() }
        }
        binding.btnNext.setOnClickListener {
            if (currentPageIdx < activePages().size - 1) { lastNavDir = +1; currentPageIdx++; renderPage() }
        }

        searchPages = allPages
        renderPage()
    }

    private fun activePages() = if (searchQuery.isBlank()) allPages else searchPages

    private fun reapplySearch() {
        if (searchQuery.isBlank()) { searchPages = allPages; renderPage(); return }
        val q = searchQuery.trim()
        searchPages = allPages.mapNotNull { page ->
            val matched = page.codepoints.filter { cp ->
                when (searchMode) {
                    "unicode" -> {
                        // Match if the hex value contains the query string (case-insensitive)
                        // e.g. query "A" matches U+0041, U+00A0, U+1A00 etc.
                        val hex = cp.toString(16).uppercase()
                        val qUp = q.uppercase()
                        hex.contains(qUp) || "U+$hex".contains(qUp)
                    }
                    else -> {
                        // Exact character match — each input character is matched individually
                        // e.g. query "A" shows only U+0041, query "a" shows only U+0061
                        try {
                            val ch = String(Character.toChars(cp))
                            q.any { inputChar -> ch == inputChar.toString() }
                        } catch (e: Exception) { false }
                    }
                }
            }
            if (matched.isEmpty()) null else page.copy(codepoints = matched)
        }
        renderPage()
    }

    private fun renderPage() {
        val pages = activePages()
        if (pages.isEmpty()) {
            binding.tvScriptName.text  = "No results"
            binding.tvScriptStats.text = ""
            binding.tvPage.text        = "0 / 0"
            binding.btnPrev.alpha      = 0.3f
            binding.btnNext.alpha      = 0.3f
            binding.tvScriptPrev.text  = ""
            binding.tvScriptNext.text  = ""
            adapter.update(emptyList(), emptySet(), false, null)
            return
        }
        val safeIdx = currentPageIdx.coerceIn(0, pages.size - 1)
        currentPageIdx = safeIdx
        val page     = pages[safeIdx]
        val showAll  = FontRepository.settings.glyphShowAll
        val p        = ThemeManager.activePalette
        val prevPage = if (safeIdx > 0) pages[safeIdx - 1] else null
        val nextPage = if (safeIdx < pages.size - 1) pages[safeIdx + 1] else null

        // ── Static content (no animation) ────────────────────────────────
        binding.tvPage.text = "${safeIdx + 1} / ${pages.size}"
        binding.btnPrev.alpha = if (safeIdx > 0) 1f else 0.3f
        binding.btnNext.alpha = if (safeIdx < pages.size - 1) 1f else 0.3f

        binding.tvScriptPrev.setOnClickListener {
            if (prevPage != null) { lastNavDir = -1; currentPageIdx--; renderPage() }
        }
        binding.tvScriptNext.setOnClickListener {
            if (nextPage != null) { lastNavDir = +1; currentPageIdx++; renderPage() }
        }

        // ── Glyph grid ────────────────────────────────────────────────────
        val tf = TypefaceLoader.getTypeface(font.id)
        adapter.update(page.codepoints, page.presentSet, showAll, tf)

        // ── Script label morph animation ──────────────────────────────────
        // On first load (lastNavDir == 0) just set values directly.
        if (lastNavDir == 0) {
            applyScriptLabels(page, prevPage, nextPage, p)
            return
        }

        // The idea:
        //   Moving FORWARD (+1): the old "next" label (small, dim, right) morphs into
        //     the center (large, bright), old "center" morphs out to the left (small, dim).
        //   Moving BACKWARD (-1): mirror of the above.
        //
        // We achieve this by animating textSize (via scaleX/scaleY on the TextView) and
        // alpha, then snapping to final values at end. TextSize can't be animated directly
        // so we use scale: center text is scale 1.0 (15sp), side texts are scale ~0.73 (11sp).
        val SCALE_CENTER = 1.0f
        val SCALE_SIDE   = 11f / 15f   // ≈ 0.733
        val DUR          = 260L

        val interp = android.view.animation.DecelerateInterpolator(1.8f)

        if (lastNavDir > 0) {
            // ── Forward: next → center, center → prev, prev disappears ───
            // Set current labels to their START state (pre-animation values)
            binding.tvScriptName.text  = prevPage?.block?.name ?: ""   // was center
            binding.tvScriptName.alpha = 1f
            binding.tvScriptName.scaleX = SCALE_CENTER; binding.tvScriptName.scaleY = SCALE_CENTER
            binding.tvScriptName.setTextColor(p.textPrimary)

            binding.tvScriptPrev.text  = ""   // prev disappears
            binding.tvScriptPrev.alpha = 0f

            binding.tvScriptNext.text  = page.block.name   // was next, becomes new center
            binding.tvScriptNext.alpha = 0.5f
            binding.tvScriptNext.scaleX = SCALE_SIDE; binding.tvScriptNext.scaleY = SCALE_SIDE
            binding.tvScriptNext.setTextColor(p.textMuted)

            // Animate center → left (shrink, dim)
            binding.tvScriptName.animate().cancel()
            binding.tvScriptName.animate()
                .scaleX(SCALE_SIDE).scaleY(SCALE_SIDE)
                .alpha(0.5f)
                .setDuration(DUR).setInterpolator(interp)
                .withEndAction {
                    // After animation snap everything to final state
                    applyScriptLabels(page, prevPage, nextPage, p)
                }.start()

            // Animate next → center (grow, brighten)
            binding.tvScriptNext.animate().cancel()
            binding.tvScriptNext.animate()
                .scaleX(SCALE_CENTER).scaleY(SCALE_CENTER)
                .alpha(1f)
                .setDuration(DUR).setInterpolator(interp)
                .start()

        } else {
            // ── Backward: prev → center, center → next, next disappears ──
            binding.tvScriptName.text  = nextPage?.block?.name ?: ""   // was center
            binding.tvScriptName.alpha = 1f
            binding.tvScriptName.scaleX = SCALE_CENTER; binding.tvScriptName.scaleY = SCALE_CENTER
            binding.tvScriptName.setTextColor(p.textPrimary)

            binding.tvScriptNext.text  = ""   // next disappears
            binding.tvScriptNext.alpha = 0f

            binding.tvScriptPrev.text  = page.block.name   // was prev, becomes new center
            binding.tvScriptPrev.alpha = 0.5f
            binding.tvScriptPrev.scaleX = SCALE_SIDE; binding.tvScriptPrev.scaleY = SCALE_SIDE
            binding.tvScriptPrev.setTextColor(p.textMuted)

            // Animate center → right (shrink, dim)
            binding.tvScriptName.animate().cancel()
            binding.tvScriptName.animate()
                .scaleX(SCALE_SIDE).scaleY(SCALE_SIDE)
                .alpha(0.5f)
                .setDuration(DUR).setInterpolator(interp)
                .withEndAction {
                    applyScriptLabels(page, prevPage, nextPage, p)
                }.start()

            // Animate prev → center (grow, brighten)
            binding.tvScriptPrev.animate().cancel()
            binding.tvScriptPrev.animate()
                .scaleX(SCALE_CENTER).scaleY(SCALE_CENTER)
                .alpha(1f)
                .setDuration(DUR).setInterpolator(interp)
                .start()
        }

        // Stats counter fades in
        binding.tvScriptStats.alpha = 0f
        binding.tvScriptStats.text  = "${page.presentSet.size} / ${page.block.range.count()}"
        binding.tvScriptStats.animate().alpha(1f).setDuration(200).start()
    }

    /** Snap all three labels to their final resting values instantly (no animation). */
    private fun applyScriptLabels(
        page: GlyphScriptDefs.BlockPage,
        prevPage: GlyphScriptDefs.BlockPage?,
        nextPage: GlyphScriptDefs.BlockPage?,
        p: ThemeManager.Palette
    ) {
        binding.tvScriptName.text   = page.block.name
        binding.tvScriptName.alpha  = 1f
        binding.tvScriptName.scaleX = 1f; binding.tvScriptName.scaleY = 1f
        binding.tvScriptName.setTextColor(p.textPrimary)

        binding.tvScriptStats.text  = "${page.presentSet.size} / ${page.block.range.count()}"
        binding.tvScriptStats.alpha = 1f

        binding.tvScriptPrev.text   = prevPage?.block?.name ?: ""
        binding.tvScriptPrev.alpha  = if (prevPage != null) 0.5f else 0f
        binding.tvScriptPrev.scaleX = 1f; binding.tvScriptPrev.scaleY = 1f
        binding.tvScriptPrev.setTextColor(p.textMuted)

        binding.tvScriptNext.text   = nextPage?.block?.name ?: ""
        binding.tvScriptNext.alpha  = if (nextPage != null) 0.5f else 0f
        binding.tvScriptNext.scaleX = 1f; binding.tvScriptNext.scaleY = 1f
        binding.tvScriptNext.setTextColor(p.textMuted)
    }

    private fun styleToggleBtn(p: ThemeManager.Palette, dp: Float) {
        val isChar = searchMode == "char"
        binding.btnSearchToggle.text = if (isChar) "Aa" else "UN"
        // Round button: cornerRadius = 50% makes it a pill/circle
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f * dp   // fully round
            setColor(Color.argb(75,
                Color.red(p.accent), Color.green(p.accent), Color.blue(p.accent)))
        }
        binding.btnSearchToggle.background = bg
        binding.btnSearchToggle.setTextColor(p.accent)
    }
}
