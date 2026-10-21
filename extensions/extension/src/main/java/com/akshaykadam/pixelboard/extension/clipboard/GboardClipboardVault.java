/**
 * PixelBoard - Gboard Enhancement Mod
 *
 * Maintained and customized by Akshay Kadam (@Akshayykadam)
 * Repository: https://github.com/Akshayykadam/PixelBoard
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.akshaykadam.pixelboard.extension.settings.GboardPatchesFeatureAvailability;
import com.akshaykadam.pixelboard.extension.settings.GboardPatchesSettings;

/**
 * AES-GCM clipboard archive stored in app-private, no-backup storage.
 *
 * The file is a tiny framed container: a four-byte magic/version header followed by records made
 * of a length, a 12-byte GCM nonce and the ciphertext. Every record is encrypted separately with a
 * key that never leaves Android Keystore, so the archive cannot be copied and decrypted on another
 * device and it is excluded from Google backups.
 */
public final class GboardClipboardVault {
    private static final String TAG = "GboardClipboardVault";
    private static final String PREF_ENABLED = "pref_clipboard_vault_enabled";
    private static final String PREF_LAST_HASH = "pref_clipboard_vault_last_hash";
    private static final String PREF_LAST_AT = "pref_clipboard_vault_last_at";
    private static final String PREF_RESTORE_WATERMARK = "pref_clipboard_vault_restore_watermark";

    private static final String FILE_NAME = "pixelboard_clipboard_vault.bin";
    private static final String KEY_ALIAS = "pixelboard_clipboard_vault_aes_gcm_v1";
    private static final byte[] MAGIC = new byte[]{'P', 'B', 'V', '1'};
    private static final int FORMAT_VERSION = 1;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int MAX_TEXT_LENGTH = 20000;
    private static final long DUPLICATE_WINDOW_MS = 5000L;

    private GboardClipboardVault() {
    }

    public static boolean isEnabled(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return preferences(context).getBoolean(PREF_ENABLED, true);
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to read vault enabled flag", throwable);
            return false;
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (context == null) {
            return;
        }
        try {
            preferences(context).edit().putBoolean(PREF_ENABLED, enabled).apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to write vault enabled flag", throwable);
        }
    }

    public static void capture(Context context, CharSequence text) {
        if (context == null || text == null) {
            return;
        }
        if (!isEnabled(context) || !hasFeature(context)) {
            return;
        }
        String value = text.toString();
        if (value.isEmpty()) {
            return;
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            value = value.substring(0, MAX_TEXT_LENGTH);
        }

        try {
            SharedPreferences preferences = preferences(context);
            String hash = sha256(value);
            long now = System.currentTimeMillis();
            long lastAt = preferences.getLong(PREF_LAST_AT, 0L);
            if (hash.equals(preferences.getString(PREF_LAST_HASH, null))
                    && now - lastAt < DUPLICATE_WINDOW_MS) {
                return;
            }

            JSONObject payload = new JSONObject();
            payload.put("t", now);
            payload.put("x", value);
            payload.put("s", 0);
            appendRecord(context, payload.toString().getBytes(StandardCharsets.UTF_8));

            preferences.edit()
                    .putString(PREF_LAST_HASH, hash)
                    .putLong(PREF_LAST_AT, now)
                    .apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to archive clipboard item", throwable);
        }
    }

    public static int getCount(Context context) {
        try {
            return readEntries(context).size();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to count vault entries", throwable);
            return 0;
        }
    }

    public static long getSizeBytes(Context context) {
        try {
            File file = vaultFile(context);
            return file != null && file.isFile() ? file.length() : 0L;
        } catch (Throwable throwable) {
            return 0L;
        }
    }

    public static long getLastTimestamp(Context context) {
        List<Entry> entries = readEntries(context);
        return entries.isEmpty() ? 0L : entries.get(entries.size() - 1).timestamp;
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        try {
            File file = vaultFile(context);
            if (file != null && file.isFile() && !file.delete()) {
                Log.w(TAG, "Failed to delete vault file");
            }
            preferences(context).edit()
                    .remove(PREF_LAST_HASH)
                    .remove(PREF_LAST_AT)
                    .remove(PREF_RESTORE_WATERMARK)
                    .apply();
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to wipe clipboard vault", throwable);
        }
    }

    public static int restoreMissing(Context context) {
        if (context == null || !isEnabled(context) || !hasFeature(context)) {
            return 0;
        }
        List<Entry> entries = readEntries(context);
        if (entries.isEmpty()) {
            return 0;
        }
        long newestInHistory = readNewestHistoryTimestamp(context);
        SharedPreferences preferences = preferences(context);
        long watermark = preferences.getLong(PREF_RESTORE_WATERMARK, 0L);
        // An empty history means the user cleared Gboard's database. Replay the whole archive.
        long restoreSince = newestInHistory <= 0L ? 0L : Math.max(newestInHistory, watermark);

        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return 0;
        }

