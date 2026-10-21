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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

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

private val themeListingFragmentTarget = GboardMethodTarget(
    classType = "Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
    name = "J",
    parameterTypes = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    returnType = "Landroid/view/View;",
)

/**
 * Applies the user's palette directly to the stable SoftKeyboardView tree and adds a Theme Studio
 * entry over the native Theme section without replacing the fragment's view hierarchy.
 *
 * Gboard's theme engine is heavily obfuscated and its color map is keyed by resource IDs that are
 * renamed in every build. The keyboard widget classes, however, are stable across releases. The
 * runtime walks SoftKeyView descendants and repaints their backgrounds and label TextViews, which
 * gives arbitrary background/key/text/digit colors without touching Gboard's theme resources.
 *
 * The color call is placed right before SoftKeyboardView.onLayout returns so Gboard's own layout
 * pass cannot overwrite the palette, and SoftKeyView.onDraw re-asserts the key fill every frame.
 */
internal val gboardThemeStudioPatch = bytecodePatch(
    name = "Theme Studio",
    description = "Custom keyboard palette with live preview: background, keys, label text and " +
        "digit colors, opened from a floating Theme Studio button in the Theme section.",
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
        installThemeSectionHeaderHook()
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

private fun BytecodePatchContext.installThemeSectionHeaderHook() {
    val method = findMutableMethodOrNull(themeListingFragmentTarget)
    if (method == null) {
        println("[Theme Studio] ThemeListingFragment.J not found; Theme section entry skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.THEME_STUDIO_RUNTIME_INSTALL_THEME_SECTION_HEADER
    val reference = RuntimeAbiCatalog.abi(call).reference
    if (implementation.instructions.any { instruction ->
            instruction.isMethodReference(reference)
        }
    ) {
        println("[Theme Studio] Theme section entry hook already present.")
        return
    }
    // The success path returns the inflated FrameLayout held in v4; the null path returns v6.
    val returnIndex = implementation.instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.RETURN_OBJECT &&
            (instruction as? OneRegisterInstruction)?.registerA == 4
    }
    if (returnIndex < 0) {
        println("[Theme Studio] ThemeListingFragment.J success return not found; entry skipped.")
        return
    }
    method.addInstructions(
        returnIndex,
        RuntimeCallEmitter.invoke(call, "v4") + "\nmove-result-object v4",
    )
    println("[Theme Studio] Theme section entry hook installed.")
}
