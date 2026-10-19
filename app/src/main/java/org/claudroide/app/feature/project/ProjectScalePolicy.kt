package org.claudroide.app.feature.project

/**
 * Task 092 — "Large projects" (Große Projekte).
 *
 * Goal: keep large repositories usable on the A56 and avoid reading irrelevant
 * folders.
 *
 * The risk this task guards against is a device that quietly becomes slow or runs
 * out of memory, discovered only after the user has already waited. The ordering in
 * the type follows that: **estimate first, index only afterwards.** [ProjectScalePolicy.assess]
 * produces a [ScaleAssessment] and nothing in this file starts work. Indexing is a
 * decision the caller makes afterwards, with a different type
 * ([ScaleApproval]), so "we already began" is never the same statement as "we can
 * begin".
 *
 * Two guarantees are structural:
 *
 *  1. **Secret exclusions apply regardless of size rules.** [ScaleAssessment.secretPaths]
 *    is collected **before** and **independently of** the size estimate, and
 *    [ScaleApproval.indexablePaths] can only ever return paths that
 *    [ProjectExclusionPolicy] does not block. Making a repository large is
 *    therefore not a way to get a `.env` indexed. A test builds an approval over a
 *    list containing a secret and asserts it cannot come back out.
 *
 *  2. **The selection can be changed later.** [ScaleApproval.withExcludedFolders]
 *    returns a **new** approval, so a change of selection cannot mutate one an
 *    already-running index is holding. The same reasoning as
 *    [LocalFileSearch.withRevokedPaths].
 *
 * The numbers are project decisions stated as named constants. They are **not**
 * measurements taken on a Galaxy A56, and nothing here claims they are.
 */

/** How much work a project would be. */
enum class ScaleBand(val label: String) {

    /** Small enough to index whole. */
    COMFORTABLE("comfortable"),

    /** Usable, but exclusions are worth a look. */
    LARGE("large"),

    /** Indexing needs a decision first. */
    VERY_LARGE("very large"),

    /** Beyond what this app will index without narrowing the selection. */
    OUT_OF_RANGE("beyond the range this app indexes")
}

/** One folder the estimate found, and why it was treated the way it was. */
data class FolderEstimate(
    val path: String,
    val fileCount: Int,
    val totalBytes: Long,
    val isExcluded: Boolean = false,
    val excludeReason: String = ""
) {
    init {
        require(fileCount >= 0) { "A file count cannot be negative." }
        require(totalBytes >= 0L) { "A byte count cannot be negative." }
        require(!isExcluded || excludeReason.isNotBlank()) {
            "An excluded folder must say why."
        }
    }
}

/**
 * What a project would cost to index.
 *
 * This is a report. It starts nothing and grants nothing.
 *
 * @property hiddenByteCount an explicitly named **estimate**, never a measurement.
 *           It is a projection from a sample, and the name says so.
 */
data class ScaleAssessment(
    val projectName: String,
    val band: ScaleBand,
    val folders: List<FolderEstimate>,
    val fileCount: Int,
    val totalBytes: Long,
    val secretPaths: List<String>,
    val sampleFileCount: Int,
    val memoryNeededBytes: Long,
    val storageNeededBytes: Long
) {
    init {
        require(fileCount >= 0) { "A file count cannot be negative." }
        require(sampleFileCount >= 0) { "A sample count cannot be negative." }
        require(memoryNeededBytes >= 0L) { "A memory estimate cannot be negative." }
    }

    /** Folders the rules take out. */
    val excludedFolders: List<FolderEstimate> get() = folders.filter { it.isExcluded }

    /** Folders that would actually be indexed. */
    val indexableFolders: List<FolderEstimate> get() = folders.filterNot { it.isExcluded }

    /** Is a decision needed before indexing? */
    val needsDecision: Boolean
        get() = band == ScaleBand.VERY_LARGE || band == ScaleBand.OUT_OF_RANGE

    /** The estimate, plainly labelled as one. */
    fun estimateLines(): List<String> = buildList {
        add("Project: $projectName")
        add("Size: ${band.label}.")
        add("Files: $fileCount, total $totalBytes byte(s).")
        add(
            "Estimated from a sample of $sampleFileCount file(s). " +
                "This is an estimate, not a measurement of your device."
        )
        add("Estimated memory: $memoryNeededBytes byte(s).")
        add("Estimated index size on the device: $storageNeededBytes byte(s).")
        if (needsDecision) {
            add("This project needs a decision before indexing. Choose which folders to use.")
        }
        if (secretPaths.isNotEmpty()) {
            add(
                "${secretPaths.size} file(s) may hold credentials and are left out " +
                    "no matter how the size rules come out."
            )
        }
        if (excludedFolders.isNotEmpty()) {
            add("Folders left out (${excludedFolders.size}):")
            excludedFolders.forEach { add("  ${it.path} — ${it.excludeReason}") }
        }
    }
}

