/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.theme

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

private val softKeyboardLayoutTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;",
    name = "onLayout",
    parameterTypes = listOf("Z", "I", "I", "I", "I"),
    returnType = "V",
)

/**
 * Applies the user's palette directly to the stable SoftKeyboardView tree.
 *
 * Gboard's theme engine is heavily obfuscated and its color map is keyed by resource IDs that are
 * renamed in every build. The keyboard widget classes, however, are stable across releases. The
 * runtime walks SoftKeyView descendants and tints their backgrounds and label TextViews, which
 * gives arbitrary background/key/text/digit colors without touching Gboard's theme resources.
 */
internal val gboardThemeStudioPatch = bytecodePatch(
    name = "Theme Studio",
    description = "Custom keyboard palette with live preview: background, keys, label text and " +
        "digit colors.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardPatchesSettingsPatch,
        gboardThemeStudioFeatureMarkerPatch,
    )

    execute {
        val method = findMutableMethodOrNull(softKeyboardLayoutTarget) ?: return@execute
        val implementation = method.implementation ?: return@execute
        val call = RuntimeCallId.THEME_STUDIO_RUNTIME_APPLY_KEYBOARD_COLORS
        val alreadyHooked = implementation.instructions.any { instruction ->
            instruction.isMethodReference(RuntimeAbiCatalog.abi(call).reference)
        }
        if (!alreadyHooked) {
            // Resolve p0 to a concrete virtual register. SoftKeyboardView.onLayout can have a
            // register file large enough that p0 aliases a register above the 4-bit invoke limit.
            val firstParameter = implementation.registerCount - 6
            method.addInstructions(
                0,
                RuntimeCallEmitter.invoke(call, "v$firstParameter"),
            )
            println("[Theme Studio] SoftKeyboardView color hook installed.")
        }
    }
}
