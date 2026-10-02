package org.claudroide.app

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
}
