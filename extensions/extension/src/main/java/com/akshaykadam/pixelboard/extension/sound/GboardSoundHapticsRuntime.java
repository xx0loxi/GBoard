/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.sound;

import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime entry points called from Gboard's obfuscated press-effect player
 * ({@code PressEffectPlayerImpl}).
 *
 * <p>Gboard reads its sound and vibration preferences into plain fields and only consults them
 * inside the player methods. Rewriting those fields at method entry gives arbitrary volume,
 * duration and amplitude without touching Gboard's own preference database, so the stock
 * Settings screens keep working and our overrides win again on the next key press.</p>
 */
public final class GboardSoundHapticsRuntime {
    private static final String TAG = "GboardSoundHaptics";
    private static final int VIBRATION_USAGE_HARDWARE_FEEDBACK = 18;
    private static final int MAX_SOUND_VOLUME = 100;
    private static final int MAX_VIBRATION_DURATION = 100;
    private static final int MAX_VIBRATION_AMPLITUDE = 255;

    private static volatile Field soundEnabledField;
    private static volatile Field soundVolumeField;
    private static volatile Field hapticEnabledField;
    private static volatile Field vibrationDurationField;
    private static volatile Field contextField;
    private static volatile boolean fieldsResolved;
    private static volatile boolean warningLogged;

    private GboardSoundHapticsRuntime() {
    }

    /**
     * Called at the start of {@code PressEffectPlayerImpl.playSoundEffect(View, int)}.
     */
    public static void applySound(Object player) {
        if (player == null) {
            return;
        }
        try {
            Context context = resolveContext(player);
            if (context == null) {
                return;
            }
            Snapshot current = snapshot(context);
            if (!current.enabled) {
                return;
            }
            resolveFields(player.getClass());
            if (soundEnabledField == null || soundVolumeField == null) {
                return;
            }
            soundEnabledField.setBoolean(player, current.soundEnabled);
            soundVolumeField.setFloat(player, current.soundVolume / (float) MAX_SOUND_VOLUME);
        } catch (Throwable throwable) {
            warnOnce("Failed to apply sound overrides", throwable);
        }
    }

    /**
     * Called at the start of {@code PressEffectPlayerImpl.performHapticEffect(View, int)}.
     *
     * @return {@code true} when the custom vibration already ran and the stock body must be
     *     skipped; {@code false} to let Gboard handle the effect as usual.
     */
    public static boolean handleHaptic(Object player, int effect) {
        if (player == null) {
            return false;
        }
        try {
            Context context = resolveContext(player);
            if (context == null) {
                return false;
            }
            Snapshot current = snapshot(context);
            if (!current.enabled) {
                return false;
            }
            resolveFields(player.getClass());
            if (hapticEnabledField == null || vibrationDurationField == null) {
                return false;
            }
            hapticEnabledField.setBoolean(player, current.hapticsEnabled);
            vibrationDurationField.setInt(player, current.vibrationDuration);
            if (effect != 0) {
                // Key release and text-handle effects keep their stock routing; only the tap
                // effect is fully replaced.
                return false;
            }
            if (!current.hapticsEnabled || current.vibrationDuration <= 0) {
                return true;
            }
            return vibrate(context, current.vibrationDuration, current.vibrationAmplitude);
        } catch (Throwable throwable) {
            warnOnce("Failed to apply haptic overrides", throwable);
            return false;
        }
    }

    /**
     * Called at the start of {@code PressEffectPlayerImpl.vibrate(int)}. Covers call sites that
     * invoke the player directly instead of going through the tap effect.
     *
     * @return {@code true} when the custom vibration already ran and the stock body must be
     *     skipped; {@code false} to let Gboard vibrate as usual.
     */
    public static boolean overrideVibration(Object player, int duration) {
        if (player == null) {
            return false;
        }
        try {
            Context context = resolveContext(player);
            if (context == null) {
                return false;
            }
            Snapshot current = snapshot(context);
            if (!current.enabled) {
                return false;
            }
            resolveFields(player.getClass());
            if (hapticEnabledField == null || vibrationDurationField == null) {
                return false;
            }
            hapticEnabledField.setBoolean(player, current.hapticsEnabled);
            vibrationDurationField.setInt(player, current.vibrationDuration);
            if (!current.hapticsEnabled) {
                return true;
            }
            if (current.vibrationDuration <= 0) {
                return true;
            }
            return vibrate(context, current.vibrationDuration, current.vibrationAmplitude);
        } catch (Throwable throwable) {
            warnOnce("Failed to override vibration", throwable);
            return false;
        }
    }