        List<Entry> pending = new ArrayList<Entry>();
        for (Entry entry : entries) {
            if (entry.timestamp > restoreSince) {
                pending.add(entry);
            }
        }

        // Gboard hides clips older than its one-hour retention window even when they are still in
        // the database, so restored items are stamped as if they were copied just now. The vault
        // watermark still tracks the original timestamps to avoid restoring the same item twice.
        long baseTimestamp = System.currentTimeMillis();
        Uri clipsUri = Uri.parse("content://" + context.getPackageName() + ".clipboard_content/clips");
        int inserted = 0;
        long lastInserted = restoreSince;
        for (int index = 0; index < pending.size(); index++) {
            Entry entry = pending.get(index);
            long visibleTimestamp = Math.max(
                    entry.timestamp,
                    baseTimestamp - (pending.size() - 1 - index));
            try {
                ContentValues values = new ContentValues();
                values.put("text", entry.text);
                values.put("timestamp", visibleTimestamp);
                values.put("item_type", 0);
                values.put("entity_type", 0);
                values.put("sensitive", 0);
                if (resolver.insert(clipsUri, values) != null) {
                    inserted++;
                    lastInserted = Math.max(lastInserted, entry.timestamp);
                }
            } catch (Throwable throwable) {
                Log.w(TAG, "Failed to restore one clipboard item", throwable);
                break;
            }
        }
        if (inserted > 0) {
            preferences.edit().putLong(PREF_RESTORE_WATERMARK, lastInserted).apply();
        }
        return inserted;
    }

    public static List<Entry> readEntries(Context context) {
        File file = vaultFile(context);
        if (file == null || !file.isFile() || file.length() == 0L) {
            return Collections.emptyList();
        }
        List<Entry> entries = new ArrayList<Entry>();
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
            byte[] magic = new byte[MAGIC.length];
            input.readFully(magic);
            if (!java.util.Arrays.equals(magic, MAGIC)) {
                return Collections.emptyList();
            }
            int version = input.readInt();
            if (version != FORMAT_VERSION) {
                return Collections.emptyList();
            }
            while (true) {
                int cipherLength;
                try {
                    cipherLength = input.readInt();
                } catch (EOFException endOfFile) {
                    break;
                }
                if (cipherLength <= 0 || cipherLength > 4 * 1024 * 1024) {
                    break;
                }
                byte[] nonce = new byte[NONCE_LENGTH];
                input.readFully(nonce);
                byte[] ciphertext = new byte[cipherLength];
                input.readFully(ciphertext);

                try {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_BITS, nonce));
                    String json = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
                    JSONObject payload = new JSONObject(json);
                    String text = payload.optString("x", "");
                    if (!text.isEmpty()) {
                        entries.add(new Entry(payload.optLong("t", 0L), text));
                    }
                } catch (Throwable throwable) {
                    Log.w(TAG, "Skipping unreadable vault record", throwable);
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to read clipboard vault", throwable);
        }
        return entries;
    }

    private static void appendRecord(Context context, byte[] plaintext) throws Exception {
        File file = vaultFile(context);
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create vault directory");
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_BITS, nonce));
        byte[] ciphertext = cipher.doFinal(plaintext);

        boolean newFile = !file.isFile() || file.length() == 0L;
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file, true))) {
            if (newFile) {
                output.write(MAGIC);
                output.writeInt(FORMAT_VERSION);
            }
            output.writeInt(ciphertext.length);
            output.write(nonce);
            output.write(ciphertext);
            output.flush();
        }
    }

    private static long readNewestHistoryTimestamp(Context context) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            return 0L;
        }
        Uri clipsUri = Uri.parse("content://" + context.getPackageName() + ".clipboard_content/clips");
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    clipsUri,
                    new String[]{"timestamp"},
                    null,
                    null,
                    "timestamp DESC");
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "Failed to read clipboard history watermark", throwable);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0L;
    }

    private static boolean hasFeature(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(
                context,
                GboardPatchesFeatureAvailability.FEATURE_CLIPBOARD_VAULT);
    }

    private static SharedPreferences preferences(Context context) {
        return GboardPatchesSettings.preferences(context);
    }

    private static File vaultFile(Context context) {
        if (context == null) {
            return null;
        }
        Context app = context.getApplicationContext();
        Context lookup = app != null ? app : context;
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            directory = lookup.getNoBackupFilesDir();
        } else {
            directory = lookup.getFilesDir();
        }
        if (directory == null) {
            directory = lookup.getFilesDir();
        }
        return directory == null ? null : new File(directory, FILE_NAME);
    }

    private static SecretKey secretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            generator.generateKey();
        }
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
        }
        return builder.toString();
    }

    public static final class Entry {
        public final long timestamp;
        public final String text;

        Entry(long timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }
    }
}
