/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Recolors the stable SoftKeyboardView tree after Gboard applies its own theme.
 */
public final class GboardThemeStudioRuntime {
    private static final String TAG = "GboardThemeStudio";
    private static final String SOFT_KEY_VIEW =
            "com.google.android.libraries.inputmethod.widgets.SoftKeyView";
    private static final Map<View, SavedColors> SAVED =
            new WeakHashMap<View, SavedColors>();

    private GboardThemeStudioRuntime() {
    }

    public static void applyKeyboardColors(View keyboard) {
        if (keyboard == null) {
            return;
        }
        try {
            Context context = keyboard.getContext();
            if (context == null) {
                return;
            }
            int[] palette = GboardThemeStudioSettings.readPalette(context);
            if (!GboardThemeStudioSettings.isEnabled(context)) {
                restoreTree(keyboard);
                return;
            }

            synchronized (SAVED) {
                applyRootBackground(keyboard, palette[GboardThemeStudioSettings.SLOT_BACKGROUND]);
                applyChildren(keyboard, palette, false);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to apply keyboard palette", throwable);
        }
    }

    private static void applyRootBackground(View root, int color) {
        SavedColors saved = savedFor(root);
        if (!saved.backgroundCaptured) {
            saved.originalBackground = root.getBackground();
            saved.backgroundCaptured = true;
        }
        if (saved.appliedBackground != color) {
            root.setBackgroundColor(color);
            saved.appliedBackground = color;
        }
    }

    private static void applyChildren(View view, int[] palette, boolean insideKey) {
        if (view == null) {
            return;
        }
        boolean key = isSoftKeyView(view);
        if (key) {
            applyKeyBackground(view, palette[GboardThemeStudioSettings.SLOT_KEYS]);
        }

        if (view instanceof TextView) {
            if (insideKey || key) {
                int target = isDigitLabel(((TextView) view).getText())
                        ? palette[GboardThemeStudioSettings.SLOT_DIGITS]
                        : palette[GboardThemeStudioSettings.SLOT_TEXT];
                applyTextColor((TextView) view, target);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyChildren(group.getChildAt(index), palette, insideKey || key);
            }
        }
    }

    private static void applyKeyBackground(View view, int color) {
        SavedColors saved = savedFor(view);
        if (!saved.backgroundCaptured) {
            saved.originalBackground = view.getBackground();
            saved.originalBackgroundTint = view.getBackgroundTintList();
            saved.backgroundCaptured = true;
        }
        if (saved.appliedBackground == color) {
            return;
        }
        if (view.getBackground() == null) {
            view.setBackgroundColor(color);
        } else {
            view.setBackgroundTintList(keyColorStates(color));
        }
        saved.appliedBackground = color;
    }

    private static void applyTextColor(TextView view, int color) {
        SavedColors saved = savedFor(view);
        if (!saved.textCaptured) {
            saved.originalTextColors = view.getTextColors();
            saved.textCaptured = true;
        }
        if (saved.appliedText != color) {
            view.setTextColor(color);
            saved.appliedText = color;
        }
    }

    private static void restoreTree(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                restoreTree(group.getChildAt(index));
            }
        }
        synchronized (SAVED) {
            SavedColors saved = SAVED.remove(view);
            if (saved != null) {
                restoreView(view, saved);
            }
        }
    }

    private static void restoreView(View view, SavedColors saved) {
        try {
            if (saved.backgroundCaptured) {
                view.setBackground(saved.originalBackground);
                view.setBackgroundTintList(saved.originalBackgroundTint);
            }
            if (saved.textCaptured && view instanceof TextView) {
                ((TextView) view).setTextColor(saved.originalTextColors);
            }
        } catch (Throwable ignored) {
        }
    }

    private static SavedColors savedFor(View view) {
        SavedColors saved = SAVED.get(view);
        if (saved == null) {
            saved = new SavedColors();
            SAVED.put(view, saved);
        }
        return saved;
    }

    private static boolean isSoftKeyView(View view) {
        Class<?> type = view.getClass();
        while (type != null) {
            if (SOFT_KEY_VIEW.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isDigitLabel(CharSequence label) {
        if (label == null) {
            return false;
        }
        String value = label.toString().trim();
        if (value.length() != 1) {
            return false;
        }
        return Character.isDigit(value.charAt(0));
    }

    private static ColorStateList keyColorStates(int color) {
        int pressed = darken(color);
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_focused},
                        new int[]{}
                },
                new int[]{pressed, pressed, color});
    }

    private static int darken(int color) {
        int alpha = color >>> 24;
        int red = (int) (((color >> 16) & 0xFF) * 0.82f);
        int green = (int) (((color >> 8) & 0xFF) * 0.82f);
        int blue = (int) ((color & 0xFF) * 0.82f);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static final class SavedColors {
        boolean backgroundCaptured;
        Drawable originalBackground;
        ColorStateList originalBackgroundTint;
        int appliedBackground = Integer.MIN_VALUE;

        boolean textCaptured;
        ColorStateList originalTextColors;
        int appliedText = Integer.MIN_VALUE;
    }
}
