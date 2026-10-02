package dev.idlesign.fp30xcompanion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ToneFilter {
    private ToneFilter() {
    }

    public static List<Tone> apply(
            List<Tone> tones,
            Set<String> favorites,
            ToneFamily family,
            String query
    ) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Tone> result = new ArrayList<>();
        HashSet<String> builtInIds = new HashSet<>();
        for (Tone tone : tones) {
            if (!"GM2".equals(tone.category)) {
                builtInIds.add(tone.stableId());
            }
        }
        for (Tone tone : tones) {
            if ("GM2".equals(tone.category) && builtInIds.contains(tone.stableId())) {
                continue;
            }
            boolean familyMatches = family == ToneFamily.FAVORITES
                    ? favorites.contains(tone.stableId())
                    : family.matches(tone);
            boolean queryMatches = normalized.isEmpty()
                    || tone.name.toLowerCase(Locale.ROOT).contains(normalized)
                    || tone.category.toLowerCase(Locale.ROOT).contains(normalized);
            if (familyMatches && queryMatches) {
                result.add(tone);
            }
        }
        Collections.sort(result, (left, right) -> left.name.compareToIgnoreCase(right.name));
        return result;
    }
}
