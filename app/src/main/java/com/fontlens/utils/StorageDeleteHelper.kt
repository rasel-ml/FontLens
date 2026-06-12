package com.fontlens.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Handles permanent file deletion with proper Android storage permissions.
 *
 * For SAF (Storage Access Framework) URIs from ACTION_OPEN_DOCUMENT_TREE:
 *   Uses DocumentsContract.deleteDocument() — works if write permission was persisted.
 *
 * For MediaStore URIs (Android 11+):
 *   Uses MediaStore.createDeleteRequest() which shows system permission dialog.
 *
 * For Android 10 MediaStore:
 *   Catches RecoverableSecurityException and shows recovery dialog.
 */
class StorageDeleteHelper(
    private val fragment: Fragment,
    private val onResult: (success: Boolean) -> Unit
) {
    private var pendingUri: Uri? = null

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        fragment.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val success = result.resultCode == Activity.RESULT_OK
            if (success) {
                // Permission granted — now actually delete
                pendingUri?.let { uri ->
                    val deleted = performDelete(fragment.requireContext(), uri)
                    onResult(deleted)
                }
            } else {
                onResult(false)
            }
            pendingUri = null
        }

    fun requestDelete(uri: Uri) {
        val context = fragment.requireContext()

        // SAF tree document URI — DocumentsContract.deleteDocument is the right API
        if (isSafUri(uri)) {
            val deleted = performDelete(context, uri)
            onResult(deleted)
            return
        }

        // MediaStore URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pending = MediaStore.createDeleteRequest(
                    context.contentResolver, listOf(uri))
                pendingUri = uri
                launcher.launch(IntentSenderRequest.Builder(pending).build())
            } catch (_: Exception) {
                onResult(performDelete(context, uri))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                onResult(performDelete(context, uri))
            } catch (e: android.app.RecoverableSecurityException) {
                pendingUri = uri
                launcher.launch(
                    IntentSenderRequest.Builder(
                        e.userAction.actionIntent.intentSender).build())
            } catch (_: Exception) {
                onResult(false)
            }
        } else {
            onResult(performDelete(context, uri))
        }
    }

    private fun isSafUri(uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        // SAF URIs have authorities like "com.android.externalstorage.documents"
        return authority.endsWith(".documents") ||
               authority.endsWith(".downloads") ||
               uri.scheme == "content" && uri.pathSegments.firstOrNull() == "tree"
    }

    private fun performDelete(context: Context, uri: Uri): Boolean {
        // Try SAF DocumentsContract first (works for folder-picked files with write permission)
        try {
            return DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {}

        // Fallback: contentResolver.delete (works for some URI types)
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }
}
