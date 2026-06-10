package com.fontlens.ui

import android.app.AlertDialog
import android.content.Context
import com.fontlens.data.FontItem
import com.fontlens.data.FontRepository

object DeleteFontDialog {

    fun show(
        context: Context,
        font: FontItem,
        onRemoved: () -> Unit
    ) {
        val name = font.effectiveMeta.family.ifEmpty { font.displayName }

        // Custom dialog view with two buttons side by side
        val view = android.view.LayoutInflater.from(context)
            .inflate(com.fontlens.R.layout.dialog_delete_font, null)

        val tvName       = view.findViewById<android.widget.TextView>(com.fontlens.R.id.tv_delete_font_name)
        val tvMessage    = view.findViewById<android.widget.TextView>(com.fontlens.R.id.tv_delete_message)
        val btnDeletePerm = view.findViewById<android.widget.TextView>(com.fontlens.R.id.btn_delete_permanently)
        val btnRemoveLib = view.findViewById<android.widget.TextView>(com.fontlens.R.id.btn_remove_library)

        tvName.text    = name
        tvMessage.text = "What would you like to do with this font?"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnRemoveLib.setOnClickListener {
            dialog.dismiss()
            FontRepository.removeFont(font.id, context)
            onRemoved()
        }

        btnDeletePerm.setOnClickListener {
            dialog.dismiss()
            // Second confirmation
            AlertDialog.Builder(context)
                .setTitle("⚠ Permanently Delete")
                .setMessage("Delete \"$name\" from your device storage?\n\nThis cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    FontRepository.removeFontFromStorage(font.id, context)
                    onRemoved()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }
}
