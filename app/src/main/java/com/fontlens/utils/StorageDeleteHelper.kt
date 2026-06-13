package com.fontlens.utils

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StorageDeleteHelper(
    private val fragment: Fragment,
    private val onResult: (success: Boolean) -> Unit
) {
    // For API 29 — store URIs pending second delete attempt after permission granted
    private var pendingUris: List<Uri> = emptyList()

    private val intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest> =
        fragment.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // API 29 needs a second delete call after permission granted
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    fragment.lifecycleScope.launch {
                        var allDeleted = true
                        for (uri in pendingUris) {
                            if (!deleteUri(fragment.requireContext(), uri)) allDeleted = false
                        }
                        onResult(allDeleted)
                    }
                } else {
                    // API 30+ — system already deleted after permission
                    onResult(true)
                }
            } else {
                onResult(false)
            }
            pendingUris = emptyList()
        }

    /** Delete a single font file */
    fun requestDelete(uri: Uri) {
        requestDeleteMultiple(listOf(uri))
    }

    /** Delete multiple font files (used in batch selection delete) */
    fun requestDeleteMultiple(uris: List<Uri>) {
        fragment.lifecycleScope.launch {
            deleteUris(fragment.requireContext(), uris)
        }
    }

    private suspend fun deleteUris(context: Context, uris: List<Uri>) {
        withContext(Dispatchers.IO) {
            // SAF (Storage Access Framework) URIs from OPEN_DOCUMENT_TREE
            // These use DocumentsContract, not MediaStore
            val safUris   = uris.filter { isSafUri(it) }
            val mediaUris = uris.filter { !isSafUri(it) }

            // Delete SAF URIs directly — write permission was persisted at folder pick time
            for (uri in safUris) {
                try {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                } catch (_: Exception) {
                    // If DocumentsContract fails try raw delete
                    try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }
            }

            // Delete MediaStore URIs with proper per-API handling
            if (mediaUris.isNotEmpty()) {
                deleteMediaUris(context, mediaUris)
            } else {
                withContext(Dispatchers.Main) { onResult(true) }
            }
        }
    }

    private suspend fun deleteMediaUris(context: Context, uris: List<Uri>) {
        withContext(Dispatchers.IO) {
            try {
                when {
                    // API 30+ — createDeleteRequest handles everything, single permission dialog
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver, uris
                        )
                        pendingUris = uris
                        withContext(Dispatchers.Main) {
                            intentSenderLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                    }

                    // API 29 — try delete, catch SecurityException, show permission dialog,
                    // then delete again in launcher callback
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                        try {
                            for (uri in uris) context.contentResolver.delete(uri, null, null)
                            withContext(Dispatchers.Main) { onResult(true) }
                        } catch (e: SecurityException) {
                            val recoverable = e as? RecoverableSecurityException
                            val intentSender = recoverable?.userAction?.actionIntent?.intentSender
                            if (intentSender != null) {
                                pendingUris = uris
                                withContext(Dispatchers.Main) {
                                    intentSenderLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                            } else {
                                withContext(Dispatchers.Main) { onResult(false) }
                            }
                        }
                    }

                    // API 26-28 — direct delete, no permission dialog needed
                    else -> {
                        var allDeleted = true
                        for (uri in uris) {
                            try {
                                context.contentResolver.delete(uri, null, null)
                            } catch (_: Exception) { allDeleted = false }
                        }
                        withContext(Dispatchers.Main) { onResult(allDeleted) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    private suspend fun deleteUri(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isSafUri(uri)) {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                } else {
                    context.contentResolver.delete(uri, null, null) > 0
                }
            } catch (_: Exception) { false }
        }
    }

    private fun isSafUri(uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        return authority.endsWith(".documents") ||
               authority.endsWith(".downloads") ||
               authority == "com.android.externalstorage.documents" ||
               authority == "com.android.providers.downloads.documents" ||
               authority == "com.android.providers.media.documents"
    }
}
