package org.claudroide.app.feature.project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Handles Android Storage Access Framework flow for project folder selection.
 * Provides persistent URI permissions that survive app restarts.
 */
class ProjectSaver(
    private val context: Context,
    private val persistentAccess: PersistentFolderAccess,
    private val uriAvailability: UriAvailability
) {

    private val activityResultLauncher: ActivityResultLauncher<Intent>? =
        (context as? ComponentActivity)?.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleResult(result.resultCode, result.data)
        }

    /**
     * Opens the system folder picker to let the user choose a project directory.
     */
    fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        activityResultLauncher?.launch(intent)
    }

    /**
     * Handles the result from the folder picker.
     */
    private fun handleResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                persistUriPermission(uri)
            }
        }
    }

    /**
     * Takes persistable URI permission and stores it.
     */
    private fun persistUriPermission(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            val displayName = getDisplayName(uri)
            val path = getPathFromUri(uri) ?: uri.toString()

            val grant = PersistedGrant(
                uriString = uri.toString(),
                displayName = displayName,
                path = path,
                readOnly = false
            )

            val persistResult = if (context.contentResolver.persistedUriPermissions.any { it.uri == uri }) {
                PersistOutcome.PERSISTED
            } else {
                PersistOutcome.NOT_PERSISTED
            }

            val newAccess = persistentAccess.persist(grant, persistResult)
            onPersistComplete(newAccess, grant, persistResult)
        } catch (e: SecurityException) {
            onPersistError(e)
        }
    }

    /**
     * Gets the display name of the selected folder.
     */
    private fun getDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                } else "Unknown"
            } ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Attempts to get the filesystem path from the URI.
     * Note: SAF URIs don't always map to filesystem paths.
     */
    private fun getPathFromUri(uri: Uri): String? {
        return try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restores persisted permissions on app startup.
     * Must be called during app initialization.
     */
    fun restorePersistedPermissions(): RestoreReport {
        return persistentAccess.restore(uriAvailability)
    }

    /**
     * Revokes access to a specific folder.
     */
    fun revokeFolder(path: String): RevokeOutcome {
        val outcome = persistentAccess.revoke(path)
        if (outcome is RevokeOutcome.Revoked) {
            persistentAccess.grants.find { it.normalisedPath == path }?.let { grant ->
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(grant.uriString),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Permission may already be gone
                }
            }
        }
        return outcome
    }

    // Callbacks for UI updates
    var onPersistComplete: (PersistentFolderAccess, PersistedGrant, PersistOutcome) -> Unit = { _, _, _ -> }
    var onPersistError: (Exception) -> Unit = { _ -> }
    var onRestoreComplete: (RestoreReport) -> Unit = { _ -> }
}

/**
 * Default UriAvailability implementation that checks via ContentResolver.
 */
class ContentResolverUriAvailability(
    private val context: Context
) : UriAvailability {
    override fun isAvailable(uriString: String, mode: AccessMode): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val flags = when (mode) {
                AccessMode.READ -> Intent.FLAG_GRANT_READ_URI_PERMISSION
                AccessMode.WRITE -> Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.close() }
            true
        } catch (e: Exception) {
            false
        }
    }
}
