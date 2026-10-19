package org.claudroide.app.feature.linux.terminal

data class TerminalDimensions(
    val cols: Int = 80,
    val rows: Int = 24,
    val widthPx: Int = 0,
    val heightPx: Int = 0
) {
    init {
        require(cols > 0) { "Terminal columns must be positive (got $cols)" }
        require(rows > 0) { "Terminal rows must be positive (got $rows)" }
    }
}
