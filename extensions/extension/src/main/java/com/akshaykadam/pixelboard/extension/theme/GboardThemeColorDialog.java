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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.akshaykadam.pixelboard.extension.R;
import com.akshaykadam.pixelboard.extension.settings.GboardSettingsText;

/**
 * Shared color picker used by the Theme Studio settings screen and by the Theme section header
 * injected into Gboard's theme listing.
 */
public final class GboardThemeColorDialog {
    private static final int[] PRESET_COLORS = new int[]{
            0xFF202124, 0xFF3C4043, 0xFF5F6368, 0xFFFFFFFF,
            0xFF000000, 0xFFF28B82, 0xFFFDD663, 0xFF81C995,
            0xFF8AB4F8, 0xFFC58AF9, 0xFFF6AEAE, 0xFF7B1FA2,
            0xFF018786, 0xFF03DAC6, 0xFFFF7043, 0xFFFFB300
    };

    private GboardThemeColorDialog() {
    }

    public static AlertDialog create(Context context, int slot, boolean autoEnable,
            Runnable onSaved) {
        if (context == null) {
            return null;
        }
        final int[] palette = GboardThemeStudioSettings.readPalette(context);
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

        final EditText hex = new EditText(context);
        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hex.setHint(GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_hex_hint));
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

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(GboardSettingsText.get(context,
                        R.string.gboard_patches_theme_studio_palette_title))
                .setView(scroll)
                .setNegativeButton(R.string.gboard_patches_dialog_cancel, null)
                .setPositiveButton(R.string.gboard_patches_dialog_save, null)
                .create();
        final String invalidColor = GboardSettingsText.get(context,
                R.string.gboard_patches_theme_studio_invalid_color);

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
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(clicked -> {
                    String value = hex.getText() == null ? "" : hex.getText().toString();
                    if (!GboardThemeStudioSettings.isValidColor(value)) {
                        hex.setError(invalidColor);
                        return;
                    }
                    GboardThemeStudioSettings.writeColor(
                            context,
                            slot,
                            GboardThemeStudioSettings.parseColor(value));
                    if (autoEnable) {
                        GboardThemeStudioSettings.setEnabled(context, true);
                    }
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    GboardThemeStudioRuntime.refreshKeyboards();
                    dialog.dismiss();
                }));

        return dialog;
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

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
