/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesSettingsPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

/**
 * Encrypted, on-device clipboard archive.
 *
 * Every item Gboard ingests is also written to an AES-GCM archive in app-private storage. The
 * archive survives reboots and APK updates because it lives in no-backup storage and is keyed by
 * Android Keystore. The existing clipboard-sync hook (a dependency of the settings patch) asks the
 * vault to re-insert missing items whenever an input view starts, so the native history drawer sees
 * them again.
 */
internal val gboardClipboardVaultPatch = bytecodePatch(
    name = "Secure Clipboard Vault",
    description = "Persist clipboard history across reboots and app updates in an encrypted, " +
        "app-private vault protected by Android Keystore.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardPatchesSettingsPatch,
        gboardClipboardVaultFeatureMarkerPatch,
    )

    execute {
        val captureCall = RuntimeCallId.CLIPBOARD_VAULT_RUNTIME_CAPTURE
        val capture = findMutableMethodOrNull(
            GboardMethodTarget(
                classType = "Lldo;",
                name = "b",
                parameterTypes = listOf(
                    "Landroid/content/Context;",
                    "Landroid/content/ClipData;",
                    "Landroid/content/ClipDescription;",
                    "Z",
                ),
                returnType = "Llao;",
            ),
        ) ?: return@execute

        val implementation = capture.implementation ?: return@execute
        val alreadyHooked = implementation.instructions.any { instruction ->
            instruction.isMethodReference(RuntimeAbiCatalog.abi(captureCall).reference)
        }
        if (!alreadyHooked) {
            // p1/p2 can alias high virtual registers in this method; resolve them to concrete
            // numbers so the emitter can switch to invoke-static/range when needed.
            val firstParameter = implementation.registerCount - 5
            val contextRegister = firstParameter + 1
            val clipDataRegister = firstParameter + 2
            capture.addInstructions(
                0,
                RuntimeCallEmitter.invoke(
                    captureCall,
                    "v$contextRegister, v$clipDataRegister",
                ),
            )
            println("[Secure Clipboard Vault] Clipboard capture hook installed.")
        }
    }
}
