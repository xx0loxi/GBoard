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
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val themeSectionAdapterTarget = GboardMethodTarget(
    classType = "Lvjr;",
    name = "p",
    parameterTypes = listOf("Lra;", "I"),
    returnType = "V",
)

/**
 * Expands every theme category instead of showing one row plus a "Show more" card.
 *
 * The theme listing adapter stores the current per-category cap in `vjr.l` and reads it right
 * before handing the count to the item adapter. Replacing that read with the same 10000 that
 * Gboard uses after the "Show more" tap makes every downloaded theme visible immediately.
 */
internal val gboardUnlockThemesPatch = bytecodePatch(
    name = "Unlock All Themes",
    description = "Show every theme in each category at once instead of hiding the rest behind " +
        "the \"Show more\" card.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        val method = findMutableMethodOrNull(themeSectionAdapterTarget) ?: return@execute
        val instructions = method.implementation?.instructions ?: return@execute
        val capIndex = instructions.indexOfFirst { instruction ->
            if (instruction.opcode != Opcode.IGET) {
                return@indexOfFirst false
            }
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference != null &&
                reference.definingClass == "Lvjr;" &&
                reference.name == "l" &&
                reference.type == "I"
        }
        if (capIndex < 0) {
            return@execute
        }
        val register = (instructions[capIndex] as? OneRegisterInstruction)?.registerA
            ?: return@execute
        method.replaceInstruction(capIndex, "const/16 v$register, 10000")
        println("[Unlock All Themes] Theme category expansion installed.")
    }
}
