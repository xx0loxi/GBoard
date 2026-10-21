/**
 * GBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Ported from kveld-morphe-patches (GPLv3).
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.optimizations

import com.akshaykadam.pixelboard.patches.gboard.shared.ANDROID_NS
import com.akshaykadam.pixelboard.patches.gboard.shared.setAndroidAttribute
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.resourcePatch
import org.w3c.dom.Element

private val BACKUP_ATTRIBUTES = listOf(
    "backupAgent",
    "fullBackupContent",
    "dataExtractionRules",
    "backupInForeground",
)

internal val gboardDisableCloudBackupPatch = resourcePatch(
    name = "Disable Cloud Backup",
    description = "Keeps keyboard data and personal dictionary off Google Drive backups.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    finalize {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: return@use

            application.setAndroidAttribute("allowBackup", "false")
            BACKUP_ATTRIBUTES.forEach { attribute ->
                application.removeAttributeNS(ANDROID_NS, attribute)
                application.removeAttribute("android:$attribute")
            }
        }
    }
}