/**
 * What the user agreed to index.
 *
 * Produced only by [ProjectScalePolicy.approve]. It cannot be constructed directly
 * with a list that includes a blocked path, because [indexablePaths] filters again on
 * the way out.
 */
class ScaleApproval internal constructor(
    val projectName: String,
    private val includedFolderPaths: List<String>,
    private val allFolders: List<FolderEstimate>
) {

    val includedFolders: List<FolderEstimate>
        get() = allFolders.filter { it.path in includedFolderPaths }

    /**
     * The folders that may be indexed.
     *
     * Filtered a second time against [ProjectExclusionPolicy] on the way out. The
     * constructor is already internal, so this is belt and braces — but a secret
     * reaching the index is bad enough that the last step out is checked too.
     */
    val indexablePaths: List<String>
        get() = includedFolders
            .map { it.path }
            .filterNot { path ->
                ProjectExclusionPolicy.classify(path).decision ==
                    ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET
            }

    /** Folders the user left out. */
    val excludedFolders: List<FolderEstimate>
        get() = allFolders.filterNot { it.path in includedFolderPaths }

    val fileCount: Int get() = includedFolders.sumOf { it.fileCount }

    val totalBytes: Long get() = includedFolders.sumOf { it.totalBytes }

    /**
     * A new approval with [paths] left out.
     *
     * Returns a new instance. Mutating this one instead would change the selection
     * under an index that has already started, and the user would no longer know what
     * that index contains.
     */
    fun withExcludedFolders(paths: List<String>): ScaleApproval {
        val normalisiert = paths.map { it.trim().replace('\\', '/') }.filter { it.isNotEmpty() }
        return ScaleApproval(
            projectName,
            includedFolderPaths.filterNot { it in normalisiert },
            allFolders
        )
    }

    fun withOnlyFolders(paths: List<String>): ScaleApproval {
        val normalisiert = paths.map { it.trim().replace('\\', '/') }.filter { it.isNotEmpty() }
        return ScaleApproval(projectName, normalisiert, allFolders)
    }

    /** The lines for the user interface. */
    fun approvalLines(): List<String> = buildList {
        add("Indexing $projectName:")
        includedFolders.forEach { add("  ${it.path} — ${it.fileCount} file(s)") }
        add("Total: $fileCount file(s), $totalBytes byte(s).")
        if (excludedFolders.isNotEmpty()) {
            add("Left out (${excludedFolders.size}): ${excludedFolders.joinToString(", ") { it.path }}")
        }
        add("You can change this selection at any time before indexing starts.")
    }
}

/**
 * Estimates the cost of indexing a project, and records the decision to do it.
 */
object ProjectScalePolicy {

    /**
     * Above this many files a project is [ScaleBand.VERY_LARGE].
     *
     * A project decision, deliberately a named constant: a real threshold would need
     * a measurement on the target device, and there is none.
     */
    const val VERY_LARGE_FILE_COUNT: Int = 20_000

    /** Above this many files a project is [ScaleBand.OUT_OF_RANGE]. */
    const val OUT_OF_RANGE_FILE_COUNT: Int = 100_000

    /**
     * The multiplier applied to a sample when projecting the total byte count.
     *
     * Applies to **bytes only**. A file count coming out of a walk is a fact; scaling
     * it would be inventing a number the app then reports as if it had counted it.
     */
    const val BYTE_PROJECTION_SAFETY_FACTOR: Double = 1.0

