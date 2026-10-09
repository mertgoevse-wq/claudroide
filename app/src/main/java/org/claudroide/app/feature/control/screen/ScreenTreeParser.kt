package org.claudroide.app.feature.control.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.claudroide.app.feature.control.model.RectBounds
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenElement
import org.claudroide.app.feature.control.model.ScreenSnapshot

/**
 * Parses native Android [AccessibilityNodeInfo] trees into structured [ScreenSnapshot]s.
 */
object ScreenTreeParser {

    private const val MAX_DEPTH = 32

    fun parse(
        rootNode: AccessibilityNodeInfo?,
        dimensions: ScreenDimensions,
        screenshotBase64: String? = null
    ): ScreenSnapshot {
        if (rootNode == null) {
            return ScreenSnapshot(
                packageName = "",
                dimensions = dimensions,
                elements = emptyList(),
                screenshotBase64 = screenshotBase64
            )
        }

        val elements = mutableListOf<ScreenElement>()
        val packageName = rootNode.packageName?.toString() ?: ""

        val rect = Rect()
        parseNodeRecursive(
            node = rootNode,
            path = "0",
            depth = 0,
            tempRect = rect,
            outList = elements
        )

        return ScreenSnapshot(
            packageName = packageName,
            activityName = null,
            dimensions = dimensions,
            elements = elements,
            screenshotBase64 = screenshotBase64
        )
    }

    private fun parseNodeRecursive(
        node: AccessibilityNodeInfo,
        path: String,
        depth: Int,
        tempRect: Rect,
        outList: MutableList<ScreenElement>
    ) {
        if (depth > MAX_DEPTH) return

        node.getBoundsInScreen(tempRect)
        val bounds = RectBounds(
            left = tempRect.left,
            top = tempRect.top,
            right = tempRect.right,
            bottom = tempRect.bottom
        )

        val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val contentDesc = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val viewId = node.viewIdResourceName?.toString()
        val className = node.className?.toString() ?: "android.view.View"
        val packageName = node.packageName?.toString() ?: ""

        val isVisible = node.isVisibleToUser && !bounds.isEmpty
        val isClickable = node.isClickable
        val isScrollable = node.isScrollable
        val isEditable = node.isEditable
        val isFocused = node.isFocused
        val isSelected = node.isSelected
        val isEnabled = node.isEnabled

        val elementId = if (!viewId.isNullOrBlank()) {
            "$path:$viewId"
        } else {
            "$path:${className.substringAfterLast('.')}"
        }

        // Only include elements that have meaningful text/desc, are interactive, or have IDs
        val hasContentOrAction = !text.isNullOrBlank() ||
                !contentDesc.isNullOrBlank() ||
                isClickable ||
                isScrollable ||
                isEditable ||
                !viewId.isNullOrBlank()

        if (isVisible && hasContentOrAction) {
            outList.add(
                ScreenElement(
                    id = elementId,
                    text = text,
                    contentDescription = contentDesc,
                    className = className,
                    packageName = packageName,
                    bounds = bounds,
                    isClickable = isClickable,
                    isScrollable = isScrollable,
                    isEditable = isEditable,
                    isFocused = isFocused,
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    isVisible = isVisible,
                    viewIdResourceName = viewId,
                    childCount = node.childCount
                )
            )
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (e: Exception) {
                null
            }
            if (child != null) {
                parseNodeRecursive(
                    node = child,
                    path = "$path/$i",
                    depth = depth + 1,
                    tempRect = tempRect,
                    outList = outList
                )
                child.recycle()
            }
        }
    }
}
