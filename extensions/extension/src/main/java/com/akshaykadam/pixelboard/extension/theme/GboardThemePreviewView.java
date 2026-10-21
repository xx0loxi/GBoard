/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lightweight keyboard mock used by the Theme Studio color picker. It mirrors the real keyboard
 * roles (background, key fill, letter label and digit label) without inflating Gboard widgets.
 */
public final class GboardThemePreviewView extends View {
    private static final String[][] LETTER_ROWS = new String[][]{
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"z", "x", "c", "v", "b", "n", "m"}
    };
    private static final String[] DIGITS = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] palette = new int[]{
            GboardThemeStudioSettings.DEFAULT_BACKGROUND,
            GboardThemeStudioSettings.DEFAULT_KEYS,
            GboardThemeStudioSettings.DEFAULT_TEXT,
            GboardThemeStudioSettings.DEFAULT_DIGITS
    };

    public GboardThemePreviewView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public GboardThemePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setPalette(int[] colors) {
        if (colors != null && colors.length >= 4) {
            palette = new int[]{colors[0], colors[1], colors[2], colors[3]};
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(palette[GboardThemeStudioSettings.SLOT_BACKGROUND]);
        canvas.drawRect(0f, 0f, width, height, paint);

        float horizontalPadding = width * 0.025f;
        float rowGap = height * 0.045f;
        float contentHeight = height - rowGap * 4f;
        float rowHeight = contentHeight / 4f;
        float top = rowGap;

        drawKeyRow(canvas, DIGITS, horizontalPadding, top, width - horizontalPadding, rowHeight, true);
        top += rowHeight + rowGap;

        for (String[] row : LETTER_ROWS) {
            drawKeyRow(canvas, row, horizontalPadding, top, width - horizontalPadding, rowHeight, false);
            top += rowHeight + rowGap;
        }
    }

    private void drawKeyRow(Canvas canvas, String[] labels, float left, float top, float right,
            float height, boolean digits) {
        int count = labels.length;
        float gap = Math.max(2f, (right - left) * 0.012f);
        float keyWidth = (right - left - gap * (count - 1)) / count;
        float radius = Math.min(height * 0.28f, keyWidth * 0.28f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(palette[GboardThemeStudioSettings.SLOT_KEYS]);
        for (int index = 0; index < count; index++) {
            float keyLeft = left + index * (keyWidth + gap);
            rect.set(keyLeft, top, keyLeft + keyWidth, top + height);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        int textColor = palette[digits
                ? GboardThemeStudioSettings.SLOT_DIGITS
                : GboardThemeStudioSettings.SLOT_TEXT];
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(10f, height * 0.38f));
        paint.setFakeBoldText(false);
        float baseline = top + height * 0.62f;
        for (int index = 0; index < count; index++) {
            float center = left + index * (keyWidth + gap) + keyWidth / 2f;
            canvas.drawText(labels[index], center, baseline, paint);
        }
    }
}
