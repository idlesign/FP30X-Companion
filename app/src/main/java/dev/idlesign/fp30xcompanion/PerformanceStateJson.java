package dev.idlesign.fp30xcompanion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class PerformanceStateJson {
    private PerformanceStateJson() {
    }

    public static JSONObject toJson(PerformanceState state) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("primary", state.primaryTone.stableId());
        putNullable(result, "layer", state.layerTone);
        putNullable(result, "left", state.leftTone);
        result.put("balance", state.balance);
        result.put("splitPoint", state.splitPoint);
        result.put("primaryOctaveShift", state.primaryOctaveShift);
        result.put("secondaryOctaveShift", state.secondaryOctaveShift);
        result.put("transpose", state.transpose);
        result.put("keyTouch", state.keyTouch);
        result.put("ambience", state.ambience);
        result.put("brilliance", state.brilliance);
        return result;
    }

    public static PerformanceState fromJson(JSONObject raw, List<Tone> tones) throws JSONException {
        Tone primary = requiredTone(tones, raw.getString("primary"));
        Tone layer = optionalTone(tones, raw.optString("layer", null));
        Tone left = optionalTone(tones, raw.optString("left", null));
        return new PerformanceState(
                primary,
                layer,
                left,
                raw.optInt("balance", PerformanceDefaults.BALANCE),
                raw.optInt("splitPoint", PerformanceDefaults.SPLIT_POINT),
                raw.optInt("primaryOctaveShift", PerformanceDefaults.OCTAVE_SHIFT),
                raw.optInt("secondaryOctaveShift", PerformanceDefaults.OCTAVE_SHIFT),
                raw.optInt("transpose", PerformanceDefaults.TRANSPOSE),
                raw.optInt("keyTouch", PerformanceDefaults.KEY_TOUCH),
                raw.optInt("ambience", PerformanceDefaults.AMBIENCE),
                raw.optInt("brilliance", PerformanceDefaults.BRILLIANCE)
        );
    }

    private static void putNullable(JSONObject object, String key, Tone tone) throws JSONException {
        if (tone == null) {
            object.put(key, JSONObject.NULL);
        } else {
            object.put(key, tone.stableId());
        }
    }

    private static Tone requiredTone(List<Tone> tones, String id) throws JSONException {
        Tone tone = ToneCatalog.findByStableId(tones, id);
        if (tone == null) {
            throw new JSONException("Unknown tone: " + id);
        }
        return tone;
    }

    private static Tone optionalTone(List<Tone> tones, String id) throws JSONException {
        if (id == null || id.isEmpty() || "null".equals(id)) {
            return null;
        }
        return requiredTone(tones, id);
    }
}
