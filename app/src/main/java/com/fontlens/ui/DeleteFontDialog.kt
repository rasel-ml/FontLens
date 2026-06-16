package com.fontlens.ui

import android.app.AlertDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.TextView
import com.fontlens.R
import com.fontlens.data.FontItem
import com.fontlens.data.FontRepository
import com.fontlens.utils.ThemeManager

object DeleteFontDialog {

    fun show(
        context: Context,
        font: FontItem,
        onRemoveFromLibrary: () -> Unit,
        onDeletePermanently: () -> Unit
    ) {
        val name = font.effectiveMeta.family.ifEmpty { font.displayName }

        // Wrap with the full app theme so ?attr/ refs in the layout AND
        // in bg_loading_dialog / bg_delete_btn / bg_style_btn drawables resolve
        val themed = ContextThemeWrapper(context, ThemeManager.currentThemeResId(context))

        val view = LayoutInflater.from(themed).inflate(R.layout.dialog_delete_font, null)

        val tvName        = view.findViewById<TextView>(R.id.tv_delete_font_name)
        val tvMessage     = view.findViewById<TextView>(R.id.tv_delete_message)
        val btnDeletePerm = view.findViewById<TextView>(R.id.btn_delete_permanently)
        val btnRemoveLib  = view.findViewById<TextView>(R.id.btn_remove_library)

        tvName.text    = name
        tvMessage.text = "What would you like to do with this font?"

        val dialog = AlertDialog.Builder(themed)
            .setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnRemoveLib.setOnClickListener {
            dialog.dismiss()
            FontRepository.removeFont(font.id, context)
            onRemoveFromLibrary()
        }

        btnDeletePerm.setOnClickListener {
            dialog.dismiss()
            // Use same themed context for second dialog
            AlertDialog.Builder(themed)
                .setTitle("⚠ Permanently Delete")
                .setMessage("Delete \"$name\" from your device?\n\nThis cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> onDeletePermanently() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    fun show(
        context: Context,
        font: FontItem,
        onRemoved: () -> Unit
    ) = show(
        context = context,
        font = font,
        onRemoveFromLibrary = onRemoved,
        onDeletePermanently = {
            FontRepository.removeFontFromStorage(font.id, context)
            onRemoved()
        }
    )
}
