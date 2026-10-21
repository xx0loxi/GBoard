/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.theme

import com.akshaykadam.pixelboard.patches.gboard.shared.applyFeatureMarker
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.resourcePatch

internal val gboardThemeStudioFeatureMarkerPatch = resourcePatch(
    description = "Mark the Theme Studio feature in the target APK.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        applyFeatureMarker(THEME_STUDIO_FEATURE_MARKER)
    }
}

internal const val THEME_STUDIO_FEATURE_MARKER =
    "com.akshaykadam.pixelboard.feature.theme_studio"
