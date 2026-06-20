package com.fontlens.ui.glyph

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fontlens.databinding.ItemGlyphCellBinding
import com.fontlens.utils.ThemeManager

class GlyphAdapter(
    private var typeface: Typeface?,
    private var presentSet: Set<Int> = emptySet(),
    private var showAll: Boolean = false
) : ListAdapter<Int, GlyphAdapter.VH>(DIFF) {

    fun update(codepoints: List<Int>, present: Set<Int>, showAllMode: Boolean, tf: Typeface?) {
        presentSet = present
        showAll    = showAllMode
        typeface   = tf
        submitList(codepoints)
    }

    inner class VH(val b: ItemGlyphCellBinding) : RecyclerView.ViewHolder(b.root)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(a: Int, b: Int) = a == b
            override fun areContentsTheSame(a: Int, b: Int) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemGlyphCellBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cp  = getItem(position)
        val ch  = try { String(Character.toChars(cp)) } catch (e: Exception) { "" }
        val b   = holder.b
        val ctx = b.root.context
        val p   = ThemeManager.activePalette
        val dp  = ctx.resources.displayMetrics.density
        val isPresent = presentSet.contains(cp)

        b.tvGlyph.text     = ch
        b.tvGlyph.typeface = typeface ?: Typeface.DEFAULT
        b.tvCodepoint.text = "U+${cp.toString(16).uppercase().padStart(4, '0')}"

        // Cell background — rounded rect
        val cellBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f * dp
            if (showAll) {
                if (isPresent) {
                    val r = Color.red(p.accent); val g = Color.green(p.accent); val bl = Color.blue(p.accent)
                    setColor(Color.argb(18, r, g, bl))
                    setStroke((1f * dp).toInt(), Color.argb(60, r, g, bl))
                } else {
                    setColor(Color.argb(12, 220, 60, 60))
                    setStroke((1f * dp).toInt(), Color.argb(40, 220, 60, 60))
                }
            } else {
                val r = Color.red(p.accent); val g = Color.green(p.accent); val bl = Color.blue(p.accent)
                setColor(Color.argb(15, r, g, bl))
                setStroke((1f * dp).toInt(), Color.argb(45, r, g, bl))
            }
        }
        b.root.background = cellBg

        // Text color + glyph color (no setShadowLayer — it renders as rect)
        if (showAll) {
            if (isPresent) {
                b.tvGlyph.setTextColor(p.textPrimary)
                b.tvCodepoint.setTextColor(p.textMuted)
                b.root.alpha = 1f
            } else {
                b.tvGlyph.setTextColor(Color.argb(130, 220, 60, 60))
                b.tvCodepoint.setTextColor(Color.argb(80, 200, 60, 60))
                b.root.alpha = 0.65f
            }
        } else {
            b.tvGlyph.setTextColor(p.textPrimary)
            b.tvCodepoint.setTextColor(p.textMuted)
            b.root.alpha = 1f
        }
        // Clear any old shadow
        b.tvGlyph.setShadowLayer(0f, 0f, 0f, 0)

        // Single tap — copy character
        b.root.setOnClickListener {
            if (ch.isNotEmpty()) {
                val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("glyph", ch))
                Toast.makeText(ctx, "Copied: $ch", Toast.LENGTH_SHORT).show()
            }
        }
        // Long press — copy U+XXXX
        b.root.setOnLongClickListener {
            val uni = "U+${cp.toString(16).uppercase().padStart(4, '0')}"
            val cb  = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("unicode", uni))
            Toast.makeText(ctx, "Copied: $uni", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
