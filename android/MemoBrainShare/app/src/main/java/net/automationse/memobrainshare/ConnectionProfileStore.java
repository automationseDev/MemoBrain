package net.automationse.memobrainshare;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Encrypted collection of independently selectable Dify/Knowledge connections. */
public final class ConnectionProfileStore {
    private static final Object LOCK = new Object();
    private static final String FILE_NAME = "connection_profiles.json.enc";

    public static final class Profile {
        public final String id, name, knowledgeName, base, key, chatUrl;
        public Profile(String id, String name, String knowledgeName, String base, String key, String chatUrl) {
            this.id = id == null || id.isEmpty() ? UUID.randomUUID().toString() : id;
            this.name = clean(name);
            this.knowledgeName = clean(knowledgeName);
            this.base = clean(base);
            this.key = clean(key);
            this.chatUrl = clean(chatUrl);
        }
        public boolean isConfigured() { return !base.isEmpty() && !key.isEmpty(); }
        JSONObject json() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("name", name); o.put("knowledge_name", knowledgeName);
            o.put("base", base); o.put("key", key); o.put("chat_url", chatUrl);
            return o;
        }
        static Profile from(JSONObject o) {
            return new Profile(o.optString("id"), o.optString("name"), o.optString("knowledge_name"),
                    o.optString("base"), o.optString("key"), o.optString("chat_url"));
        }
        private static String clean(String value) { return value == null ? "" : value.trim(); }
    }

    private final Context context;
    public ConnectionProfileStore(Context context) { this.context = context.getApplicationContext(); }

    public void migrate(SecurePrefs legacy) {
        synchronized (LOCK) {
            if (file().isFile()) return;
            try {
                JSONObject root = emptyRoot();
                if (legacy.isConfigured() || !legacy.getChatWebUrl().isEmpty()) {
                    Profile p = new Profile(null, "デフォルト", "", legacy.getBase(), legacy.getKey(), legacy.getChatWebUrl());
                    root.getJSONArray("profiles").put(p.json());
                    root.put("selected_id", p.id);
                }
                write(root);
            } catch (Exception e) { throw new IllegalStateException("接続プロファイルを初期化できません", e); }
        }
    }

    public List<Profile> list() {
        synchronized (LOCK) {
            ArrayList<Profile> out = new ArrayList<>();
            JSONArray a = readSafe().optJSONArray("profiles");
            if (a != null) for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) != null) out.add(Profile.from(a.optJSONObject(i)));
            return out;
        }
    }

    public Profile selected() {
        synchronized (LOCK) {
            JSONObject root = readSafe();
            Profile selected = findIn(root, root.optString("selected_id"));
            if (selected != null) return selected;
            JSONArray a = root.optJSONArray("profiles");
            return a != null && a.optJSONObject(0) != null ? Profile.from(a.optJSONObject(0)) : null;
        }
    }

    public Profile find(String id) { synchronized (LOCK) { return findIn(readSafe(), id); } }

    public void save(Profile profile, boolean select) {
        synchronized (LOCK) {
            try {
                JSONObject root = readSafe(); JSONArray old = root.optJSONArray("profiles"); JSONArray next = new JSONArray();
                boolean replaced = false;
                if (old != null) for (int i = 0; i < old.length(); i++) {
                    JSONObject item = old.optJSONObject(i); if (item == null) continue;
                    if (profile.id.equals(item.optString("id"))) { next.put(profile.json()); replaced = true; } else next.put(item);
                }
                if (!replaced) next.put(profile.json());
                root.put("profiles", next);
                if (select || root.optString("selected_id").isEmpty()) root.put("selected_id", profile.id);
                write(root);
            } catch (Exception e) { throw new IllegalStateException("接続プロファイルを保存できません", e); }
        }
    }

    public void select(String id) {
        synchronized (LOCK) {
            try { JSONObject root = readSafe(); if (findIn(root, id) == null) return; root.put("selected_id", id); write(root); }
            catch (Exception e) { throw new IllegalStateException("接続プロファイルを選択できません", e); }
        }
    }

    public void delete(String id) {
        synchronized (LOCK) {
            try {
                JSONObject root = readSafe(); JSONArray old = root.optJSONArray("profiles"); JSONArray next = new JSONArray();
                if (old != null) for (int i = 0; i < old.length(); i++) { JSONObject p = old.optJSONObject(i); if (p != null && !id.equals(p.optString("id"))) next.put(p); }
                root.put("profiles", next);
                if (id.equals(root.optString("selected_id"))) root.put("selected_id", next.optJSONObject(0) == null ? "" : next.optJSONObject(0).optString("id"));
                write(root);
            } catch (Exception e) { throw new IllegalStateException("接続プロファイルを削除できません", e); }
        }
    }

    private Profile findIn(JSONObject root, String id) {
        if (id == null || id.isEmpty()) return null; JSONArray a = root.optJSONArray("profiles");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject p = a.optJSONObject(i); if (p != null && id.equals(p.optString("id"))) return Profile.from(p); }
        return null;
    }
    private File file() { return new File(context.getFilesDir(), FILE_NAME); }
    private JSONObject emptyRoot() throws Exception { JSONObject root = new JSONObject(); root.put("version", 1); root.put("selected_id", ""); root.put("profiles", new JSONArray()); return root; }
    private JSONObject readSafe() {
        if (!file().isFile()) try { return emptyRoot(); } catch (Exception e) { throw new IllegalStateException(e); }
        try (FileInputStream in = new FileInputStream(file()); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CryptoStore.decrypt(in, out); return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) { throw new IllegalStateException("接続プロファイルを復号できません", e); }
    }
    private void write(JSONObject root) throws Exception {
        File tmp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
        try (ByteArrayInputStream in = new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8)); FileOutputStream out = new FileOutputStream(tmp)) { CryptoStore.encrypt(in, out); }
        if (file().exists() && !file().delete()) throw new IllegalStateException("旧設定を更新できません");
        if (!tmp.renameTo(file())) throw new IllegalStateException("設定ファイルを確定できません");
    }
}
