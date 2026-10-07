package org.claudroide.app.feature.linux.server

enum class VncBackendType {
    XVFB_X0VNCSERVER,
    TIGER_VNC_STANDALONE,
    X11VNC
}

data class HeadlessConfig(
    val virtualResolutionWidth: Int = 1920,
    val virtualResolutionHeight: Int = 1080,
    val virtualDpi: Int = 160,
    val bindLoopbackOnly: Boolean = true,
    val disableDisplayManager: Boolean = true,
    val exportXvfb: Boolean = true,
    val vncBackend: VncBackendType = VncBackendType.XVFB_X0VNCSERVER
) {
    /**
     * Generates Xvfb / headless display startup command for Linux userland.
     */
    fun generateDisplayCommand(displayNum: Int = 1): String {
        return "Xvfb :$displayNum -screen 0 ${virtualResolutionWidth}x${virtualResolutionHeight}x24 -dpi $virtualDpi"
    }

    /**
     * Generates VNC server startup command bound strictly to loopback (127.0.0.1) for secure tunnel access,
     * supporting different VNC server implementations.
     */
    fun generateVncServerCommand(
        displayNum: Int = 1,
        rfbPort: Int = 5900,
        backend: VncBackendType = vncBackend
    ): String {
        val localhostArg = if (bindLoopbackOnly) "-localhost yes" else "-localhost no"

        return when (backend) {
            VncBackendType.XVFB_X0VNCSERVER -> {
                "x0vncserver -display :$displayNum -rfbport $rfbPort $localhostArg -SecurityTypes None"
            }
            VncBackendType.TIGER_VNC_STANDALONE -> {
                val geom = "${virtualResolutionWidth}x${virtualResolutionHeight}"
                val listenArg = if (bindLoopbackOnly) "-localhost" else ""
                "tigervncserver :$displayNum -geometry $geom -depth 24 -rfbport $rfbPort $listenArg -SecurityTypes None"
            }
            VncBackendType.X11VNC -> {
                val listenArg = if (bindLoopbackOnly) "-listen 127.0.0.1" else ""
                "x11vnc -display :$displayNum -rfbport $rfbPort $listenArg -nopw -forever -shared"
            }
        }
    }
}
