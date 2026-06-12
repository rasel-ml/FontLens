package com.fontlens.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Handles permanent file deletion with proper Android storage permission.
 *
 * Android 11+ (API 30+): Uses MediaStore.createDeleteRequest() which shows
 * the system "Allow <app> to delete this file?" dialog.
 *
 * Android 10 (API 29): Catches RecoverableSecurityException and launches
 * the recovery intent.
 *
 * Android 8-9 (API 26-28): Direct delete via contentResolver works for
 * files the app has URI permission to (opened via document picker).
 *
 * Usage:
 *   val helper = StorageDeleteHelper(fragment) { success ->
 *       if (success) { // file deleted, remove from library }
 *   }
 *   helper.requestDelete(uri)
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
                pendingUri?.let { tryDirectDelete(fragment.requireContext(), it) }
            }
            onResult(success)
            pendingUri = null
        }

    fun requestDelete(uri: Uri) {
        val context = fragment.requireContext()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — system permission dialog
            try {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(uri)
                )
                pendingUri = uri
                launcher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            } catch (e: Exception) {
                // URI may not be a MediaStore URI (e.g. from SAF document tree)
                // Fall back to direct delete
                val deleted = tryDirectDelete(context, uri)
                onResult(deleted)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 — try direct, catch RecoverableSecurityException
            try {
                tryDirectDelete(context, uri)
                onResult(true)
            } catch (e: android.app.RecoverableSecurityException) {
                pendingUri = uri
                launcher.launch(
                    IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                )
            } catch (e: Exception) {
                onResult(false)
            }
        } else {
            // Android 8-9 — direct delete with SAF URI permission
            val deleted = tryDirectDelete(context, uri)
            onResult(deleted)
        }
    }

    private fun tryDirectDelete(context: Context, uri: Uri): Boolean {
        return try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            // Last resort — try DocumentsContract delete
            try {
                android.provider.DocumentsContract.deleteDocument(
                    context.contentResolver, uri
                )
            } catch (e2: Exception) {
                false
            }
        }
    }
}
