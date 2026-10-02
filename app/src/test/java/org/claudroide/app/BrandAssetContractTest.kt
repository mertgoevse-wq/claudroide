package org.claudroide.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The brand assets' contract.
 *
 * The regression this guards is not hypothetical. The mark and the banner were
 * generated, committed under `assets/brand/`, and then never wired into the
 * app: there was no drawable, no alt text and no `painterResource` call
 * anywhere in the source. The files looked finished and the app looked
 * unbranded, and nothing failed, because missing artwork is not a compile
 * error.
 *
 * So these tests check the *shipped* resources, not the design documents. A
 * picture in `assets/` proves nothing; a drawable a screen can actually load
 * does.
 */
class BrandAssetContractTest {

    private val resRoot: File = File("src/main/res")

    private fun drawable(name: String): File? =
        resRoot.walkTopDown().firstOrNull { it.name == name }

    @Test
    fun theBrandImagesAreRealAppResources_notLooseFilesInAssets() {
        // The exact failure this class exists for: a finished-looking PNG that
        // no screen can reach.
        for (name in listOf("claudroide_banner.webp", "claudroide_mark.webp")) {
            assertTrue(
                "$name must exist under src/main/res so R.drawable can resolve it",
                drawable(name) != null,
            )
        }
    }

    @Test
    fun noShippedBrandImageExceedsTheSizeCap() {
        // 50 KB per image, from the asset rules. A launcher-sized PNG per
        // density is what pushes an APK up, and none of these need to be
        // larger than a phone screen can show.
        val capBytes = 50L * 1024L
        for (name in listOf("claudroide_banner.webp", "claudroide_mark.webp")) {
            val file = drawable(name) ?: continue
            val bytes = file.length()
            assertTrue(
                "$name is ${bytes / 1024} KB, over the ${capBytes / 1024} KB cap",
                bytes <= capBytes,
            )
        }
    }

    @Test
    fun everyBrandImageHasAltText_inBothLanguages() {
        // An image is invisible to a screen reader unless something describes
        // it, and the description has to exist in the user's language -- a
        // single English string would leave German users with nothing.
        for (locale in listOf("values", "values-de")) {
            val xml = File(resRoot, "$locale/strings.xml").readText()
            for (name in listOf("banner_alt_text", "brand_mark_alt_text")) {
                assertTrue(
                    "$locale has no non-empty description for $name",
                    Regex("<string name=\"$name\">[^<]+</string>").containsMatchIn(xml),
                )
            }
        }
    }

    @Test
    fun theTwoLocalesCarryTheSameKeys() {
        // A key that exists only in `values` silently falls back to English for
        // every German user, which is how a half-translated app happens.
        val keys = File(resRoot, "values/strings.xml")
            .readText()
            .let { Regex("name=\"([^\"]+)\"").findAll(it).map { m -> m.groupValues[1] }.toSet() }
        val deKeys = File(resRoot, "values-de/strings.xml")
            .readText()
            .let { Regex("name=\"([^\"]+)\"").findAll(it).map { m -> m.groupValues[1] }.toSet() }

        assertTrue(
            "German is missing keys present in English: ${(keys - deKeys).sorted()}",
            keys.none { it !in deKeys },
        )
    }

    @Test
    fun theDisplayedNameIsTheOneTheOwnerChose() {
        // Guards the rename from "Claudroide" to "ClauDroide". Only the
        // *display* name is capitalised this way; the package identifier
        // deliberately stays org.claudroide.app, so this test must not look at
        // it. What it does guard is the visible string drifting back.
        val appName = Regex("<string name=\"app_name\">([^<]+)</string>")
            .find(File(resRoot, "values/strings.xml").readText())!!.groupValues[1]
        assertEquals("ClauDroide", appName)
    }

    @Test
    fun theAltTextDescribesTheArtworkThatActuallyExists() {
        // The alt text used to say "the green robot mark beside the product
        // name", which described the previous single-droid banner. When the
        // artwork changed to two figures holding hands, the description did not
        // follow, and a screen-reader user was told about a picture that was no
        // longer on the screen. The description has to name both figures.
        val alt = Regex("<string name=\"banner_alt_text\">([^<]+)</string>")
            .find(File(resRoot, "values/strings.xml").readText())!!.groupValues[1]
        assertTrue(
            "banner alt text must mention both figures, was: $alt",
            alt.contains("Android") && alt.contains("bot") && alt.contains("hands"),
        )
    }

    @Test
    fun theBannerShowsTwoFigures_andTheMarkStaysTheSingleDroid() {
        // The artwork now carries a second bot, but the launcher icon and the
        // empty-state mark must stay the single dome: at 96dp two figures would
        // be unreadable, and the icon is the one asset that has to work at
        // 48dp in a notification shade. Read the shipped PNG dimensions rather
        // than trusting this comment.
        val banner = repoFile("assets/brand/banner.png")
        val mark = repoFile("assets/brand/mark-256.png")
        assertTrue("banner.png missing", banner.exists())
        assertTrue("mark-256.png missing", mark.exists())
        assertTrue(
            "the banner must be wide enough for two figures plus a wordmark",
            banner.readPngWidth() > mark.readPngWidth(),
        )
    }

    /**
     * Locate a repository file from the directory Gradle runs tests in.
     *
     * The working directory for `:app:testDebugUnitTest` is `app/`, so the
     * repository root is one level up. The first version used `../..`, which
     * pointed above the repository, and every asset test failed with
     * "banner.png missing" -- a wrong path wearing the costume of a missing
     * file, which is the kind of failure that makes you delete something that
     * was never broken. Each candidate is checked for existence, and the error
     * names the directory searched rather than just the file.
     */
    private fun repoFile(rel: String): File {
        val candidates = listOf(File(rel), File("..", rel), File("../..", rel))
        return candidates.firstOrNull { it.exists() }
            ?: error("cannot locate $rel from ${File(".").absolutePath}")
    }

    /** PNG width from the IHDR chunk; avoids pulling in an image library. */
    private fun File.readPngWidth(): Int {
        val head = readBytes().take(24)
        require(head.size >= 24) { "not a PNG: $name" }
        return ((head[16].toInt() and 0xFF) shl 24) or
            ((head[17].toInt() and 0xFF) shl 16) or
            ((head[18].toInt() and 0xFF) shl 8) or
            (head[19].toInt() and 0xFF)
    }
}
