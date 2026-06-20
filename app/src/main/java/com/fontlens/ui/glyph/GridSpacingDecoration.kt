package com.fontlens.ui.glyph

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Adds equal spacing around every grid cell so borders never touch.
 * [spacing] is in pixels. Half-spacing on each side means the gap between
 * two adjacent cells is always exactly [spacing] pixels.
 */
class GridSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.left   = spacing
        outRect.right  = spacing
        outRect.top    = spacing
        outRect.bottom = spacing
    }
}
