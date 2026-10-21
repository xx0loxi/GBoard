/**
 * GBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Background-hook selection inspired by kveld-morphe-patches (GPLv3).
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.patches.gboard.features.optimizations

import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.shared.addInstructions
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch

/**
 * Broadcast receivers and workers that Gboard starts on boot, on package changes and on a
 * schedule. They all perform diagnostics, crash uploads or remote-config bookkeeping, none of
 * which is needed for typing. Returning early keeps the keyboard process quiet: fewer wake-ups,
 * less background I/O and no metric/telemetry uploads.
 *
 * Every target is resolved lazily; on builds where a class was renamed or repurposed the hook is
 * skipped instead of failing the whole patch.
 */
private val backgroundReceivers = listOf(
    GboardMethodTarget(
        classType = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
        name = "onReceive",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
        name = "onReceive",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lcom/google/android/libraries/phenotype/client/stable/PhenotypeUpdateBackgroundBroadcastReceiver;",
        name = "onReceive",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        returnType = "V",
    ),
    GboardMethodTarget(
        classType = "Lcom/google/android/libraries/phenotype/client/stable/AccountRemovedBroadcastReceiver;",
        name = "onReceive",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
        returnType = "V",
    ),
)

/** 18.5.3-only targets. Obfuscated names are stable within one Gboard build, so a miss is safe. */
private val versionSpecificTargets = listOf(
    // Native crash reporter (Primes).
    GboardMethodTarget(
        classType = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
        name = "a",
        parameterTypes = listOf("Lapxy;"),
        returnType = "V",
    ),
    // Daily-ping scheduler module: only enqueues the periodic telemetry worker.
    GboardMethodTarget(
        classType = "Lacgf;",
        name = "dR",
        parameterTypes = listOf("Landroid/content/Context;", "Lagox;"),
        returnType = "V",
    ),
    // Ads reporting widget intent.
    GboardMethodTarget(
        classType = "Lairb;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/net/Uri;"),
        returnType = "V",
    ),
)

internal val gboardPerformancePrivacyPatch = bytecodePatch(
    name = "Performance & Privacy",
    description = "Stops background diagnostics, crash reporting, remote-config sync and the " +
        "ad reporting widget to reduce wake-ups, network traffic and battery use.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        var hooked = 0

        (backgroundReceivers + versionSpecificTargets).forEach { target ->
            val method = findMutableMethodOrNull(target) ?: return@forEach
            method.addInstructions(0, "return-void")
            hooked++
        }

        println("[Performance & Privacy] Neutralized $hooked background hook(s).")
    }
}
