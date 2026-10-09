package org.claudroide.app.feature.control.screen

import org.claudroide.app.feature.control.model.ScreenElement
import org.claudroide.app.feature.control.model.ScreenSnapshot

/**
 * Result of comparing two screen snapshots (before and after an action).
 */
data class ScreenDiff(
    val hasChanged: Boolean,
    val packageChanged: Boolean,
    val previousPackage: String,
    val currentPackage: String,
    val addedElements: List<ScreenElement>,
    val removedElements: List<ScreenElement>,
    val movedOrModifiedElements: List<ScreenElement>,
    val isDialogOrKeyboardAppeared: Boolean,
    val isAppCrashDetected: Boolean,
    val isPermissionRequested: Boolean,
    val changeScore: Float,
    val summary: String
)

/**
 * Change detector engine for verifying actions and observing UI transitions.
 */
object ScreenChangeDetector {

    private val CRASH_TEXT_INDICATORS = setOf(
        "has stopped",
        "keeps stopping",
        "isn't responding",
        "angehalten",
        "reagiert nicht",
        "wurde beendet"
    )

    private val PERMISSION_PACKAGE_INDICATORS = setOf(
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.samsung.android.permissioncontroller"
    )

    fun computeDiff(before: ScreenSnapshot?, after: ScreenSnapshot): ScreenDiff {
        if (before == null) {
            return ScreenDiff(
                hasChanged = true,
                packageChanged = false,
                previousPackage = "",
                currentPackage = after.packageName,
                addedElements = after.elements,
                removedElements = emptyList(),
                movedOrModifiedElements = emptyList(),
                isDialogOrKeyboardAppeared = false,
                isAppCrashDetected = checkAppCrash(after),
                isPermissionRequested = checkPermission(after),
                changeScore = 1.0f,
                summary = "Initial screen observation of ${after.packageName} with ${after.elements.size} elements"
            )
        }

        val packageChanged = before.packageName != after.packageName
        val beforeElementsById = before.elements.associateBy { it.id }
        val afterElementsById = after.elements.associateBy { it.id }

        val added = after.elements.filter { it.id !in beforeElementsById }
        val removed = before.elements.filter { it.id !in afterElementsById }

        val movedOrModified = after.elements.filter { afterElem ->
            val beforeElem = beforeElementsById[afterElem.id] ?: return@filter false
            beforeElem.bounds != afterElem.bounds ||
                    beforeElem.text != afterElem.text ||
                    beforeElem.isEnabled != afterElem.isEnabled ||
                    beforeElem.isSelected != afterElem.isSelected
        }

        val totalElements = (before.elements.size + after.elements.size).coerceAtLeast(1)
        val changedElementCount = added.size + removed.size + movedOrModified.size
        val rawScore = (changedElementCount.toFloat() / totalElements).coerceIn(0.0f, 1.0f)
        val changeScore = if (packageChanged) 1.0f else rawScore

        val isCrash = checkAppCrash(after)
        val isPermission = checkPermission(after)
        val isDialogOrKeyboard = checkDialogOrKeyboard(before, after)
        val hasChanged = packageChanged || changedElementCount > 0

        val summary = buildSummary(
            packageChanged = packageChanged,
            beforePkg = before.packageName,
            afterPkg = after.packageName,
            addedCount = added.size,
            removedCount = removed.size,
            modifiedCount = movedOrModified.size,
            isCrash = isCrash,
            isPermission = isPermission,
            isDialogOrKeyboard = isDialogOrKeyboard
        )

        return ScreenDiff(
            hasChanged = hasChanged,
            packageChanged = packageChanged,
            previousPackage = before.packageName,
            currentPackage = after.packageName,
            addedElements = added,
            removedElements = removed,
            movedOrModifiedElements = movedOrModified,
            isDialogOrKeyboardAppeared = isDialogOrKeyboard,
            isAppCrashDetected = isCrash,
            isPermissionRequested = isPermission,
            changeScore = changeScore,
            summary = summary
        )
    }

    private fun checkAppCrash(snapshot: ScreenSnapshot): Boolean {
        val allText = snapshot.visibleText.joinToString(" ").lowercase()
        return CRASH_TEXT_INDICATORS.any { allText.contains(it) }
    }

    private fun checkPermission(snapshot: ScreenSnapshot): Boolean {
        if (snapshot.packageName in PERMISSION_PACKAGE_INDICATORS) return true
        val allText = snapshot.visibleText.joinToString(" ").lowercase()
        return allText.contains("allow") && allText.contains("permission") ||
                allText.contains("zulassen") && allText.contains("berechtigung")
    }

    private fun checkDialogOrKeyboard(before: ScreenSnapshot, after: ScreenSnapshot): Boolean {
        val imePresent = after.elements.any { it.packageName.contains("inputmethod") || it.className.contains("KeyboardView") }
        val dialogPresent = after.elements.any { it.className.contains("Dialog") || it.className.contains("PopupWindow") }
        return imePresent || dialogPresent
    }

    private fun buildSummary(
        packageChanged: Boolean,
        beforePkg: String,
        afterPkg: String,
        addedCount: Int,
        removedCount: Int,
        modifiedCount: Int,
        isCrash: Boolean,
        isPermission: Boolean,
        isDialogOrKeyboard: Boolean
    ): String {
        return buildString {
            if (isCrash) append("[CRASH_DETECTED] ")
            if (isPermission) append("[PERMISSION_REQUESTED] ")
            if (isDialogOrKeyboard) append("[DIALOG_OR_KEYBOARD] ")
            if (packageChanged) {
                append("Package changed from '$beforePkg' to '$afterPkg'. ")
            }
            append("Elements: +$addedCount, -$removedCount, ~$modifiedCount")
        }
    }
}
