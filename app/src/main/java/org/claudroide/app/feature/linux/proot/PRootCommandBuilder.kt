package org.claudroide.app.feature.linux.proot

/**
 * Builds standard CLI arguments for launching rootless Linux processes under PRoot on Android.
 * Follows Termux and proot standard conventions:
 * - Link2symlink to handle non-hardlink Android storage partitions
 * - Fake root (-0) for apt/dpkg package management
 * - Standard /dev, /proc, /sys bindings
 */
object PRootCommandBuilder {

    fun buildCommandLine(config: PRootConfig): List<String> {
        val args = mutableListOf<String>()

        // 1. Binary invocation
        args.add(config.prootBinaryPath)

        // 2. Flags
        if (config.link2symlink) {
            args.add("--link2symlink")
        }
        if (config.fakeRoot) {
            args.add("-0")
        }
        if (config.killOnExit) {
            args.add("--kill-on-exit")
        }

        // 3. Rootfs and working directory
        args.add("-r")
        args.add(config.rootfsPath)

        if (config.workingDir.isNotBlank()) {
            args.add("-w")
            args.add(config.workingDir)
        }

        // 4. Mount bindings
        for (mount in config.mounts) {
            args.add("-b")
            args.add(mount.toCliArgument())
        }

        // 5. Custom arguments
        args.addAll(config.customArgs)

        // 6. Target command (or shell)
        args.addAll(config.command)

        return args
    }

    /**
     * Formats command line for logging or shell script export.
     */
    fun formatCommandString(config: PRootConfig): String {
        return buildCommandLine(config).joinToString(" ") { token ->
            if (token.contains(" ") || token.contains("$")) {
                "\"$token\""
            } else {
                token
            }
        }
    }
}
