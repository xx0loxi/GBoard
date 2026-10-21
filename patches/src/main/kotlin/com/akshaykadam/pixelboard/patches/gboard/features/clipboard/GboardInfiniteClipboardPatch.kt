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
import com.akshaykadam.pixelboard.patches.gboard.shared.isOpcode
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/**
 * Removes the two eviction paths Gboard uses for clipboard history.
 *
 * Gboard normally keeps unpinned clips for one hour and trims the history to 100 entries.
 * Returning early from the periodic cleanup and from the insert-side trim keeps every copied
 * item for as long as the user wants. The native clipboard drawer reads the database directly,
 * so it is not affected by the legacy provider query limit.
 */
internal val gboardInfiniteClipboardPatch = bytecodePatch(
    name = "Infinite Clipboard",
    description = "Keep every copied item indefinitely. Removes Gboard's one-hour expiry and " +
        "the 100-entry clipboard history trim.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        val cleanup = findMutableMethodOrNull(
            GboardMethodTarget(
                classType = "Lldo;",
                name = "g",
                parameterTypes = emptyList(),
                returnType = "V",
            ),
        )
        if (cleanup != null) {
            cleanup.addInstructions(0, "return-void")
            println("[Infinite Clipboard] Disabled periodic clipboard eviction.")
        }

        val retention = findMutableMethodOrNull(
            GboardMethodTarget(
                classType = "Llcs;",
                name = "a",
                parameterTypes = listOf("Landroid/content/Context;"),
                returnType = "J",
            ),
        )
        if (retention != null) {
            retention.addInstructions(
                0,
                """
                    const-wide v0, 0x7fffffffffffffffL
                    return-wide v0
                """.trimIndent(),
            )
            println("[Infinite Clipboard] Removed clipboard retention limit.")
        }

        val trim = findMutableMethodOrNull(
            GboardMethodTarget(
                classType = "Llcs;",
                name = "k",
                parameterTypes = listOf("Landroid/content/Context;", "Z"),
                returnType = "Latfe;",
            ),
        )
        if (trim != null) {
            val instructions = trim.implementation?.instructions
            if (instructions != null) {
                val capIndex = instructions.indexOfFirst { instruction ->
                    instruction.isOpcode("RSUB_INT_LIT8") &&
                        (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 100 &&
                        (instruction as? TwoRegisterInstruction)?.registerA == 9
                }
                if (capIndex >= 0) {
                    // v7 already contains the unpinned history size at this point. Reusing it as
                    // the Math.min() bound keeps every non-pinned clip instead of the newest 100.
                    trim.replaceInstruction(capIndex, "move v9, v7")
                    println("[Infinite Clipboard] Removed the 100-entry history trim.")
                }
            }
        }
    }
}
