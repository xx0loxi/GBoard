/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime entry points called from the patched Gboard clipboard handler.
 */
public final class GboardClipboardVaultRuntime {
    private static final String TAG = "GboardClipboardVault";
    private static volatile Field contextField;
    private static volatile Method currentApplicationMethod;

    private GboardClipboardVaultRuntime() {
    }

    public static void capture(Context context, ClipData clipData) {
        if (context == null || clipData == null) {
            return;
        }
        try {
            if (clipData.getItemCount() == 0) {
                return;
            }
            ClipData.Item item = clipData.getItemAt(0);
            if (item == null) {
                return;
            }
            CharSequence text = item.coerceToText(context);
            if (text != null && text.length() > 0) {
                GboardClipboardVault.capture(context, text);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to capture clipboard item", throwable);
        }
    }

    public static void restoreOnStartInputView(Object handler) {
        Context context = resolveContext(handler);
        if (context == null) {
            return;
        }
        try {
            int restored = GboardClipboardVault.restoreMissing(context);
            if (restored > 0) {
                Log.i(TAG, "Restored " + restored + " encrypted clipboard item(s).");
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to restore clipboard vault", throwable);
        }
    }

    public static Context resolveContext(Object handler) {
        if (handler != null) {
            try {
                Field field = resolveContextField(handler);
                if (field != null) {
                    Object value = field.get(handler);
                    if (value instanceof Context) {
                        return (Context) value;
                    }
                }
            } catch (Throwable ignored) {
                // Fall through to the process-wide application context.
            }
        }
        try {
            if (currentApplicationMethod == null) {
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                currentApplicationMethod = activityThread.getDeclaredMethod(
                        "currentApplication");
                currentApplicationMethod.setAccessible(true);
            }
            Object application = currentApplicationMethod.invoke(null);
            if (application instanceof Context) {
                return (Context) application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Field resolveContextField(Object handler) {
        if (contextField == null) {
            for (Field field : handler.getClass().getDeclaredFields()) {
                if (Context.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    contextField = field;
                    break;
                }
            }
        }
        return contextField;
    }
}
