package dev.idlesign.fp30xcompanion;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ToneCatalog {
    private ToneCatalog() {
    }

    public static List<Tone> load(Context context) throws IOException {
        try (InputStream stream = context.getAssets().open("tones.csv")) {
            return parse(stream);
        }
    }

    public static Tone findByStableId(List<Tone> tones, String stableId) {
        if (stableId == null) {
            return null;
        }
        for (Tone tone : tones) {
            if (stableId.equals(tone.stableId())) {
                return tone;
            }
        }
        return null;
    }

    public static Tone findByName(List<Tone> tones, String name) {
        if (name == null) {
            return null;
        }
        for (Tone tone : tones) {
            if (name.equals(tone.name)) {
                return tone;
            }
        }
        return null;
    }

    static List<Tone> parse(InputStream stream) throws IOException {
        ArrayList<Tone> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("number,category,name,")) {
                throw new IOException("Invalid tone catalog header");
            }
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != 9) {
                    throw new IOException("Invalid tone catalog row: " + line);
                }
                result.add(new Tone(
                        Integer.parseInt(fields[0]),
                        fields[1],
                        fields[2],
                        Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]),
                        Integer.parseInt(fields[5]),
                        Tone.Kind.valueOf(fields[6]),
                        Integer.parseInt(fields[7]),
                        Integer.parseInt(fields[8])
                ));
            }
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid tone catalog value", error);
        }
        if (result.size() != 321) {
            throw new IOException("Expected 321 tones, found " + result.size());
        }
        return Collections.unmodifiableList(result);
    }
}