    /** Bytes of memory assumed per indexed file. */
    const val MEMORY_BYTES_PER_FILE: Long = 4_096L

    /** Bytes of device storage assumed per indexed file. */
    const val STORAGE_BYTES_PER_FILE: Long = 1_024L

    /**
     * Estimates a project from what a walk already found.
     *
     * @param sampleBytes total bytes of the files actually looked at.
     * @param sampleCount how many files were actually looked at.
     * @param alreadyExcluded paths the user or the rules have taken out already.
     */
    fun assess(
        projectName: String,
        folders: List<FolderEstimate>,
        sampleBytes: Long,
        sampleCount: Int,
        alreadyExcluded: List<String> = emptyList()
    ): ScaleAssessment {
        val ausgeschlossen = alreadyExcluded.map { it.trim().replace('\\', '/') }.toSet()

        val bewertet = folders.map { folder ->
            val vomNutzerRaus = folder.path in ausgeschlossen
            val schwer = ScanRules.heavyDirectoryOf(folder.path)
            val versteckt = ScanRules.isHidden(folder.path)
            when {
                folder.isExcluded -> folder
                vomNutzerRaus -> folder.copy(
                    isExcluded = true,
                    excludeReason = "left out by you"
                )
                schwer != null -> folder.copy(
                    isExcluded = true,
                    excludeReason = "large folder ($schwer)"
                )
                versteckt -> folder.copy(
                    isExcluded = true,
                    excludeReason = "hidden folder"
                )
                else -> folder
            }
        }

        val verbleibend = bewertet.filterNot { it.isExcluded }
        val dateien = verbleibend.sumOf { it.fileCount }
        val bytes = verbleibend.sumOf { it.totalBytes }

        // Secrets are collected on their own, before and apart from the size rules.
        val geheimnisse = bewertet
            .filter { ProjectExclusionPolicy.classify(it.path).decision == ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET }
            .map { it.path }
        val endgueltig = bewertet.filterNot { it.path in geheimnisse }

        // The file count comes from the walk and is a fact, not a guess: multiplying
        // it by an extrapolation factor would turn a known number into an invented
        // one. Only the byte total is projected, because that is the value a walk
        // often does not finish computing.
        val geschaetzteDateien = dateien
        val geschaetzteBytes = if (sampleCount > 0 && sampleBytes > 0) {
            ((sampleBytes.toDouble() / sampleCount) * geschaetzteDateien * BYTE_PROJECTION_SAFETY_FACTOR)
                .toLong()
        } else {
            bytes
        }

        val band = when {
            geschaetzteDateien >= OUT_OF_RANGE_FILE_COUNT -> ScaleBand.OUT_OF_RANGE
            geschaetzteDateien >= VERY_LARGE_FILE_COUNT -> ScaleBand.VERY_LARGE
            geschaetzteDateien >= 5_000 -> ScaleBand.LARGE
            else -> ScaleBand.COMFORTABLE
        }

        return ScaleAssessment(
            projectName = projectName,
            band = band,
            folders = endgueltig,
            fileCount = geschaetzteDateien,
            totalBytes = geschaetzteBytes,
            secretPaths = geheimnisse,
            sampleFileCount = sampleCount,
            memoryNeededBytes = geschaetzteDateien * MEMORY_BYTES_PER_FILE,
            storageNeededBytes = geschaetzteDateien * STORAGE_BYTES_PER_FILE
        )
    }

    /**
     * Records the decision to index.
     *
     * A separate call from [assess] on purpose: the estimate does not start anything,
     * and the decision is a new object rather than a flag on the estimate.
     */
    fun approve(
        assessment: ScaleAssessment,
        onlyFolders: List<String> = emptyList()
    ): ScaleApproval = ScaleApproval(
        projectName = assessment.projectName,
        includedFolderPaths = when {
            onlyFolders.isNotEmpty() -> onlyFolders
            // A project that needs a decision does not silently index everything:
            // the caller has to name the folders.
            assessment.needsDecision -> emptyList()
            else -> assessment.indexableFolders.map { it.path }
        },
        allFolders = assessment.folders
    )
}
