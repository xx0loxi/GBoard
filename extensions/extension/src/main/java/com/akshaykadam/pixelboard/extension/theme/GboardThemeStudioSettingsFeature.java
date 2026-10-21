/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

public final class GboardThemeStudioSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "GboardPatches";
    private static final int[] PRESET_COLORS = new int[]{
            0xFF202124, 0xFF3C4043, 0xFF5F6368, 0xFFFFFFFF,
            0xFF000000, 0xFFF28B82, 0xFFFDD663, 0xFF81C995,
            0xFF8AB4F8, 0xFFC58AF9, 0xFFF6AEAE, 0xFF7B1FA2,
            0xFF018786, 0xFF03DAC6, 0xFFFF7043, 0xFFFFB300
    };

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerSummary;
    private final String enabledTitle;
    private final String enabledSummary;
    private final String sectionColors;
    private final String sectionActions;
    private final String backgroundTitle;
    private final String backgroundSummary;
    private final String keysTitle;
    private final String keysSummary;
    private final String textTitle;
    private final String textSummary;
    private final String digitsTitle;
    private final String digitsSummary;
    private final String resetTitle;
    private final String resetSummary;
    private final String paletteTitle;
    private final String hexHint;
    private final String invalidColor;

    public GboardThemeStudioSettingsFeature(Context context) {
        entryTitle = GboardSettingsText.get(context, R.string.gboard_patches_theme_studio_title);
        entrySummary = GboardSettingsText.get(context, R.string.gboard_patches_theme_studio_summary);
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        headerSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_header_summary);
        enabledTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_enabled_title);
        enabledSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_enabled_summary);
        sectionColors = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_section_colors);
        sectionActions = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_section_actions);
        backgroundTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_background_title);
        backgroundSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_background_summary);
        keysTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_keys_title);
        keysSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_keys_summary);
        textTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_text_title);
        textSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_text_summary);
        digitsTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_digits_title);
        digitsSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_digits_summary);
        resetTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_reset_title);
        resetSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_reset_summary);
        paletteTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_palette_title);
        hexHint = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_hex_hint);
        invalidColor = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_invalid_color);
    }

    @Override
    public String getEntryTitle() {
        return entryTitle;
    }

    @Override
    public String getEntrySummary() {
        return entrySummary;
    }

    @Override
    public boolean isAvailable(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(
                context,
                GboardPatchesFeatureAvailability.FEATURE_THEME_STUDIO);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            if (host == null || host.getContext() == null) {
                return buildErrorScreen();
            }
            Context context = host.getContext();
            boolean enabled = GboardThemeStudioSettings.isEnabled(context);
            int[] palette = GboardThemeStudioSettings.readPalette(context);

            List<GboardPatchesSettingsContract.Row> colorRows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            colorRows.add(new GboardPatchesSettingsContract.ToggleRow(
                    enabledTitle,
                    enabledSummary,
                    true,
                    enabled,
                    value -> {
                        GboardThemeStudioSettings.setEnabled(context, value);
                        GboardPatchesSettingsContract.refresh(host);
                    }));
            colorRows.add(colorRow(host, backgroundTitle, backgroundSummary, palette,
                    GboardThemeStudioSettings.SLOT_BACKGROUND));
            colorRows.add(colorRow(host, keysTitle, keysSummary, palette,
                    GboardThemeStudioSettings.SLOT_KEYS));
            colorRows.add(colorRow(host, textTitle, textSummary, palette,
                    GboardThemeStudioSettings.SLOT_TEXT));
            colorRows.add(colorRow(host, digitsTitle, digitsSummary, palette,
                    GboardThemeStudioSettings.SLOT_DIGITS));

            List<GboardPatchesSettingsContract.Row> actionRows =
                    Collections.<GboardPatchesSettingsContract.Row>singletonList(
                            new GboardPatchesSettingsContract.CommandRow(
                                    resetTitle,
                                    resetSummary,
                                    true,
                                    () -> {
                                        GboardThemeStudioSettings.reset(context);
                                        GboardPatchesSettingsContract.refresh(host);
                                    }));

            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    Arrays.asList(
                            new GboardPatchesSettingsContract.Section(sectionColors, colorRows),
                            new GboardPatchesSettingsContract.Section(sectionActions, actionRows)),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to render Theme Studio settings", throwable);
            return buildErrorScreen();
        }
    }

    private GboardPatchesSettingsContract.SelectorRow colorRow(
            GboardPatchesSettingsContract.FeatureHost host,
            String title,
            String summary,
            int[] palette,
            int slot) {
        return new GboardPatchesSettingsContract.SelectorRow(
                title,
                summary,
                GboardThemeStudioSettings.formatColor(palette[slot]),
                true,
                () -> showColorDialog(host, slot));
    }

    private void showColorDialog(GboardPatchesSettingsContract.FeatureHost host, int slot) {
        try {
            Context context = host.getContext();
            int[] palette = GboardThemeStudioSettings.readPalette(context);
            final int[] selected = new int[]{palette[slot]};

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(context, 20);
            container.setPadding(padding, padding, padding, 0);

            GboardThemePreviewView preview = new GboardThemePreviewView(context);
            preview.setPalette(palette);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 150));
            previewParams.bottomMargin = dp(context, 12);
            container.addView(preview, previewParams);

            EditText hex = new EditText(context);
            hex.setSingleLine(true);
            hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            hex.setHint(hexHint);
            hex.setText(GboardThemeStudioSettings.formatColor(selected[0]));
            container.addView(hex, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout swatches = new LinearLayout(context);
            swatches.setOrientation(LinearLayout.HORIZONTAL);
            swatches.setPadding(0, dp(context, 8), 0, 0);
            for (int color : PRESET_COLORS) {
                View swatch = new View(context);
                LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(
                        dp(context, 30), dp(context, 30));
                swatchParams.rightMargin = dp(context, 6);
                swatch.setBackground(swatchDrawable(context, color, color == selected[0]));
                swatch.setOnClickListener(ignored -> {
                    selected[0] = color;
                    hex.setText(GboardThemeStudioSettings.formatColor(color));
                    updatePreview(preview, palette, slot, selected[0]);
                });
                swatches.addView(swatch, swatchParams);
            }
            container.addView(swatches);

            ScrollView scroll = new ScrollView(context);
            scroll.addView(container);

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(paletteTitle)
                    .setView(scroll)
                    .setNegativeButton(R.string.gboard_patches_dialog_cancel, null)
                    .setPositiveButton(R.string.gboard_patches_dialog_save, null)
                    .create();
            hex.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String value = editable == null ? "" : editable.toString();
                    if (GboardThemeStudioSettings.isValidColor(value)) {
                        selected[0] = GboardThemeStudioSettings.parseColor(value);
                        updatePreview(preview, palette, slot, selected[0]);
                    }
                }
            });
            dialog.setOnShowListener(ignored -> dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE).setOnClickListener(clicked -> {
                        String value = hex.getText() == null ? "" : hex.getText().toString();
                        if (!GboardThemeStudioSettings.isValidColor(value)) {
                            hex.setError(invalidColor);
                            return;
                        }
                        GboardThemeStudioSettings.writeColor(
                                context,
                                slot,
                                GboardThemeStudioSettings.parseColor(value));
                        GboardPatchesSettingsContract.refresh(host);
                        dialog.dismiss();
                    }));

            GboardPatchesSettingsContract.showManagedDialog(host, onDismiss -> {
                dialog.setOnDismissListener(ignored -> onDismiss.run());
                dialog.show();
                return true;
            });
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show color picker", throwable);
            Toast.makeText(host.getContext(), invalidColor, Toast.LENGTH_SHORT).show();
        }
    }

    private static void updatePreview(GboardThemePreviewView preview, int[] palette, int slot,
            int color) {
        int[] updated = new int[]{palette[0], palette[1], palette[2], palette[3]};
        updated[slot] = color;
        preview.setPalette(updated);
    }

    private static GradientDrawable swatchDrawable(Context context, int color, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, 6));
        drawable.setStroke(selected ? dp(context, 3) : dp(context, 1),
                selected ? 0xFFFFFFFF : 0x66000000);
        return drawable;
    }

    private GboardPatchesSettingsContract.Screen buildErrorScreen() {
        return new GboardPatchesSettingsContract.Screen(
                entryTitle,
                headerBadge,
                entryTitle,
                headerSummary,
                Collections.singletonList(new GboardPatchesSettingsContract.StatusBlock(
                        entryTitle,
                        entrySummary,
                        GboardPatchesSettingsContract.StatusTone.WARNING)),
                Collections.emptyList());
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
