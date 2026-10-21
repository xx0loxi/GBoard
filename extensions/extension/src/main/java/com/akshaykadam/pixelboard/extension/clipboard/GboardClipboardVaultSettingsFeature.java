/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

public final class GboardClipboardVaultSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "GboardPatches";

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerSummary;
    private final String enabledTitle;
    private final String enabledSummary;
    private final String sectionStatus;
    private final String statusTitle;
    private final String statusSummary;
    private final String restoreTitle;
    private final String restoreSummary;
    private final String wipeTitle;
    private final String wipeSummary;
    private final String wipeConfirmTitle;
    private final String wipeConfirmMessage;
    private final String sectionInformation;
    private final String infoTitle;
    private final String infoSummary;

    public GboardClipboardVaultSettingsFeature(Context context) {
        entryTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_title);
        entrySummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_summary);
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        headerSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_header_summary);
        enabledTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_enabled_title);
        enabledSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_enabled_summary);
        sectionStatus = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_section_status);
        statusTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_status_title);
        statusSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_status_summary);
        restoreTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_restore_title);
        restoreSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_restore_summary);
        wipeTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_wipe_title);
        wipeSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_wipe_summary);
        wipeConfirmTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_wipe_confirm_title);
        wipeConfirmMessage = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_wipe_confirm_message);
        sectionInformation = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_section_information);
        infoTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_info_title);
        infoSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_vault_info_summary);
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
                GboardPatchesFeatureAvailability.FEATURE_CLIPBOARD_VAULT);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            if (host == null || host.getContext() == null) {
                return buildErrorScreen();
            }
            Context context = host.getContext();
            boolean enabled = GboardClipboardVault.isEnabled(context);
            int count = GboardClipboardVault.getCount(context);
            long size = GboardClipboardVault.getSizeBytes(context);

            List<GboardPatchesSettingsContract.Row> rows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            rows.add(new GboardPatchesSettingsContract.ToggleRow(
                    enabledTitle,
                    enabledSummary,
                    true,
                    enabled,
                    value -> {
                        GboardClipboardVault.setEnabled(context, value);
                        GboardPatchesSettingsContract.refresh(host);
                    }));
            rows.add(new GboardPatchesSettingsContract.DetailRow(
                    statusTitle,
                    GboardSettingsText.format(context, R.string.gboard_patches_clipboard_vault_status_summary,
                            count, formatSize(size)),
                    true));
            rows.add(new GboardPatchesSettingsContract.CommandRow(
                    restoreTitle,
                    restoreSummary,
                    enabled && count > 0,
                    () -> {
                        int restored = GboardClipboardVault.restoreMissing(context);
                        GboardPatchesSettingsContract.showMessage(host,
                                context.getString(R.string.gboard_patches_clipboard_vault_restore_title)
                                        + ": " + restored);
                        GboardPatchesSettingsContract.refresh(host);
                    }));
            rows.add(new GboardPatchesSettingsContract.DangerRow(
                    wipeTitle,
                    wipeSummary,
                    count > 0,
                    () -> {
                        GboardClipboardVault.clear(context);
                        GboardPatchesSettingsContract.refresh(host);
                    },
                    wipeConfirmTitle,
                    wipeConfirmMessage));

            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    java.util.Arrays.asList(
                            new GboardPatchesSettingsContract.Section(sectionStatus, rows),
                            new GboardPatchesSettingsContract.Section(
                                    sectionInformation,
                                    Collections.singletonList(
                                            new GboardPatchesSettingsContract.InfoRow(
                                                    infoTitle,
                                                    infoSummary,
                                                    true)))),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to render clipboard vault settings", throwable);
            return buildErrorScreen();
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

    private static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0d);
        }
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0d * 1024.0d));
    }
}
