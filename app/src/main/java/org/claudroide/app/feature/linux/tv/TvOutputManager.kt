package org.claudroide.app.feature.linux.tv

import android.content.Context

enum class DisplayTarget {
    PHONE_ONLY,
    EXTERNAL_DISPLAY,
    MIRROR_BOTH
}

data class DisplayRoute(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val isPresentation: Boolean,
    val isWireless: Boolean = false
)

interface DisplayProvider {
    fun getAvailableDisplays(): List<DisplayRoute>
}

class AndroidDisplayProvider(private val context: Context) : DisplayProvider {
    override fun getAvailableDisplays(): List<DisplayRoute> {
        return try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val displays = dm?.displays ?: emptyArray()
            displays.map { d ->
                val metrics = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                d.getRealMetrics(metrics)
                val isPresentation = (d.flags and android.view.Display.FLAG_PRESENTATION) != 0
                val isWireless = (d.flags and android.view.Display.FLAG_PRIVATE) != 0 || d.name.contains("wireless", ignoreCase = true)
                DisplayRoute(
                    id = d.displayId,
                    name = d.name,
                    width = metrics.widthPixels,
                    height = metrics.heightPixels,
                    isPresentation = isPresentation,
                    isWireless = isWireless
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

/**
 * Handles routing the remote or local Linux desktop GUI through the A56
 * onto an external display/TV (HDMI Alt Mode or Cast).
 *
 * Implements fallback to PHONE_ONLY when no secondary display is detected,
 * per TV_OUTPUT_PLAN.md.
 */
class TvOutputManager(
    private val displayProvider: DisplayProvider? = null
) {
    var activeTarget: DisplayTarget = DisplayTarget.PHONE_ONLY
        private set

    var selectedRoute: DisplayRoute? = null
        private set

    var onRouteChangedListener: ((DisplayTarget, DisplayRoute?) -> Unit)? = null

    fun getAvailableRoutes(): List<DisplayRoute> {
        return displayProvider?.getAvailableDisplays() ?: emptyList()
    }

    fun hasExternalDisplay(): Boolean {
        return getAvailableRoutes().any { it.id != 0 && it.isPresentation }
    }

    fun routeDesktopToExternalDisplay(target: DisplayTarget, routeId: Int? = null): Boolean {
        if (target == DisplayTarget.PHONE_ONLY) {
            activeTarget = DisplayTarget.PHONE_ONLY
            selectedRoute = null
            onRouteChangedListener?.invoke(activeTarget, null)
            return true
        }

        val routes = getAvailableRoutes()
        val targetRoute = if (routeId != null) {
            routes.find { it.id == routeId }
        } else {
            routes.firstOrNull { it.id != 0 }
        }

        if (targetRoute == null) {
            // Fallback: phone-only is always acceptable; TV is enhancement
            activeTarget = DisplayTarget.PHONE_ONLY
            selectedRoute = null
            onRouteChangedListener?.invoke(activeTarget, null)
            return false
        }

        activeTarget = target
        selectedRoute = targetRoute
        onRouteChangedListener?.invoke(activeTarget, selectedRoute)
        return true
    }

    /** Compatibility signature matching original stub */
    fun routeDesktopToExternalDisplay(enabled: Boolean): Boolean {
        return routeDesktopToExternalDisplay(
            if (enabled) DisplayTarget.EXTERNAL_DISPLAY else DisplayTarget.PHONE_ONLY
        )
    }
}
