package com.akshaykadam.pixelboard.extension.sound;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

public final class GboardSoundHapticsSettings {
    private static final String TAG = "GboardSoundHaptics";
    private static final String PREF_ENABLED = "pref_sound_haptics_enabled";
    private static final String PREF_SOUND_ENABLED = "pref_sound_haptics_sound_enabled";
    private static final String PREF_SOUND_VOLUME = "pref_sound_haptics_sound_volume";
    private static final String PREF_HAPTICS_ENABLED = "pref_sound_haptics_haptics_enabled";
    private static final String PREF_VIBRATION_DURATION = "pref_sound_haptics_vibration_duration";
    private static final String PREF_VIBRATION_AMPLITUDE = "pref_sound_haptics_vibration_amplitude";

    public static final int DEFAULT_SOUND_VOLUME = 100;
    public static final int DEFAULT_VIBRATION_DURATION = 30;
    public static final int DEFAULT_VIBRATION_AMPLITUDE = 0;

    private GboardSoundHapticsSettings() {
    }

    public static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(PREF_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        write(context, PREF_ENABLED, enabled);
    }

    public static boolean isSoundEnabled(Context context) {
        return preferences(context).getBoolean(PREF_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        write(context, PREF_SOUND_ENABLED, enabled);
    }

    public static int getSoundVolume(Context context) {
        return clamp(preferences(context).getInt(PREF_SOUND_VOLUME, DEFAULT_SOUND_VOLUME), 0, 100);
    }

    public static void setSoundVolume(Context context, int volume) {
        write(context, PREF_SOUND_VOLUME, clamp(volume, 0, 100));
    }

    public static boolean isHapticsEnabled(Context context) {
        return preferences(context).getBoolean(PREF_HAPTICS_ENABLED, true);
    }

    public static void setHapticsEnabled(Context context, boolean enabled) {
        write(context, PREF_HAPTICS_ENABLED, enabled);
    }

    public static int getVibrationDuration(Context context) {
        return clamp(preferences(context).getInt(
                PREF_VIBRATION_DURATION, DEFAULT_VIBRATION_DURATION), 0, 100);
    }

    public static void setVibrationDuration(Context context, int duration) {
        write(context, PREF_VIBRATION_DURATION, clamp(duration, 0, 100));
    }

    public static int getVibrationAmplitude(Context context) {
        return clamp(preferences(context).getInt(
                PREF_VIBRATION_AMPLITUDE, DEFAULT_VIBRATION_AMPLITUDE), 0, 255);
    }

    public static void setVibrationAmplitude(Context context, int amplitude) {
        write(context, PREF_VIBRATION_AMPLITUDE, clamp(amplitude, 0, 255));
    }

    public static void reset(Context context) {
        try {
            preferences(context).edit()
                    .remove(PREF_ENABLED)
                    .remove(PREF_SOUND_ENABLED)
                    .remove(PREF_SOUND_VOLUME)
                    .remove(PREF_HAPTICS_ENABLED)
                    .remove(PREF_VIBRATION_DURATION)
                    .remove(PREF_VIBRATION_AMPLITUDE)
                    .apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to reset sound and haptics settings", throwable);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    private static void write(Context context, String key, boolean value) {
        try {
            preferences(context).edit().putBoolean(key, value).apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to write " + key, throwable);
        }
    }

    private static void write(Context context, String key, int value) {
        try {
            preferences(context).edit().putInt(key, value).apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to write " + key, throwable);
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
