package com.akshaykadam.pixelboard.patches.gboard.registry

import com.google.gson.JsonParser
import com.akshaykadam.pixelboard.patches.gboard.features.clipboard.gboardClipboardVaultPatch
import com.akshaykadam.pixelboard.patches.gboard.features.clipboard.gboardInfiniteClipboardPatch
import com.akshaykadam.pixelboard.patches.gboard.features.optimizations.gboardDisableCloudBackupPatch
import com.akshaykadam.pixelboard.patches.gboard.features.optimizations.gboardPerformancePrivacyPatch
import com.akshaykadam.pixelboard.patches.gboard.features.theme.gboardThemeStudioPatch
import com.akshaykadam.pixelboard.patches.gboard.features.theme.gboardUnlockThemesPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.generated.GboardTargetAdmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GboardPublishedPatchCatalogContractTest {
    @Test
    fun catalogExposesEachPublishedRegistrationExactlyOnce() {
        val registrations = GboardPublishedPatchCatalog.publishedRegistrations

        assertTrue(registrations.isNotEmpty())
        assertEquals(10, registrations.size)
        assertEquals(registrations.size, registrations.map { patch -> patch.name }.distinct().size)
        assertSame(
            gboardAdvancedVoiceTypingPatch,
            registrations.single { patch -> patch.name == "Advanced Voice Typing" },
        )
        assertSame(
            gboardAiWritingToolsPatch,
            registrations.single { patch -> patch.name == "AI Writing Tools" },
        )
        assertSame(
            gboardSignatureBypassPatch,
            registrations.single { patch -> patch.name == "Add Gboard Signature Bypass" },
        )
        assertSame(
            gboardPackageRenamePatch,
            registrations.single { patch -> patch.name == "Package Rename" },
        )
        assertSame(
            gboardPerformancePrivacyPatch,
            registrations.single { patch -> patch.name == "Performance & Privacy" },
        )
        assertSame(
            gboardDisableCloudBackupPatch,
            registrations.single { patch -> patch.name == "Disable Cloud Backup" },
        )
        assertSame(
            gboardInfiniteClipboardPatch,
            registrations.single { patch -> patch.name == "Infinite Clipboard" },
        )
        assertSame(
            gboardClipboardVaultPatch,
            registrations.single { patch -> patch.name == "Secure Clipboard Vault" },
        )
        assertSame(
            gboardThemeStudioPatch,
            registrations.single { patch -> patch.name == "Theme Studio" },
        )
        assertSame(
            gboardUnlockThemesPatch,
            registrations.single { patch -> patch.name == "Unlock All Themes" },
        )
    }

    @Test
    fun catalogRendersThePublishedInventoryFromItsRegistrations() {
        val inventory = JsonParser.parseString(
            GboardPublishedPatchCatalog.publishedInventory("test-version"),
        ).asJsonObject
        val patches = inventory.getAsJsonArray("patches").map { element -> element.asJsonObject }

        assertEquals("test-version", inventory.get("version").asString)
        assertEquals(
            patches.map { patch -> patch.get("name").asString }.sorted(),
            patches.map { patch -> patch.get("name").asString },
        )

        val aiWriting = patches.single { patch ->
            patch.get("name").asString == "AI Writing Tools"
        }
        assertTrue(aiWriting.get("use").asBoolean)
        assertEquals(
            listOf("18.5.3.993608717-beta-arm64-v8a"),
            aiWriting.getAsJsonObject("compatiblePackages")
                .getAsJsonArray("com.google.android.inputmethod.latin")
                .map { version -> version.asString },
        )
    }

    @Test
    fun embeddedPatchListMatchesPublishedCatalog() {
        val resource = checkNotNull(javaClass.classLoader.getResource("patches-list.json")) {
            "patches-list.json is missing from the bundle resources"
        }
        val inventory = JsonParser.parseString(resource.readText()).asJsonObject
        val embeddedNames = inventory.getAsJsonArray("patches")
            .map { patch -> patch.asJsonObject.get("name").asString }
            .sorted()

        assertEquals(
            GboardPublishedPatchCatalog.publishedRegistrations
                .mapNotNull { patch -> patch.name }
                .sorted(),
            embeddedNames,
        )
        assertEquals(
            GboardPublishedPatchCatalog.PUBLISHED_DOWNLOAD_URL,
            inventory.get("download_url").asString,
        )
    }

    @Test
    fun catalogPublishesOnlyTheDistributionTargets() {
        val patches = generatedPublishedPatches()

        assertMatchesPublishedRegistrationCount(patches.size)
        assertEquals(
            setOf(GboardTargetAdmission.packageName),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages").keySet()
            }.toSet(),
        )
        assertTrue(
            GboardTargetAdmission.versionNames.containsAll(
                GboardPublishedPatchCatalog.publishedVersionNames,
            ),
        )
        assertEquals(
            GboardPublishedPatchCatalog.publishedVersionNames.toSet(),
            patches.flatMap { patch ->
                patch.getAsJsonObject("compatiblePackages")
                    .getAsJsonArray(GboardTargetAdmission.packageName)
                    .map { version -> version.asString }
            }.toSet(),
        )
    }
}
