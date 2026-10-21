/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesSettingsPatch
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.resourcePatch

/**
 * Patch entry for the clipboard history browser. The browser itself lives in the settings
 * extension and reads the encrypted vault; this patch only ensures the vault, the settings screen
 * and the feature marker are present.
 */
internal val gboardClipboardHistoryPatch = resourcePatch(
    name = "Clipboard History",
    description = "Browse, search, copy and delete the encrypted clipboard items saved by the " +
        "Secure Clipboard Vault.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(
        gboardPatchesSettingsPatch,
        gboardClipboardVaultPatch,
        gboardClipboardHistoryFeatureMarkerPatch,
    )
}
