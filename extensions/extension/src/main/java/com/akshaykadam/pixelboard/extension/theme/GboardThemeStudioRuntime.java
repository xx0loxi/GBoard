/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

/**
 * Recolors the stable SoftKeyboardView tree after Gboard applies its own theme and injects the
 * Theme Studio controls into Gboard's native Theme section.
 */
public final class GboardThemeStudioRuntime {
    private static final String TAG = "GboardThemeStudio";
    private static final String SOFT_KEY_VIEW =
            "com.google.android.libraries.inputmethod.widgets.SoftKeyView";
    private static final String SETTINGS_ACTIVITY =
            "com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsActivity";
    private static final String EXTRA_NAVIGATION_PATH =
            "com.akshaykadam.pixelboard.extension.extra.PATCHES_NAVIGATION_PATH";
    private static final Map<View, SavedColors> SAVED =
            new WeakHashMap<View, SavedColors>();
    private static final Map<View, Boolean> KEYBOARDS =
            new WeakHashMap<View, Boolean>();
    private static final int HEADER_TAG = View.generateViewId();

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
            if (context == null || !GboardThemeStudioSettings.isEnabled(context)) {
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

    /**
     * Wraps the theme listing in a vertical container with the palette controls above the native
     * theme grid. Returning the wrapper keeps every native theme visible and selectable.
     */
    public static View installThemeSectionHeader(View root) {
        if (root == null) {
            return null;
        }
        try {
            Object existing = root.getTag(HEADER_TAG);
            if (existing instanceof ThemeSectionHeader) {
                ((ThemeSectionHeader) existing).refresh();
                return root;
            }
            Context context = root.getContext();
            if (context == null) {
                return root;
            }

            ThemeSectionHeader header = new ThemeSectionHeader(context);
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            headerParams.setMargins(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 4));
            wrapper.addView(header, headerParams);

            ViewGroup parent = root.getParent() instanceof ViewGroup
                    ? (ViewGroup) root.getParent()
                    : null;
            if (parent != null) {
                int index = parent.indexOfChild(root);
                ViewGroup.LayoutParams params = root.getLayoutParams();
                parent.removeViewAt(index);
                wrapper.setTag(HEADER_TAG, header);
                if (params != null) {
                    parent.addView(wrapper, index, params);
                } else {
                    parent.addView(wrapper, index);
                }
            }
            wrapper.addView(root, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            wrapper.setTag(HEADER_TAG, header);
            return wrapper;
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to install Theme section header", throwable);
            return root;
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

    private static GradientDrawable swatchDrawable(Context context, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, 5));
        drawable.setStroke(dp(context, 1), luminance(color) > 0.55f ? 0x55000000 : 0x55FFFFFF);
        return drawable;
    }

    private static int resolveThemeColor(Context context, int attr, int fallback) {
        try {
            TypedValue value = new TypedValue();
            if (context.getTheme().resolveAttribute(attr, value, true)) {
                if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                        && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return value.data;
                }
                if (value.resourceId != 0) {
                    try {
                        return context.getResources()
                                .getColorStateList(value.resourceId)
                                .getDefaultColor();
                    } catch (Throwable ignored) {
                        return context.getResources().getColor(value.resourceId);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static int titleResForSlot(int slot) {
        switch (slot) {
            case GboardThemeStudioSettings.SLOT_KEYS:
                return R.string.gboard_patches_theme_studio_keys_title;
            case GboardThemeStudioSettings.SLOT_TEXT:
                return R.string.gboard_patches_theme_studio_text_title;
            case GboardThemeStudioSettings.SLOT_DIGITS:
                return R.string.gboard_patches_theme_studio_digits_title;
            case GboardThemeStudioSettings.SLOT_BACKGROUND:
            default:
                return R.string.gboard_patches_theme_studio_background_title;
        }
    }

    private static void openFullSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, SETTINGS_ACTIVITY);
            ArrayList<String> path = new ArrayList<String>();
            path.add(GboardThemeStudioSettingsFeature.class.getName());
            intent.putStringArrayListExtra(EXTRA_NAVIGATION_PATH, path);
            if (!isActivityContext(context)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to open Theme Studio settings", throwable);
        }
    }

    private static boolean isActivityContext(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return true;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof Activity;
    }

    private static void applySelectableBackground(Context context, View view) {
        try {
            TypedValue value = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, value, true)
                    && value.resourceId != 0) {
                view.setBackgroundResource(value.resourceId);
            }
        } catch (Throwable ignored) {
        }
    }

    private static int dp(Context context, int value) {
        if (context == null) {
            return value;
        }
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class ThemeSectionHeader extends LinearLayout {
        private final Switch toggle;
        private final TextView[] values = new TextView[4];
        private final View[] swatches = new View[4];
        private final LinearLayout[] rows = new LinearLayout[4];
        private final GboardThemePreviewView preview;

        ThemeSectionHeader(Context context) {
            super(context);
            setOrientation(VERTICAL);
            int cardPadding = dp(context, 12);
            setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
            GradientDrawable background = new GradientDrawable();
            background.setColor(resolveThemeColor(
                    context, android.R.attr.colorBackground, 0xFFFFFFFF));
            background.setCornerRadius(dp(context, 16));
            background.setStroke(dp(context, 1), 0x22000000);
            setBackground(background);

            LinearLayout titleRow = new LinearLayout(context);
            titleRow.setOrientation(HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(context);
            title.setText(GboardSettingsText.get(
                    context, R.string.gboard_patches_theme_studio_title));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(resolveThemeColor(
                    context, android.R.attr.textColorPrimary, 0xFF202124));
            titleRow.addView(title, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            toggle = new Switch(context);
            titleRow.addView(toggle, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(titleRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView summary = new TextView(context);
            summary.setText(GboardSettingsText.get(
                    context, R.string.gboard_patches_theme_studio_header_summary));
            summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            summary.setTextColor(resolveThemeColor(
                    context, android.R.attr.textColorSecondary, 0xFF5F6368));
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryParams.topMargin = dp(context, 2);
            summaryParams.bottomMargin = dp(context, 8);
            addView(summary, summaryParams);

            preview = new GboardThemePreviewView(context);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 80));
            previewParams.bottomMargin = dp(context, 4);
            addView(preview, previewParams);
            if (context.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                preview.setVisibility(GONE);
            }

            for (int slot = 0; slot < 4; slot++) {
                rows[slot] = createColorRow(context, slot);
                addView(rows[slot], new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            TextView reset = createAction(context,
                    R.string.gboard_patches_theme_studio_reset_title);
            reset.setOnClickListener(ignored -> resetPalette());
            TextView more = createAction(context,
                    R.string.gboard_patches_theme_studio_more_options);
            more.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            more.setOnClickListener(ignored -> openFullSettings(getContext()));
            actions.addView(reset, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(more, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            actionParams.topMargin = dp(context, 4);
            addView(actions, actionParams);

            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Context current = getContext();
                if (current == null) {
                    return;
                }
                GboardThemeStudioSettings.setEnabled(current, isChecked);
                applyEnabledState(isChecked);
                refreshKeyboards();
            });
            refresh();
        }

        private LinearLayout createColorRow(Context context, final int slot) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(context, 4), 0, dp(context, 4), 0);
            row.setMinimumHeight(dp(context, 36));
            row.setClickable(true);
            row.setFocusable(true);
            applySelectableBackground(context, row);

            TextView label = new TextView(context);
            label.setText(GboardSettingsText.get(context, titleResForSlot(slot)));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            label.setTextColor(resolveThemeColor(
                    context, android.R.attr.textColorPrimary, 0xFF202124));
            row.addView(label, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView value = new TextView(context);
            value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            value.setTextColor(resolveThemeColor(
                    context, android.R.attr.textColorSecondary, 0xFF5F6368));
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            valueParams.rightMargin = dp(context, 10);
            row.addView(value, valueParams);
            values[slot] = value;

            View swatch = new View(context);
            row.addView(swatch, new LinearLayout.LayoutParams(
                    dp(context, 22), dp(context, 22)));
            swatches[slot] = swatch;

            row.setOnClickListener(ignored -> {
                AlertDialog dialog = GboardThemeColorDialog.create(
                        getContext(), slot, true, this::refresh);
                if (dialog != null) {
                    dialog.show();
                }
            });
            return row;
        }

        private TextView createAction(Context context, int titleResourceId) {
            TextView action = new TextView(context);
            action.setText(GboardSettingsText.get(context, titleResourceId));
            action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            action.setTextColor(resolveThemeColor(
                    context, android.R.attr.colorAccent, 0xFF1A73E8));
            action.setGravity(Gravity.CENTER_VERTICAL);
            action.setPadding(dp(context, 4), dp(context, 6), dp(context, 4), dp(context, 6));
            action.setClickable(true);
            action.setFocusable(true);
            applySelectableBackground(context, action);
            return action;
        }

        void refresh() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            int[] palette = GboardThemeStudioSettings.readPalette(context);
            boolean enabled = GboardThemeStudioSettings.isEnabled(context);
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(enabled);
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Context current = getContext();
                if (current == null) {
                    return;
                }
                GboardThemeStudioSettings.setEnabled(current, isChecked);
                applyEnabledState(isChecked);
                refreshKeyboards();
            });
            for (int slot = 0; slot < 4; slot++) {
                values[slot].setText(GboardThemeStudioSettings.formatColor(palette[slot]));
                swatches[slot].setBackground(swatchDrawable(context, palette[slot]));
            }
            preview.setPalette(palette);
            applyEnabledState(enabled);
        }

        private void applyEnabledState(boolean enabled) {
            float alpha = enabled ? 1f : 0.55f;
            for (LinearLayout row : rows) {
                if (row != null) {
                    row.setAlpha(alpha);
                }
            }
            preview.setAlpha(alpha);
        }

        private void resetPalette() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            GboardThemeStudioSettings.reset(context);
            refresh();
            refreshKeyboards();
            Toast.makeText(
                    context,
                    GboardSettingsText.get(
                            context, R.string.gboard_patches_theme_studio_reset_done),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            refresh();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == VISIBLE) {
                refresh();
            }
        }
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
