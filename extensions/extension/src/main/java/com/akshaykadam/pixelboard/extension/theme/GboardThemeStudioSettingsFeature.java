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
import android.util.Log;
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
                        GboardThemeStudioRuntime.refreshKeyboards();
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
                                        GboardThemeStudioRuntime.refreshKeyboards();
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
            AlertDialog dialog = GboardThemeColorDialog.create(context, slot, false, () ->
                    GboardPatchesSettingsContract.refresh(host));
            if (dialog == null) {
                Toast.makeText(context, invalidColor, Toast.LENGTH_SHORT).show();
                return;
            }
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
}
