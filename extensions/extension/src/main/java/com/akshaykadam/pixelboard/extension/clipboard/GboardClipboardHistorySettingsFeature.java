/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettingsContract;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

public final class GboardClipboardHistorySettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private static final String TAG = "GboardPatches";
    private static final int MAX_VISIBLE_ENTRIES = 50;
    private static final int TITLE_LENGTH = 50;
    private static final int SUMMARY_LENGTH = 140;

    private final String entryTitle;
    private final String entrySummary;
    private final String headerBadge;
    private final String headerSummary;
    private final String sectionEntries;
    private final String searchTitle;
    private final String searchSummary;
    private final String searchHint;
    private final String searchEmpty;
    private final String statusTitle;
    private final String statusSummary;
    private final String statusLimited;
    private final String emptyTitle;
    private final String emptySummary;
    private final String copyAction;
    private final String deleteAction;
    private final String copied;
    private final String deleted;
    private final String deleteFailed;
    private final String clearTitle;
    private final String clearSummary;
    private final String clearConfirmTitle;
    private final String clearConfirmMessage;

    private String searchQuery = "";

    public GboardClipboardHistorySettingsFeature(Context context) {
        entryTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_title);
        entrySummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_summary);
        headerBadge = GboardSettingsText.get(context, R.string.gboard_patches_header_badge);
        headerSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_header_summary);
        sectionEntries = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_section_entries);
        searchTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_search_title);
        searchSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_search_summary);
        searchHint = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_search_hint);
        searchEmpty = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_search_empty);
        statusTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_status_title);
        statusSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_status_summary);
        statusLimited = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_status_limited);
        emptyTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_empty_title);
        emptySummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_empty_summary);
        copyAction = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_copy_action);
        deleteAction = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_delete_action);
        copied = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_copied);
        deleted = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_deleted);
        deleteFailed = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_delete_failed);
        clearTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_clear_title);
        clearSummary = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_clear_summary);
        clearConfirmTitle = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_clear_confirm_title);
        clearConfirmMessage = GboardSettingsText.get(context,
                R.string.gboard_patches_clipboard_history_clear_confirm_message);
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
                GboardPatchesFeatureAvailability.FEATURE_CLIPBOARD_HISTORY);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        try {
            if (host == null || host.getContext() == null) {
                return buildErrorScreen();
            }
            Context context = host.getContext();
            List<GboardClipboardVault.Entry> all =
                    GboardClipboardVault.readEntries(context);
            String query = searchQuery == null ? "" : searchQuery.trim();
            List<GboardClipboardVault.Entry> matches = filter(all, query);
            Collections.reverse(matches);

            List<GboardPatchesSettingsContract.Row> rows =
                    new ArrayList<GboardPatchesSettingsContract.Row>();
            rows.add(new GboardPatchesSettingsContract.SelectorRow(
                    searchTitle,
                    searchSummary,
                    query.isEmpty() ? null : query,
                    true,
                    () -> showSearchDialog(host)));
            String status = matches.size() > MAX_VISIBLE_ENTRIES
                    ? GboardSettingsText.format(context,
                            R.string.gboard_patches_clipboard_history_status_limited,
                            MAX_VISIBLE_ENTRIES, matches.size())
                    : GboardSettingsText.format(context,
                            R.string.gboard_patches_clipboard_history_status_summary,
                            matches.size());
            rows.add(new GboardPatchesSettingsContract.DetailRow(
                    statusTitle, status, true));

            if (matches.isEmpty()) {
                rows.add(new GboardPatchesSettingsContract.InfoRow(
                        query.isEmpty() ? emptyTitle : searchTitle,
                        query.isEmpty() ? emptySummary : searchEmpty,
                        true));
            } else {
                int visible = Math.min(matches.size(), MAX_VISIBLE_ENTRIES);
                for (int index = 0; index < visible; index++) {
                    GboardClipboardVault.Entry entry = matches.get(index);
                    rows.add(new GboardPatchesSettingsContract.NavigationRow(
                            snippet(entry.text, TITLE_LENGTH),
                            entrySummaryText(context, entry),
                            true,
                            () -> showEntryActions(host, entry)));
                }
            }
            rows.add(new GboardPatchesSettingsContract.DangerRow(
                    clearTitle,
                    clearSummary,
                    !all.isEmpty(),
                    () -> {
                        GboardClipboardVault.clear(context);
                        GboardPatchesSettingsContract.refresh(host);
                    },
                    clearConfirmTitle,
                    clearConfirmMessage));

            return new GboardPatchesSettingsContract.Screen(
                    entryTitle,
                    headerBadge,
                    entryTitle,
                    headerSummary,
                    Collections.emptyList(),
                    Collections.singletonList(
                            new GboardPatchesSettingsContract.Section(
                                    sectionEntries, rows)),
                    GboardPatchesSettingsContract.RefreshPolicy.none(),
                    GboardPatchesSettingsContract.PanelStyle.FLAT);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to render clipboard history settings", throwable);
            return buildErrorScreen();
        }
    }

    private List<GboardClipboardVault.Entry> filter(
            List<GboardClipboardVault.Entry> entries, String query) {
        if (query.isEmpty()) {
            return new ArrayList<GboardClipboardVault.Entry>(entries);
        }
        String needle = query.toLowerCase(Locale.getDefault());
        List<GboardClipboardVault.Entry> matches =
                new ArrayList<GboardClipboardVault.Entry>();
        for (GboardClipboardVault.Entry entry : entries) {
            if (entry.text.toLowerCase(Locale.getDefault()).contains(needle)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private void showSearchDialog(final GboardPatchesSettingsContract.FeatureHost host) {
        try {
            GboardPatchesSettingsContract.showTextInputDialog(
                    host,
                    searchTitle,
                    searchHint,
                    searchQuery == null ? "" : searchQuery,
                    value -> searchQuery = value == null ? "" : value.trim());
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show clipboard search dialog", throwable);
        }
    }

    private void showEntryActions(final GboardPatchesSettingsContract.FeatureHost host,
            final GboardClipboardVault.Entry entry) {
        try {
            final Context context = host.getContext();
            GboardPatchesSettingsContract.showChoiceDialog(
                    host,
                    snippet(entry.text, TITLE_LENGTH),
                    new String[]{copyAction, deleteAction},
                    new String[]{"copy", "delete"},
                    null,
                    null,
                    null,
                    value -> {
                        if ("copy".equals(value)) {
                            if (copyToClipboard(context, entry.text)) {
                                GboardPatchesSettingsContract.showMessage(host, copied);
                            }
                            return;
                        }
                        if ("delete".equals(value)) {
                            boolean removed = GboardClipboardVault.deleteEntry(context, entry);
                            GboardPatchesSettingsContract.showMessage(
                                    host, removed ? deleted : deleteFailed);
                            GboardPatchesSettingsContract.refresh(host);
                        }
                    });
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to show clipboard entry actions", throwable);
        }
    }

    private static boolean copyToClipboard(Context context, String text) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(
                    Context.CLIPBOARD_SERVICE);
            if (manager == null) {
                return false;
            }
            manager.setPrimaryClip(ClipData.newPlainText("PixelBoard clipboard", text));
            return true;
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to copy clipboard entry", throwable);
            return false;
        }
    }

    private String entrySummaryText(Context context, GboardClipboardVault.Entry entry) {
        String preview = snippet(entry.text, SUMMARY_LENGTH);
        String timestamp = formatTimestamp(context, entry.timestamp);
        if (timestamp.isEmpty()) {
            return preview;
        }
        return timestamp + " \u00b7 " + preview;
    }

    private static String formatTimestamp(Context context, long timestamp) {
        if (timestamp <= 0L) {
            return "";
        }
        try {
            Date date = new Date(timestamp);
            return DateFormat.getMediumDateFormat(context).format(date)
                    + " " + DateFormat.getTimeFormat(context).format(date);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String snippet(String text, int limit) {
        if (text == null) {
            return "";
        }
        String value = text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 1)).trim() + "\u2026";
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
