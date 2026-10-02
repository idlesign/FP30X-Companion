package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PresetRepository {
    interface ActivePresetStore {
        String get();

        void set(String id);
    }

    interface Clock {
        long nowMillis();
    }

    public static final String DEFAULT_ID = "default";
    public static final String DEFAULT_NAME = "Base profile";
    private static final int FORMAT_VERSION = 1;
    private static final String PRESETS_FILE = "presets.json";
    private static final String ACTIVE_PRESET_KEY = "active_preset_id";

    private final File file;
    private final ActivePresetStore activeStore;
    private final Clock clock;
    private final List<Tone> tones;
    private final String defaultName;
    private final PerformanceState defaultState;
    private final ArrayList<Preset> presets = new ArrayList<>();

    public PresetRepository(Context context, List<Tone> tones) {
        this(
                new File(context.getFilesDir(), PRESETS_FILE),
                sharedPreferencesStore(context.getSharedPreferences("fp30x", Context.MODE_PRIVATE)),
                System::currentTimeMillis,
                tones,
                context.getString(R.string.default_profile_name)
        );
    }

    PresetRepository(File file, ActivePresetStore activeStore, Clock clock, List<Tone> tones) {
        this(file, activeStore, clock, tones, DEFAULT_NAME);
    }

    PresetRepository(
            File file,
            ActivePresetStore activeStore,
            Clock clock,
            List<Tone> tones,
            String defaultName
    ) {
        this.file = file;
        this.activeStore = activeStore;
        this.clock = clock;
        this.tones = new ArrayList<>(tones);
        this.defaultName = defaultName;
        defaultState = PerformanceDefaults.fromCatalog(tones);
        loadFromDisk();
        if (find(activeStore.get()) == null && !DEFAULT_ID.equals(activeStore.get())) {
            activeStore.set(DEFAULT_ID);
        }
    }

    public List<Preset> all() {
        return Collections.unmodifiableList(new ArrayList<>(presets));
    }

    public String activeId() {
        String id = activeStore.get();
        return id == null ? DEFAULT_ID : id;
    }

    public Preset activePreset() {
        Preset active = find(activeId());
        return active == null ? defaultPreset() : active;
    }

    public Preset defaultPreset() {
        return new Preset(DEFAULT_ID, defaultName, defaultState, 0, 0);
    }

    public Preset save(String presetId, String name, PerformanceState state) throws IOException {
        long now = clock.nowMillis();
        Preset existing = find(presetId);
        Preset saved;
        if (existing == null || DEFAULT_ID.equals(presetId)) {
            saved = new Preset(UUID.randomUUID().toString(), name, state, now, now);
            presets.add(saved);
        } else {
            saved = existing.update(name, state, now);
            presets.set(presets.indexOf(existing), saved);
        }
        persist();
        activeStore.set(saved.id);
        return saved;
    }

    public Preset duplicate(String presetId) throws IOException {
        Preset source = DEFAULT_ID.equals(presetId) ? defaultPreset() : find(presetId);
        if (source == null) {
            source = activePreset();
        }
        long now = clock.nowMillis();
        Preset copy = new Preset(
                UUID.randomUUID().toString(),
                "[C] " + source.name,
                source.state,
                now,
                now
        );
        presets.add(copy);
        persist();
        activeStore.set(copy.id);
        return copy;
    }

    public Preset activate(String presetId) {
        Preset preset = find(presetId);
        if (preset == null) {
            preset = defaultPreset();
        }
        activeStore.set(preset.id);
        return preset;
    }

    public Preset resetToDefault() {
        activeStore.set(DEFAULT_ID);
        return defaultPreset();
    }

    public void delete(String presetId) throws IOException {
        Preset preset = find(presetId);
        if (preset == null) {
            return;
        }
        presets.remove(preset);
        persist();
        if (presetId.equals(activeId())) {
            activeStore.set(DEFAULT_ID);
        }
    }

    private Preset find(String id) {
        if (id == null || DEFAULT_ID.equals(id)) {
            return null;
        }
        for (Preset preset : presets) {
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return null;
    }

    private void loadFromDisk() {
        if (!file.isFile()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(readText(file));
            if (root.optInt("version", 0) != FORMAT_VERSION) {
                return;
            }
            JSONArray rawPresets = root.getJSONArray("presets");
            for (int index = 0; index < rawPresets.length(); index++) {
                JSONObject raw = rawPresets.getJSONObject(index);
                presets.add(new Preset(
                        raw.getString("id"),
                        raw.getString("name"),
                        PerformanceStateJson.fromJson(raw.getJSONObject("state"), tones),
                        raw.getLong("createdAt"),
                        raw.getLong("updatedAt")
                ));
            }
        } catch (IOException | JSONException | IllegalArgumentException ignored) {
            presets.clear();
        }
    }

    private void persist() throws IOException {
        try {
            JSONObject root = new JSONObject();
            root.put("version", FORMAT_VERSION);
            JSONArray rawPresets = new JSONArray();
            for (Preset preset : presets) {
                JSONObject raw = new JSONObject();
                raw.put("id", preset.id);
                raw.put("name", preset.name);
                raw.put("createdAt", preset.createdAtMillis);
                raw.put("updatedAt", preset.updatedAtMillis);
                raw.put("state", PerformanceStateJson.toJson(preset.state));
                rawPresets.put(raw);
            }
            root.put("presets", rawPresets);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Could not create preset directory");
            }
            File temporary = new File(file.getPath() + ".tmp");
            writeText(temporary, root.toString(2));
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not replace presets file");
            }
            if (!temporary.renameTo(file)) {
                throw new IOException("Could not finalize presets file");
            }
        } catch (JSONException error) {
            throw new IOException("Could not encode presets", error);
        }
    }

    private static String readText(File source) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void writeText(File target, String text) throws IOException {
        try (FileOutputStream output = new FileOutputStream(target)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static ActivePresetStore sharedPreferencesStore(SharedPreferences preferences) {
        return new ActivePresetStore() {
            @Override
            public String get() {
                return preferences.getString(ACTIVE_PRESET_KEY, DEFAULT_ID);
            }

            @Override
            public void set(String id) {
                preferences.edit().putString(ACTIVE_PRESET_KEY, id).apply();
            }
        };
    }
}