    private static boolean vibrate(Context context, int duration, int amplitude) {
        try {
            Object service = context.getSystemService(Context.VIBRATOR_SERVICE);
            if (!(service instanceof Vibrator)) {
                return false;
            }
            Vibrator vibrator = (Vibrator) service;
            if (!vibrator.hasVibrator()) {
                return false;
            }
            int safeDuration = clamp(duration, 1, MAX_VIBRATION_DURATION);
            int safeAmplitude = amplitude <= 0 ? -1 : clamp(amplitude, 1, MAX_VIBRATION_AMPLITUDE);
            Object effect = createOneShot(safeDuration, safeAmplitude);
            if (effect != null && vibrateWithEffect(vibrator, effect)) {
                return true;
            }
            vibrator.vibrate(safeDuration);
            return true;
        } catch (Throwable throwable) {
            warnOnce("Failed to perform vibration", throwable);
            return false;
        }
    }

    private static Object createOneShot(int duration, int amplitude) {
        try {
            Class<?> effectClass = Class.forName("android.os.VibrationEffect");
            Method createOneShot = effectClass.getMethod(
                    "createOneShot", long.class, int.class);
            return createOneShot.invoke(null, (long) duration, amplitude);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean vibrateWithEffect(Vibrator vibrator, Object effect) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Class<?> effectClass = Class.forName("android.os.VibrationEffect");
                Class<?> attributesClass = Class.forName("android.os.VibrationAttributes");
                Method createForUsage = attributesClass.getMethod("createForUsage", int.class);
                Object attributes = createForUsage.invoke(
                        null, VIBRATION_USAGE_HARDWARE_FEEDBACK);
                Method vibrate = Vibrator.class.getMethod(
                        "vibrate", effectClass, attributesClass);
                vibrate.invoke(vibrator, effect, attributes);
                return true;
            } catch (Throwable ignored) {
            }
        }
        try {
            Class<?> effectClass = Class.forName("android.os.VibrationEffect");
            Method vibrate = Vibrator.class.getMethod("vibrate", effectClass);
            vibrate.invoke(vibrator, effect);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Context resolveContext(Object player) {
        try {
            resolveFields(player.getClass());
            Field field = contextField;
            if (field != null) {
                Object value = field.get(player);
                if (value instanceof Context) {
                    return (Context) value;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            currentApplication.setAccessible(true);
            Object application = currentApplication.invoke(null);
            if (application instanceof Context) {
                return (Context) application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void resolveFields(Class<?> playerClass) {
        if (fieldsResolved) {
            return;
        }
        synchronized (GboardSoundHapticsRuntime.class) {
            if (fieldsResolved) {
                return;
            }
            Field soundEnabled = findField(playerClass, "e", boolean.class);
            Field soundVolume = findField(playerClass, "f", float.class);
            Field hapticEnabled = findField(playerClass, "d", boolean.class);
            Field vibrationDuration = findField(playerClass, "n", int.class);
            Field context = findField(playerClass, "h", Context.class);
            if (context == null) {
                for (Field candidate : playerClass.getDeclaredFields()) {
                    if (Context.class.isAssignableFrom(candidate.getType())) {
                        candidate.setAccessible(true);
                        context = candidate;
                        break;
                    }
                }
            }
            soundEnabledField = soundEnabled;
            soundVolumeField = soundVolume;
            hapticEnabledField = hapticEnabled;
            vibrationDurationField = vibrationDuration;
            contextField = context;
            fieldsResolved = true;
            if (soundEnabled == null || soundVolume == null
                    || hapticEnabled == null || vibrationDuration == null) {
                Log.w(TAG, "Press-effect player layout not recognized; overrides disabled.");
            }
        }
    }

    private static Field findField(Class<?> owner, String name, Class<?> type) {
        try {
            Field field = owner.getDeclaredField(name);
            if (!type.isAssignableFrom(field.getType())) {
                return null;
            }
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Snapshot snapshot(Context context) {
        return new Snapshot(
                GboardSoundHapticsSettings.isEnabled(context),
                GboardSoundHapticsSettings.isSoundEnabled(context),
                GboardSoundHapticsSettings.getSoundVolume(context),
                GboardSoundHapticsSettings.isHapticsEnabled(context),
                GboardSoundHapticsSettings.getVibrationDuration(context),
                GboardSoundHapticsSettings.getVibrationAmplitude(context));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (warningLogged) {
            return;
        }
        warningLogged = true;
        try {
            Log.w(TAG, message, throwable);
        } catch (Throwable ignored) {
        }
    }

    private static final class Snapshot {
        final boolean enabled;
        final boolean soundEnabled;
        final int soundVolume;
        final boolean hapticsEnabled;
        final int vibrationDuration;
        final int vibrationAmplitude;

        Snapshot(boolean enabled, boolean soundEnabled, int soundVolume, boolean hapticsEnabled,
                int vibrationDuration, int vibrationAmplitude) {
            this.enabled = enabled;
            this.soundEnabled = soundEnabled;
            this.soundVolume = soundVolume;
            this.hapticsEnabled = hapticsEnabled;
            this.vibrationDuration = vibrationDuration;
            this.vibrationAmplitude = vibrationAmplitude;
        }
    }
}
