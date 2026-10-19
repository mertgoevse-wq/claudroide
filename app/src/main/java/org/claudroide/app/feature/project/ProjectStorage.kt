package org.claudroide.app.feature.project

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * UI State for project directory access and persistence.
 */
data class ProjectFolderUiState(
    val activeProject: PersistedGrant? = null,
    val allProjects: List<PersistedGrant> = emptyList(),
    val restoreReport: RestoreReport? = null,
    val errorMessage: String? = null,
    val systemLimitationWarning: String? = null,
    val isLoading: Boolean = false,
) {
    val hasActiveProject: Boolean
        get() = activeProject != null
}

/**
 * Central storage and lifecycle manager for SAF-granted project directories.
 * Persists URI grants to Android SharedPreferences, validates them via ContentResolver
 * and UriAvailability, and exposes reactive StateFlow to the UI.
 */
class ProjectStorage(
    private val context: Context,
    private val uriAvailability: UriAvailability = ContentResolverUriAvailability(context),
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(ProjectFolderUiState())
    val state: StateFlow<ProjectFolderUiState> = _state.asStateFlow()

    init {
        restorePersistedGrants()
    }

    /**
     * Resolves the human-readable folder name from a SAF URI.
     */
    fun resolveDisplayName(uri: Uri): String {
        try {
            val documentUri = if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )
            } else {
                uri
            }

            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {
            // Fall back to document id extraction
        }

        try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val clean = docId.substringAfterLast(':').substringAfterLast('/')
            if (clean.isNotBlank()) return clean
        } catch (_: Exception) {
            // Fall back to uri segment
        }

        return uri.lastPathSegment ?: "Projektordner"
    }

    /**
     * Inspects whether the selected tree is at storage root level, where Android 11+
     * Scoped Storage enforces severe system restrictions.
     */
    fun detectSystemRootLimitation(uri: Uri): String? {
        val path = uri.path ?: ""
        val uriStr = uri.toString()
        val isRoot = path.endsWith(":") ||
                path.endsWith("%3A") ||
                uriStr.endsWith("%3A") ||
                path.equals("/tree/primary:", ignoreCase = true) ||
                uriStr.contains("primary%3A", ignoreCase = true) && !uriStr.substringAfter("primary%3A").contains("%2F") && !uriStr.substringAfter("primary%3A").contains("/")

        return if (isRoot) {
            "Hinweis zu Android Scoped Storage: Bei Freigabe des gesamten Hauptspeicher-Stamms (Root) schränkt Android den Zugriff auf System- und Android/data-Unterordner ein. Es wird empfohlen, einen spezifischen Projektordner zu wählen."
        } else {
            null
        }
    }

    /**
     * Called when the user picks a folder via ActivityResultContracts.OpenDocumentTree.
     */
    fun onFolderSelected(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // Persist permission with Android ContentResolver
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (sec: SecurityException) {
                // If write permission is not grantable by the provider, try read-only
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // Inspect actual persistable flags granted by the OS
            val actualPerm = context.contentResolver.persistedUriPermissions.find { it.uri == uri }
            val isWritable = actualPerm?.isWritePermission == true
            val isReadable = actualPerm?.isReadPermission == true || uriAvailability.isAvailable(uri.toString(), AccessMode.READ)

            if (!isReadable) {
                _state.value = _state.value.copy(
                    errorMessage = "Fehler: Ordnerberechtigung wurde vom System nicht gewährt."
                )
                return
            }

            val displayName = resolveDisplayName(uri)
            val rootWarning = detectSystemRootLimitation(uri)

            val grant = PersistedGrant(
                uriString = uri.toString(),
                displayName = displayName,
                path = displayName,
                readOnly = !isWritable
            )

            // Update in-memory grants list (avoid duplicate paths)
            val currentGrants = _state.value.allProjects.filterNot { it.uriString == grant.uriString }
            val updatedGrants = listOf(grant) + currentGrants

            // Update PersistentFolderAccessProvider for pure Kotlin compatibility
            val access = PersistentFolderAccessProvider.get().replaceAll(updatedGrants)
            PersistentFolderAccessProvider.initialize(access)

            // Persist to SharedPreferences
            saveGrantsToPrefs(updatedGrants, grant.uriString)

            _state.value = _state.value.copy(
                activeProject = grant,
                allProjects = updatedGrants,
                errorMessage = null,
                systemLimitationWarning = rootWarning
            )
        } catch (e: SecurityException) {
            _state.value = _state.value.copy(
                errorMessage = "Sicherheitsfehler beim Zugriff auf den Ordner: ${e.localizedMessage ?: "Zugriff verweigert"}"
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                errorMessage = "Unerwarteter Fehler: ${e.localizedMessage ?: "Ordner konnte nicht geöffnet werden"}"
            )
        }
    }

    /**
     * Handles cancellation of the SAF picker dialog (user backed out).
     */
    fun onPickerCancelled() {
        // Safe no-op, clears transient errors without dismissing or crashing Activity
        _state.value = _state.value.copy(errorMessage = null)
    }

    /**
     * Switches the active project to one of the previously persisted grants.
     */
    fun selectActiveProject(grant: PersistedGrant) {
        if (!uriAvailability.isAvailable(grant.uriString, AccessMode.READ)) {
            _state.value = _state.value.copy(
                errorMessage = "Projektordner „${grant.displayName}“ ist nicht mehr erreichbar."
            )
            return
        }

        saveGrantsToPrefs(_state.value.allProjects, grant.uriString)
        _state.value = _state.value.copy(
            activeProject = grant,
            errorMessage = null
        )
    }

    /**
     * Revokes access to a granted folder.
     */
    fun revokeFolder(grant: PersistedGrant) {
        try {
            val uri = Uri.parse(grant.uriString)
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Permission may already be released
        }

        val remaining = _state.value.allProjects.filterNot { it.uriString == grant.uriString }
        val newActive = if (_state.value.activeProject?.uriString == grant.uriString) {
            remaining.firstOrNull()
        } else {
            _state.value.activeProject
        }

        val access = PersistentFolderAccessProvider.get().stateAfterRevoke(grant.path)
        PersistentFolderAccessProvider.initialize(access)

        saveGrantsToPrefs(remaining, newActive?.uriString)

        _state.value = _state.value.copy(
            activeProject = newActive,
            allProjects = remaining,
            errorMessage = null
        )
    }

    /**
     * Restores persisted permissions on startup, reconciling with system grants.
     */
    fun restorePersistedGrants(): RestoreReport {
        val storedGrants = loadGrantsFromPrefs()
        val activeUri = prefs.getString(KEY_ACTIVE_PROJECT_URI, null)

        val systemGrants = try {
            context.contentResolver.persistedUriPermissions
        } catch (_: Exception) {
            emptyList()
        }

        val usable = mutableListOf<PersistedGrant>()
        val invalid = mutableListOf<InvalidGrant>()

        for (grant in storedGrants) {
            val grantUri = Uri.parse(grant.uriString)
            val hasSystemGrant = systemGrants.any { it.uri == grantUri }
            val isAccessible = uriAvailability.isAvailable(grant.uriString, AccessMode.READ)

            if (hasSystemGrant && isAccessible) {
                val actualPerm = systemGrants.find { it.uri == grantUri }
                val readOnly = actualPerm?.isWritePermission != true
                usable.add(grant.copy(readOnly = readOnly))
            } else {
                invalid.add(
                    InvalidGrant(
                        grant = grant,
                        reason = if (!hasSystemGrant) "Berechtigung vom System widerrufen." else "Ordner nicht erreichbar."
                    )
                )
            }
        }

        val activeGrant = usable.find { it.uriString == activeUri } ?: usable.firstOrNull()

        // Update PersistentFolderAccessProvider
        val access = PersistentFolderAccess(grants = usable)
        PersistentFolderAccessProvider.initialize(access)

        val report = RestoreReport(usable = usable, invalid = invalid)

        _state.value = _state.value.copy(
            activeProject = activeGrant,
            allProjects = usable,
            restoreReport = report,
            isLoading = false
        )

        return report
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun saveGrantsToPrefs(grants: List<PersistedGrant>, activeUri: String?) {
        try {
            val array = JSONArray()
            grants.forEach { grant ->
                val obj = JSONObject().apply {
                    put("uriString", grant.uriString)
                    put("displayName", grant.displayName)
                    put("path", grant.path)
                    put("readOnly", grant.readOnly)
                }
                array.put(obj)
            }
            prefs.edit()
                .putString(KEY_GRANTS_JSON, array.toString())
                .putString(KEY_ACTIVE_PROJECT_URI, activeUri)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun loadGrantsFromPrefs(): List<PersistedGrant> {
        val json = prefs.getString(KEY_GRANTS_JSON, null) ?: return emptyList()
        val list = mutableListOf<PersistedGrant>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PersistedGrant(
                        uriString = obj.getString("uriString"),
                        displayName = obj.getString("displayName"),
                        path = obj.getString("path"),
                        readOnly = obj.optBoolean("readOnly", false)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "claudroide_project_storage"
        private const val KEY_GRANTS_JSON = "persisted_grants_json"
        private const val KEY_ACTIVE_PROJECT_URI = "active_project_uri"

        @Volatile
        private var instance: ProjectStorage? = null

        fun get(context: Context): ProjectStorage {
            return instance ?: synchronized(this) {
                instance ?: ProjectStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}
