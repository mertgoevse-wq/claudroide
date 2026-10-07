package org.claudroide.app.feature.linux.terminal

enum class TerminalSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STARTING_SHELL,
    RUNNING,
    RECONNECTING,
    FAILED,
    CLOSING
}
