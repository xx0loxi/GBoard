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
import com.akshaykadam.pixelboard.patches.shared.BytecodePatchContext
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

private val softKeyboardLayoutTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;",
    name = "onLayout",
    parameterTypes = listOf("Z", "I", "I", "I", "I"),
    returnType = "V",
)

private val softKeyDrawTarget = GboardMethodTarget(
    classType = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;",
    name = "onDraw",
    parameterTypes = listOf("Landroid/graphics/Canvas;"),
    returnType = "V",
)

/**
 * Applies the user's palette directly to the stable SoftKeyboardView tree.
 *
 * Gboard's theme engine is heavily obfuscated and its color map is keyed by resource IDs that are
 * renamed in every build. The keyboard widget classes, however, are stable across releases. The
 * runtime walks SoftKeyView descendants and repaints their backgrounds and label TextViews, which
 * gives arbitrary background/key/text/digit colors without touching Gboard's theme resources.
 *
 * The color call is placed right before SoftKeyboardView.onLayout returns so Gboard's own layout
 * pass cannot overwrite the palette, and SoftKeyView.onDraw re-asserts the key fill every frame.
 * No Theme-section UI is modified: the palette is configured from the PixelBoard settings screen.
 */
internal val gboardThemeStudioPatch = bytecodePatch(
    name = "Theme Studio",
    description = "Custom keyboard palette with live preview: background, keys, label text and " +
        "digit colors, configured in the PixelBoard patches settings screen.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardPatchesSettingsPatch,
        gboardThemeStudioFeatureMarkerPatch,
    )

    execute {
        installKeyboardColorHook()
        installSoftKeyColorHook()
    }
}

private fun BytecodePatchContext.installKeyboardColorHook() {
    val method = findMutableMethodOrNull(softKeyboardLayoutTarget)
    if (method == null) {
        println("[Theme Studio] SoftKeyboardView.onLayout not found; color hook skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.THEME_STUDIO_RUNTIME_APPLY_KEYBOARD_COLORS
    val reference = RuntimeAbiCatalog.abi(call).reference
    if (implementation.instructions.any { instruction ->
            instruction.isMethodReference(reference)
        }
    ) {
        println("[Theme Studio] SoftKeyboardView color hook already present.")
        return
    }
    val returnIndex = implementation.instructions.indexOfLast { instruction ->
        instruction.opcode == Opcode.RETURN_VOID
    }
    if (returnIndex < 0) {
        println("[Theme Studio] SoftKeyboardView.onLayout return not found; color hook skipped.")
        return
    }
    // Resolve p0 to a concrete virtual register. SoftKeyboardView.onLayout can have a
    // register file large enough that p0 aliases a register above the 4-bit invoke limit.
    val firstParameter = implementation.registerCount - 6
    method.addInstructions(returnIndex, RuntimeCallEmitter.invoke(call, "v$firstParameter"))
    println("[Theme Studio] SoftKeyboardView color hook installed before return.")
}

private fun BytecodePatchContext.installSoftKeyColorHook() {
    val method = findMutableMethodOrNull(softKeyDrawTarget)
    if (method == null) {
        println("[Theme Studio] SoftKeyView.onDraw not found; key color hook skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.THEME_STUDIO_RUNTIME_APPLY_KEY_COLORS
    val reference = RuntimeAbiCatalog.abi(call).reference
    if (implementation.instructions.any { instruction ->
            instruction.isMethodReference(reference)
        }
    ) {
        println("[Theme Studio] SoftKeyView color hook already present.")
        return
    }
    val firstParameter = implementation.registerCount - 2
    method.addInstructions(0, RuntimeCallEmitter.invoke(call, "v$firstParameter"))
    println("[Theme Studio] SoftKeyView color hook installed.")
}
