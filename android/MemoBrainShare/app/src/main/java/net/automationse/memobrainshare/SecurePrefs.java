package net.automationse.memobrainshare;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurePrefs {
    private static final String ALIAS = "MemoBrainApiKey";
    private static final String PREFS = "mb";
    private static final String KEY_API = "key";
    private static final String KEY_BASE_SECURE = "base_secure";
    private static final String KEY_BASE_LEGACY = "base";

    private final Context context;

    public SecurePrefs(Context context) {
        this.context = context.getApplicationContext();
    }

    private SharedPreferences sp() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
    }

    private String encrypt(String value) throws Exception {
        Cipher cp = Cipher.getInstance("AES/GCM/NoPadding");
        cp.init(Cipher.ENCRYPT_MODE, key());
        byte[] enc = cp.doFinal((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cp.getIV(), Base64.NO_WRAP)
                + ":"
                + Base64.encodeToString(enc, Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        if (value == null || value.isEmpty()) return "";
        String[] p = value.split(":", 2);
        if (p.length != 2) return "";
        Cipher cp = Cipher.getInstance("AES/GCM/NoPadding");
        cp.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
        return new String(cp.doFinal(Base64.decode(p[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }

    private void putEncrypted(String name, String value) throws Exception {
        sp().edit().putString(name, encrypt(value == null ? "" : value)).apply();
    }

    private String getEncrypted(String name) {
        try {
            return decrypt(sp().getString(name, ""));
        } catch (Exception e) {
            return "";
        }
    }

    public void putKey(String value) throws Exception {
        putEncrypted(KEY_API, value == null ? "" : value.trim());
    }

    public String getKey() {
        return getEncrypted(KEY_API);
    }

    public void putBase(String value) throws Exception {
        putEncrypted(KEY_BASE_SECURE, value == null ? "" : value.trim());
        sp().edit().remove(KEY_BASE_LEGACY).apply();
    }

    public String getBase() {
        String encrypted = getEncrypted(KEY_BASE_SECURE);
        if (!encrypted.isEmpty()) return encrypted;

        String legacy = sp().getString(KEY_BASE_LEGACY, "");
        if (legacy == null || legacy.trim().isEmpty()) return "";
        try {
            putBase(legacy.trim());
        } catch (Exception ignored) {
            return "";
        }
        return legacy.trim();
    }

    public boolean isConfigured() {
        return !getBase().isEmpty() && !getKey().isEmpty();
    }

    public void clearConnection() {
        sp().edit()
                .remove(KEY_API)
                .remove(KEY_BASE_SECURE)
                .remove(KEY_BASE_LEGACY)
                .apply();
    }
}
