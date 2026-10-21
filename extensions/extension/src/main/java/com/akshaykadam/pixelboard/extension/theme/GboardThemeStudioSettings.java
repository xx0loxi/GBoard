/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

public final class GboardThemeStudioSettings {
    public static final String KEY_ENABLED = "pref_theme_studio_enabled";
    public static final String KEY_BACKGROUND = "pref_theme_studio_background";
    public static final String KEY_KEYS = "pref_theme_studio_keys";
    public static final String KEY_TEXT = "pref_theme_studio_text";
    public static final String KEY_DIGITS = "pref_theme_studio_digits";

    public static final int DEFAULT_BACKGROUND = 0xFF202124;
    public static final int DEFAULT_KEYS = 0xFF3C4043;
    public static final int DEFAULT_TEXT = 0xFFE8EAED;
    public static final int DEFAULT_DIGITS = 0xFF8AB4F8;

    public static final int SLOT_BACKGROUND = 0;
    public static final int SLOT_KEYS = 1;
    public static final int SLOT_TEXT = 2;
    public static final int SLOT_DIGITS = 3;

    private GboardThemeStudioSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    public static boolean isEnabled(Context context) {
        try {
            return preferences(context).getBoolean(KEY_ENABLED, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        try {
            preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    public static int readColor(Context context, int slot) {
        try {
            return preferences(context).getInt(keyForSlot(slot), defaultForSlot(slot));
        } catch (Throwable ignored) {
            return defaultForSlot(slot);
        }
    }

    public static void writeColor(Context context, int slot, int color) {
        try {
            preferences(context).edit().putInt(keyForSlot(slot), color).apply();
        } catch (Throwable ignored) {
        }
    }

    public static int[] readPalette(Context context) {
        return new int[]{
                readColor(context, SLOT_BACKGROUND),
                readColor(context, SLOT_KEYS),
                readColor(context, SLOT_TEXT),
                readColor(context, SLOT_DIGITS)
        };
    }

    public static void reset(Context context) {
        try {
            preferences(context).edit()
                    .putInt(KEY_BACKGROUND, DEFAULT_BACKGROUND)
                    .putInt(KEY_KEYS, DEFAULT_KEYS)
                    .putInt(KEY_TEXT, DEFAULT_TEXT)
                    .putInt(KEY_DIGITS, DEFAULT_DIGITS)
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    public static String keyForSlot(int slot) {
        switch (slot) {
            case SLOT_KEYS:
                return KEY_KEYS;
            case SLOT_TEXT:
                return KEY_TEXT;
            case SLOT_DIGITS:
                return KEY_DIGITS;
            case SLOT_BACKGROUND:
            default:
                return KEY_BACKGROUND;
        }
    }

    public static int defaultForSlot(int slot) {
        switch (slot) {
            case SLOT_KEYS:
                return DEFAULT_KEYS;
            case SLOT_TEXT:
                return DEFAULT_TEXT;
            case SLOT_DIGITS:
                return DEFAULT_DIGITS;
            case SLOT_BACKGROUND:
            default:
                return DEFAULT_BACKGROUND;
        }
    }

    public static String formatColor(int color) {
        if ((color >>> 24) == 0xFF) {
            return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
        }
        return String.format(Locale.US, "#%08X", color);
    }

    public static int parseColor(String value) {
        if (!isValidColor(value)) {
            return 0;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }
        try {
            return (int) Long.parseLong(normalized, 16);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean isValidColor(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6 && normalized.length() != 8) {
            return false;
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (Character.digit(normalized.charAt(index), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
