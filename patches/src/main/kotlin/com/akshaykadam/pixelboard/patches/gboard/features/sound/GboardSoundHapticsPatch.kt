/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.sound

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
import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

/**
 * PressEffectPlayerImpl in Gboard 18.5.3 (obfuscated as {@code afsv}):
 * - {@code g(View, int)} plays the key click and then delegates to the tap haptic;
 * - {@code d(View, int)} performs haptics for tap/release/text-handle effects;
 * - {@code f(int)} performs a vibration with the configured duration.
 *
 * The runtime rewrites the player's cached sound/vibration fields at method entry and replaces
 * the tap vibration with the user's duration and amplitude. Gboard's own preferences stay
 * untouched, so stock settings and preference observers keep working.
 */
private val soundEffectTarget = GboardMethodTarget(
    classType = "Lafsv;",
    name = "g",
    parameterTypes = listOf("Landroid/view/View;", "I"),
    returnType = "V",
)

private val hapticEffectTarget = GboardMethodTarget(
    classType = "Lafsv;",
    name = "d",
    parameterTypes = listOf("Landroid/view/View;", "I"),
    returnType = "V",
)

private val vibrationTarget = GboardMethodTarget(
    classType = "Lafsv;",
    name = "f",
    parameterTypes = listOf("I"),
    returnType = "V",
)

internal val gboardSoundHapticsPatch = bytecodePatch(
    name = "Sound & Haptics",
    description = "Override key press sound volume and vibration duration or amplitude without " +
        "changing Gboard's own sound and haptics preferences.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesExtensionCarrierPatch,
        gboardPatchesSettingsPatch,
        gboardSoundHapticsFeatureMarkerPatch,
    )

    execute {
        installSoundHook()
        installHapticHook()
        installVibrationHook()
    }
}

private fun BytecodePatchContext.installSoundHook() {
    val method = findMutableMethodOrNull(soundEffectTarget)
    if (method == null) {
        println("[Sound & Haptics] PressEffectPlayerImpl.playSound not found; sound override skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.SOUND_HAPTICS_RUNTIME_APPLY_SOUND
    if (implementation.instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        println("[Sound & Haptics] Sound hook already present.")
        return
    }
    val firstParameter = implementation.registerCount - 3
    if (firstParameter < 0) {
        println("[Sound & Haptics] Unexpected sound method registers; sound override skipped.")
        return
    }
    method.addInstructions(0, RuntimeCallEmitter.invoke(call, "v$firstParameter"))
    println("[Sound & Haptics] Sound override hook installed.")
}

private fun BytecodePatchContext.installHapticHook() {
    val method = findMutableMethodOrNull(hapticEffectTarget)
    if (method == null) {
        println("[Sound & Haptics] PressEffectPlayerImpl.performEffect not found; haptic override skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.SOUND_HAPTICS_RUNTIME_HANDLE_HAPTIC
    if (implementation.instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        println("[Sound & Haptics] Haptic hook already present.")
        return
    }
    val firstParameter = implementation.registerCount - 3
    if (firstParameter < 1) {
        println("[Sound & Haptics] No free local register for the haptic guard; override skipped.")
        return
    }
    val instructions = implementation.instructions
    method.addInstructionsWithLabels(
        0,
        """
            ${RuntimeCallEmitter.invoke(call, "v$firstParameter, v${firstParameter + 1}")}
            move-result v0
            if-eqz v0, :pixelboard_sound_haptics_continue
            return-void
        """.trimIndent(),
        ExternalLabel("pixelboard_sound_haptics_continue", instructions.first()),
    )
    println("[Sound & Haptics] Haptic override hook installed.")
}

private fun BytecodePatchContext.installVibrationHook() {
    val method = findMutableMethodOrNull(vibrationTarget)
    if (method == null) {
        println("[Sound & Haptics] PressEffectPlayerImpl.vibrate not found; vibration override skipped.")
        return
    }
    val implementation = method.implementation ?: return
    val call = RuntimeCallId.SOUND_HAPTICS_RUNTIME_OVERRIDE_VIBRATION
    if (implementation.instructions.any { it.isMethodReference(RuntimeAbiCatalog.abi(call).reference) }) {
        println("[Sound & Haptics] Vibration hook already present.")
        return
    }
    val firstParameter = implementation.registerCount - 2
    if (firstParameter < 1) {
        println("[Sound & Haptics] No free local register for the vibration guard; override skipped.")
        return
    }
    val instructions = implementation.instructions
    method.addInstructionsWithLabels(
        0,
        """
            ${RuntimeCallEmitter.invoke(call, "v$firstParameter, v${firstParameter + 1}")}
            move-result v0
            if-eqz v0, :pixelboard_sound_haptics_vibration_continue
            return-void
        """.trimIndent(),
        ExternalLabel("pixelboard_sound_haptics_vibration_continue", instructions.first()),
    )
    println("[Sound & Haptics] Vibration override hook installed.")
}
