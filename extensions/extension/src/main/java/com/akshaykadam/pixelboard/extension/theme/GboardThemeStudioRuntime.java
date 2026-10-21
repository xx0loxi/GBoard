/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Recolors the stable SoftKeyboardView tree after Gboard applies its own theme. The palette is
 * configured from the PixelBoard patches settings screen; no Theme-section UI is modified.
 */
public final class GboardThemeStudioRuntime {
    private static final String TAG = "GboardThemeStudio";
    private static final String SOFT_KEY_VIEW =
            "com.google.android.libraries.inputmethod.widgets.SoftKeyView";
    private static final Map<View, SavedColors> SAVED =
            new WeakHashMap<View, SavedColors>();
    private static final Map<View, Boolean> KEYBOARDS =
            new WeakHashMap<View, Boolean>();

    private GboardThemeStudioRuntime() {
    }

    public static void applyKeyboardColors(View keyboard) {
        if (keyboard == null) {
            return;
        }
        try {
            Context context = keyboard.getContext();
            if (context == null || isActivityContext(context)) {
                // Theme previews inside Gboard Settings are hosted by an Activity; recoloring
                // them fights Gboard's own preview engine, so only the real IME is touched.
                return;
            }
            synchronized (KEYBOARDS) {
                KEYBOARDS.put(keyboard, Boolean.TRUE);
            }
            if (!GboardThemeStudioSettings.isEnabled(context)) {
                restoreTree(keyboard);
                return;
            }

            int[] palette = GboardThemeStudioSettings.readPalette(context);
            synchronized (SAVED) {
                applyRootBackground(keyboard, palette[GboardThemeStudioSettings.SLOT_BACKGROUND]);
                applyChildren(keyboard, palette, false);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to apply keyboard palette", throwable);
        }
    }

    /**
     * Re-asserts the key fill and label colors right before the key is drawn. Gboard may re-tint
     * key backgrounds while applying its own theme, so this keeps the custom palette on top.
     */
    public static void applyKeyColors(View key) {
        if (key == null) {
            return;
        }
        try {
            Context context = key.getContext();
            if (context == null || isActivityContext(context)
                    || !GboardThemeStudioSettings.isEnabled(context)) {
                return;
            }
            int[] palette = GboardThemeStudioSettings.readPalette(context);
            synchronized (SAVED) {
                applyKeyBackground(key, palette[GboardThemeStudioSettings.SLOT_KEYS]);
                applyKeyText(key, palette);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to apply key palette", throwable);
        }
    }

    public static void refreshKeyboards() {
        List<View> keyboards;
        synchronized (KEYBOARDS) {
            if (KEYBOARDS.isEmpty()) {
                return;
            }
            keyboards = new ArrayList<View>(KEYBOARDS.keySet());
        }
        for (View keyboard : keyboards) {
            applyKeyboardColors(keyboard);
        }
    }

    private static void applyRootBackground(View root, int color) {
        SavedColors saved = savedFor(root);
        if (!saved.backgroundCaptured) {
            saved.originalBackground = root.getBackground();
            saved.originalBackgroundTint = root.getBackgroundTintList();
            saved.backgroundCaptured = true;
        }
        if (saved.appliedBackground == color
                && root.getBackground() == saved.appliedDrawable
                && root.getBackgroundTintList() == null) {
            return;
        }
        root.setBackgroundColor(color);
        root.setBackgroundTintList(null);
        saved.appliedDrawable = root.getBackground();
        saved.appliedBackground = color;
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

    private static void applyKeyText(View view, int[] palette) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            int target = isDigitLabel(((TextView) view).getText())
                    ? palette[GboardThemeStudioSettings.SLOT_DIGITS]
                    : palette[GboardThemeStudioSettings.SLOT_TEXT];
            applyTextColor((TextView) view, target);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyKeyText(group.getChildAt(index), palette);
            }
        }
    }

    private static void applyKeyBackground(View view, int color) {
        Context context = view.getContext();
        if (context == null) {
            return;
        }
        SavedColors saved = savedFor(view);
        if (!saved.backgroundCaptured) {
            saved.originalBackground = view.getBackground();
            saved.originalBackgroundTint = view.getBackgroundTintList();
            saved.cornerRadius = resolveCornerRadius(view, saved.originalBackground);
            saved.backgroundCaptured = true;
        }
        if (saved.appliedBackground == color
                && view.getBackground() == saved.appliedDrawable
                && view.getBackgroundTintList() == null) {
            return;
        }
        Drawable background = keyBackgroundDrawable(context, color, saved.cornerRadius);
        view.setBackground(background);
        view.setBackgroundTintList(null);
        saved.appliedDrawable = view.getBackground();
        saved.appliedBackground = color;
    }

    private static void applyTextColor(TextView view, int color) {
        SavedColors saved = savedFor(view);
        if (!saved.textCaptured) {
            saved.originalTextColors = view.getTextColors();
            saved.textCaptured = true;
        }
        if (saved.appliedText == color && view.getCurrentTextColor() == color) {
            return;
        }
        view.setTextColor(color);
        saved.appliedText = color;
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

    private static Drawable keyBackgroundDrawable(Context context, int color, float radius) {
        GradientDrawable content = new GradientDrawable();
        content.setColor(color);
        content.setCornerRadius(radius);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(radius);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor(color)), content, mask);
    }

    private static float resolveCornerRadius(View view, Drawable background) {
        if (background != null) {
            try {
                Outline outline = new Outline();
                background.getOutline(outline);
                if (outline.getRadius() > 0f) {
                    return outline.getRadius();
                }
            } catch (Throwable ignored) {
            }
        }
        return dp(view.getContext(), 6);
    }

    private static int rippleColor(int color) {
        int alpha = 0x33;
        return luminance(color) > 0.55f
                ? (alpha << 24)
                : (alpha << 24) | 0xFFFFFF;
    }

    private static float luminance(int color) {
        float red = ((color >> 16) & 0xFF) / 255f;
        float green = ((color >> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        return 0.2126f * red + 0.7152f * green + 0.0722f * blue;
    }

    private static boolean isActivityContext(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof android.app.Activity) {
                return true;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof android.app.Activity;
    }

    private static int dp(Context context, int value) {
        if (context == null) {
            return value;
        }
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class SavedColors {
        boolean backgroundCaptured;
        Drawable originalBackground;
        ColorStateList originalBackgroundTint;
        Drawable appliedDrawable;
        int appliedBackground = Integer.MIN_VALUE;
        float cornerRadius;

        boolean textCaptured;
        ColorStateList originalTextColors;
        int appliedText = Integer.MIN_VALUE;
    }
}
