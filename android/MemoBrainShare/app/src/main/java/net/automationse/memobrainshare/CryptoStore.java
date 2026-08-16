package net.automationse.memobrainshare;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts temporary pending-job payloads at rest using Android Keystore. */
public final class CryptoStore {
    private static final String ALIAS = "MemoBrainPendingDataKey";
    private static final byte[] MAGIC = new byte[]{'M', 'B', 'E', '1'};

    private CryptoStore() {}

    private static SecretKey key() throws Exception {
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

    public static void encrypt(InputStream in, OutputStream out) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();

        out.write(MAGIC);
        out.write(iv.length);
        out.write(iv);
        out.flush();

        try (CipherOutputStream cipherOut = new CipherOutputStream(out, cipher)) {
            copy(in, cipherOut);
        }
    }

    public static void decrypt(InputStream in, OutputStream out) throws Exception {
        byte[] magic = readExact(in, MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) throw new IOException("暗号化データ形式が不正です");

        int ivLength = in.read();
        if (ivLength < 12 || ivLength > 32) throw new IOException("暗号化データのIVが不正です");
        byte[] iv = readExact(in, ivLength);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));

        try (CipherInputStream cipherIn = new CipherInputStream(in, cipher)) {
            copy(cipherIn, out);
            out.flush();
        }
    }

    private static byte[] readExact(InputStream in, int size) throws IOException {
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int n = in.read(data, offset, size - offset);
            if (n < 0) throw new IOException("暗号化データが途中で終了しました");
            offset += n;
        }
        return data;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
