package org.claudroide.app.feature.linux.terminal

data class TerminalConfig(
    val termType: String = "xterm-256color",
    val initialDimensions: TerminalDimensions = TerminalDimensions(cols = 80, rows = 24),
    val connectTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 0,
    val autoReconnect: Boolean = true,
    val maxReconnectAttempts: Int = 3,
    val reconnectDelayMs: Long = 1000L,
    val maxScrollback: Int = 2000
)
