package com.fontlens.ui

import android.app.AlertDialog
import android.content.Context
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.data.FontRepository

object DeleteFontDialog {

    /**
     * onRemoveFromLibrary — called when user picks "Remove from Library" (no storage delete)
     * onDeletePermanently — called when user picks "Delete Permanently" (caller handles storage permission)
     */
    fun show(
        context: Context,
        font: FontItem,
        onRemoveFromLibrary: () -> Unit,
        onDeletePermanently: () -> Unit
    ) {
        val name = font.effectiveMeta.family.ifEmpty { font.displayName }
        val view = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_delete_font, null)

        val tvName        = view.findViewById<android.widget.TextView>(R.id.tv_delete_font_name)
        val tvMessage     = view.findViewById<android.widget.TextView>(R.id.tv_delete_message)
        val btnDeletePerm = view.findViewById<android.widget.TextView>(R.id.btn_delete_permanently)
        val btnRemoveLib  = view.findViewById<android.widget.TextView>(R.id.btn_remove_library)

        tvName.text    = name
        tvMessage.text = context.getString(R.string.delete_font_message)

        val dialog = AlertDialog.Builder(context)
            .setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnRemoveLib.setOnClickListener {
            dialog.dismiss()
            FontRepository.removeFont(font.id, context)
            onRemoveFromLibrary()
        }

        btnDeletePerm.setOnClickListener {
            dialog.dismiss()
            // Second confirmation, then delegate storage deletion to caller
            AlertDialog.Builder(context, R.style.Theme_FontLens_Dialog)
                .setTitle("⚠ Permanently Delete")
                .setMessage("Delete \"$name\" from your device?\n\nAndroid will ask for permission. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    onDeletePermanently()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    // Convenience overload — keeps old callers working where both actions do same thing
    fun show(
        context: Context,
        font: FontItem,
        onRemoved: () -> Unit
    ) = show(
        context = context,
        font = font,
        onRemoveFromLibrary = onRemoved,
        onDeletePermanently = {
            // Fallback — try direct delete (works if app opened file via ACTION_OPEN_DOCUMENT)
            FontRepository.removeFontFromStorage(font.id, context)
            onRemoved()
        }
    )
}
