/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.sound;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

public final class GboardSoundHapticsSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "GboardPatches";

    private static final int[] VOLUME_PRESETS = {0, 25, 50, 75, 100};
    private static final int[] DURATION_PRESETS = {0, 10, 20, 30, 40, 50, 75, 100};
    private static final int[] AMPLITUDE_PRESETS = {0, 32, 64, 96, 128, 160, 192, 224, 255};

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerSummary;
    private final String enabledTitle;
    private final String enabledSummary;
    private final String sectionOverrides;
    private final String sectionSound;
    private final String soundTitle;
    private final String soundSummary;
    private final String volumeTitle;
    private final String volumeSummary;
    private final String volumeValue;
    private final String sectionHaptics;
    private final String hapticsTitle;
    private final String hapticsSummary;
    private final String durationTitle;
    private final String durationSummary;
    private final String durationValue;
    private final String amplitudeTitle;
    private final String amplitudeSummary;
    private final String amplitudeDefault;
    private final String amplitudeValue;
    private final String sectionActions;
    private final String resetTitle;
    private final String resetSummary;

    public GboardSoundHapticsSettingsFeature(Context context) {
        entryTitle = GboardSettingsText.get(context, R.string.gboard_patches_sound_haptics_title);
        entrySummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_summary);
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        headerSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_header_summary);
        enabledTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_enabled_title);
        enabledSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_enabled_summary);
        sectionOverrides = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_section_overrides);
        sectionSound = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_section_sound);
        soundTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_sound_title);
        soundSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_sound_summary);
        volumeTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_volume_title);
        volumeSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_volume_summary);
        volumeValue = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_volume_value);
        sectionHaptics = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_section_haptics);
        hapticsTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_haptics_title);
        hapticsSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_haptics_summary);
        durationTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_duration_title);
        durationSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_duration_summary);
        durationValue = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_duration_value);
        amplitudeTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_amplitude_title);
        amplitudeSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_amplitude_summary);
        amplitudeDefault = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_amplitude_default);
        amplitudeValue = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_amplitude_value);
        sectionActions = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_section_actions);
        resetTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_reset_title);
        resetSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_sound_haptics_reset_summary);
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
                GboardPatchesFeatureAvailability.FEATURE_SOUND_HAPTICS);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            if (host == null || host.getContext() == null) {
                return buildErrorScreen();
            }
            Context context = host.getContext();
            boolean enabled = GboardSoundHapticsSettings.isEnabled(context);
            boolean soundEnabled = GboardSoundHapticsSettings.isSoundEnabled(context);
            boolean hapticsEnabled = GboardSoundHapticsSettings.isHapticsEnabled(context);
            int volume = GboardSoundHapticsSettings.getSoundVolume(context);
            int duration = GboardSoundHapticsSettings.getVibrationDuration(context);
            int amplitude = GboardSoundHapticsSettings.getVibrationAmplitude(context);

            List<GboardPatchesSettingsContract.Row> overrideRows =
                    Collections.<GboardPatchesSettingsContract.Row>singletonList(
                            new GboardPatchesSettingsContract.ToggleRow(
                                    enabledTitle,
                                    enabledSummary,
                                    true,
                                    enabled,
                                    value -> {
                                        GboardSoundHapticsSettings.setEnabled(context, value);
                                        GboardPatchesSettingsContract.refresh(host);
                                    }));

            List<GboardPatchesSettingsContract.Row> soundRows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            soundRows.add(new GboardPatchesSettingsContract.ToggleRow(
                    soundTitle,
                    soundSummary,
                    enabled,
                    soundEnabled,
                    value -> {
                        GboardSoundHapticsSettings.setSoundEnabled(context, value);
                        GboardPatchesSettingsContract.refresh(host);
                    }));
            soundRows.add(new GboardPatchesSettingsContract.SelectorRow(
                    volumeTitle,
                    volumeSummary,
                    formatVolume(context, volume),
                    enabled && soundEnabled,
                    () -> showVolumeDialog(host, volume)));

            List<GboardPatchesSettingsContract.Row> hapticRows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            hapticRows.add(new GboardPatchesSettingsContract.ToggleRow(
                    hapticsTitle,
                    hapticsSummary,
                    enabled,
                    hapticsEnabled,
                    value -> {
                        GboardSoundHapticsSettings.setHapticsEnabled(context, value);
                        GboardPatchesSettingsContract.refresh(host);
                    }));
            hapticRows.add(new GboardPatchesSettingsContract.SelectorRow(
                    durationTitle,
                    durationSummary,
                    formatDuration(context, duration),
                    enabled && hapticsEnabled,
                    () -> showDurationDialog(host, duration)));
            hapticRows.add(new GboardPatchesSettingsContract.SelectorRow(
                    amplitudeTitle,
                    amplitudeSummary,
                    formatAmplitude(context, amplitude),
                    enabled && hapticsEnabled,
                    () -> showAmplitudeDialog(host, amplitude)));

            List<GboardPatchesSettingsContract.Row> actionRows =
                    Collections.<GboardPatchesSettingsContract.Row>singletonList(
                            new GboardPatchesSettingsContract.CommandRow(
                                    resetTitle,
                                    resetSummary,
                                    enabled,
                                    () -> {
                                        GboardSoundHapticsSettings.reset(context);
                                        GboardPatchesSettingsContract.refresh(host);
                                    }));

            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    Arrays.asList(
                            new GboardPatchesSettingsContract.Section(sectionOverrides, overrideRows),
                            new GboardPatchesSettingsContract.Section(sectionSound, soundRows),
                            new GboardPatchesSettingsContract.Section(sectionHaptics, hapticRows),
                            new GboardPatchesSettingsContract.Section(sectionActions, actionRows)),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to render Sound & Haptics settings", throwable);
            return buildErrorScreen();
        }
    }

    private void showVolumeDialog(
            GboardPatchesSettingsContract.FeatureHost host, int current) {
        try {
            String[] labels = new String[VOLUME_PRESETS.length];
            String[] values = new String[VOLUME_PRESETS.length];
            for (int index = 0; index < VOLUME_PRESETS.length; index++) {
                labels[index] = formatVolume(host.getContext(), VOLUME_PRESETS[index]);
                values[index] = Integer.toString(VOLUME_PRESETS[index]);
            }
            Context context = host.getContext();
            GboardPatchesSettingsContract.showChoiceDialog(
                    host,
                    volumeTitle,
                    labels,
                    values,
                    Integer.toString(current),
                    null,
                    null,
                    value -> {
                        GboardSoundHapticsSettings.setSoundVolume(context, parse(value, current));
                        GboardPatchesSettingsContract.refresh(host);
                    });
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show sound volume dialog", throwable);
        }
    }

    private void showDurationDialog(
            GboardPatchesSettingsContract.FeatureHost host, int current) {
        try {
            String[] labels = new String[DURATION_PRESETS.length];
            String[] values = new String[DURATION_PRESETS.length];
            for (int index = 0; index < DURATION_PRESETS.length; index++) {
                labels[index] = formatDuration(host.getContext(), DURATION_PRESETS[index]);
                values[index] = Integer.toString(DURATION_PRESETS[index]);
            }
            Context context = host.getContext();
            GboardPatchesSettingsContract.showChoiceDialog(
                    host,
                    durationTitle,
                    labels,
                    values,
                    Integer.toString(current),
                    null,
                    null,
                    value -> {
                        GboardSoundHapticsSettings.setVibrationDuration(
                                context, parse(value, current));
                        GboardPatchesSettingsContract.refresh(host);
                    });
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show vibration duration dialog", throwable);
        }
    }

    private void showAmplitudeDialog(
            GboardPatchesSettingsContract.FeatureHost host, int current) {
        try {
            String[] labels = new String[AMPLITUDE_PRESETS.length];
            String[] values = new String[AMPLITUDE_PRESETS.length];
            for (int index = 0; index < AMPLITUDE_PRESETS.length; index++) {
                labels[index] = formatAmplitude(host.getContext(), AMPLITUDE_PRESETS[index]);
                values[index] = Integer.toString(AMPLITUDE_PRESETS[index]);
            }
            Context context = host.getContext();
            GboardPatchesSettingsContract.showChoiceDialog(
                    host,
                    amplitudeTitle,
                    labels,
                    values,
                    Integer.toString(current),
                    null,
                    null,
                    value -> {
                        GboardSoundHapticsSettings.setVibrationAmplitude(
                                context, parse(value, current));
                        GboardPatchesSettingsContract.refresh(host);
                    });
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show vibration strength dialog", throwable);
        }
    }

    private String formatVolume(Context context, int volume) {
        return GboardSettingsText.format(
                context, R.string.gboard_patches_sound_haptics_volume_value, volume);
    }

    private String formatDuration(Context context, int duration) {
        return GboardSettingsText.format(
                context, R.string.gboard_patches_sound_haptics_duration_value, duration);
    }

    private String formatAmplitude(Context context, int amplitude) {
        if (amplitude <= 0) {
            return amplitudeDefault;
        }
        return GboardSettingsText.format(
                context, R.string.gboard_patches_sound_haptics_amplitude_value, amplitude);
    }

    private static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return fallback;
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
