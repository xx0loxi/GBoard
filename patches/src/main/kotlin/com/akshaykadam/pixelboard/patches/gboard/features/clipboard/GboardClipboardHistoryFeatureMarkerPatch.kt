/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import com.akshaykadam.pixelboard.patches.gboard.shared.applyFeatureMarker
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.resourcePatch

internal val gboardClipboardHistoryFeatureMarkerPatch = resourcePatch(
    description = "Mark the clipboard history browser feature in the target APK.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(CLIPBOARD_HISTORY_FEATURE_MARKER)
    }
}

internal const val CLIPBOARD_HISTORY_FEATURE_MARKER =
    "com.akshaykadam.pixelboard.feature.clipboard_history"
