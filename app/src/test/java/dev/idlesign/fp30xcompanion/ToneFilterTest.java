package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ToneFilterTest {
    private final Tone zebra = tone(1, "Other", "Zebra Pad", 89);
    private final Tone piano = tone(2, "Piano", "Concert Piano", 1);
    private final Tone bass = tone(3, "GM2", "Acoustic Bass", 33);
    private final List<Tone> tones = Arrays.asList(zebra, piano, bass);

    @Test
    public void allResultsAreSortedAlphabetically() {
        List<Tone> result = ToneFilter.apply(tones, Set.of(), ToneFamily.ALL, "");
        assertEquals("Acoustic Bass", result.get(0).name);
        assertEquals("Concert Piano", result.get(1).name);
        assertEquals("Zebra Pad", result.get(2).name);
    }

    @Test
    public void filtersFavoritesFamilyAndQuery() {
        HashSet<String> favorites = new HashSet<>();
        favorites.add(zebra.stableId());
        assertEquals(List.of(zebra),
                ToneFilter.apply(tones, favorites, ToneFamily.FAVORITES, ""));
        assertEquals(List.of(piano),
                ToneFilter.apply(tones, favorites, ToneFamily.ACOUSTIC_PIANOS, ""));
        assertEquals(List.of(bass),
                ToneFilter.apply(tones, favorites, ToneFamily.BASSES, "bass"));
        assertEquals(List.of(),
                ToneFilter.apply(tones, favorites, ToneFamily.BASSES, "piano"));
    }

    @Test
    public void hidesGm2DuplicatesWhenAnEquivalentBuiltInToneExists() {
        Tone builtIn = new Tone(10, "Other", "AcousticBass", 121, 0, 33,
                Tone.Kind.INTERNAL, 6, 4);
        Tone gm2 = new Tone(11, "GM2", "AcousticBass", 121, 0, 33,
                Tone.Kind.BANK_PROGRAM, -1, -1);
        List<Tone> catalog = Arrays.asList(builtIn, gm2);

        assertEquals(List.of(builtIn),
                ToneFilter.apply(catalog, Set.of(), ToneFamily.ALL, ""));
        assertEquals(List.of(builtIn),
                ToneFilter.apply(catalog, Set.of(), ToneFamily.BASSES, ""));
    }

    @Test
    public void melodicProgramBoundariesMapToEveryFamily() {
        ToneFamily[] families = {
                ToneFamily.ACOUSTIC_PIANOS,
                ToneFamily.ELECTRIC_PIANOS,
                ToneFamily.HARPSICHORDS_CLAVINETS,
                ToneFamily.MALLETS_BELLS,
                ToneFamily.ORGANS_ACCORDIONS,
                ToneFamily.GUITARS,
                ToneFamily.BASSES,
                ToneFamily.SOLO_STRINGS_HARPS,
                ToneFamily.ENSEMBLES_CHOIRS_ORCHESTRA,
                ToneFamily.BRASS,
                ToneFamily.SAX_REEDS,
                ToneFamily.FLUTES_PIPES,
                ToneFamily.SYNTH_LEADS,
                ToneFamily.SYNTH_PADS,
                ToneFamily.ATMOSPHERES_TEXTURES,
                ToneFamily.WORLD_INSTRUMENTS,
                ToneFamily.PERCUSSION
        };
        int[][] ranges = {
                {1, 4}, {5, 6}, {7, 8}, {9, 16}, {17, 24}, {25, 32},
                {33, 40}, {41, 48}, {49, 56}, {57, 64}, {65, 72}, {73, 80},
                {81, 88}, {89, 96}, {97, 104}, {105, 112}, {113, 120}
        };
        for (int index = 0; index < families.length; index++) {
            ToneFamily family = families[index];
            assertTrue(family.matches(tone(index, "GM2", "first", ranges[index][0])));
            assertTrue(family.matches(tone(index, "GM2", "last", ranges[index][1])));
            if (ranges[index][0] > 1) {
                assertFalse(family.matches(tone(index, "GM2", "before", ranges[index][0] - 1)));
            }
            assertFalse(family.matches(tone(index, "GM2", "after", ranges[index][1] + 1)));
        }
    }

    @Test
    public void completeCatalogHasApprovedFamilyCounts() throws IOException {
        Path path = Path.of("src/main/assets/tones.csv");
        if (!Files.exists(path)) {
            path = Path.of("app/src/main/assets/tones.csv");
        }
        List<Tone> catalog;
        try (var stream = Files.newInputStream(path)) {
            catalog = ToneCatalog.parse(stream);
        }
        assertEquals(315,
                ToneFilter.apply(catalog, Set.of(), ToneFamily.ALL, "").size());
        assertFamilyCount(catalog, ToneFamily.ACOUSTIC_PIANOS, 19);
        assertFamilyCount(catalog, ToneFamily.ELECTRIC_PIANOS, 13);
        assertFamilyCount(catalog, ToneFamily.HARPSICHORDS_CLAVINETS, 8);
        assertFamilyCount(catalog, ToneFamily.MALLETS_BELLS, 12);
        assertFamilyCount(catalog, ToneFamily.ORGANS_ACCORDIONS, 30);
        assertFamilyCount(catalog, ToneFamily.GUITARS, 24);
        assertFamilyCount(catalog, ToneFamily.BASSES, 18);
        assertFamilyCount(catalog, ToneFamily.SOLO_STRINGS_HARPS, 12);
        assertFamilyCount(catalog, ToneFamily.ENSEMBLES_CHOIRS_ORCHESTRA, 27);
        assertFamilyCount(catalog, ToneFamily.BRASS, 20);
        assertFamilyCount(catalog, ToneFamily.SAX_REEDS, 8);
        assertFamilyCount(catalog, ToneFamily.FLUTES_PIPES, 8);
        assertFamilyCount(catalog, ToneFamily.SYNTH_LEADS, 17);
        assertFamilyCount(catalog, ToneFamily.SYNTH_PADS, 15);
        assertFamilyCount(catalog, ToneFamily.ATMOSPHERES_TEXTURES, 11);
        assertFamilyCount(catalog, ToneFamily.WORLD_INSTRUMENTS, 10);
        assertFamilyCount(catalog, ToneFamily.PERCUSSION, 13);
        assertFamilyCount(catalog, ToneFamily.DRUM_KITS, 9);
        assertFamilyCount(catalog, ToneFamily.ALL_SOUND_EFFECTS, 41);
        assertFamilyCount(catalog, ToneFamily.INSTRUMENT_PERFORMANCE_NOISES, 5);
        assertFamilyCount(catalog, ToneFamily.NATURE, 6);
        assertFamilyCount(catalog, ToneFamily.ANIMALS, 4);
        assertFamilyCount(catalog, ToneFamily.HOUSEHOLD_SIGNALS, 6);
        assertFamilyCount(catalog, ToneFamily.TRANSPORT_MACHINERY, 10);
        assertFamilyCount(catalog, ToneFamily.PEOPLE_BODY, 6);
        assertFamilyCount(catalog, ToneFamily.SHOTS_EXPLOSIONS, 4);
    }

    @Test
    public void drumsOverrideProgramAndEffectsHaveSpecificSubfamilies() {
        Tone drumKit = tone(1, "Drums", "Orchestra Set", 49);
        assertTrue(ToneFamily.DRUM_KITS.matches(drumKit));
        assertFalse(ToneFamily.ENSEMBLES_CHOIRS_ORCHESTRA.matches(drumKit));

        ToneFamily[] effects = {
                ToneFamily.INSTRUMENT_PERFORMANCE_NOISES,
                ToneFamily.INSTRUMENT_PERFORMANCE_NOISES,
                ToneFamily.NATURE,
                ToneFamily.ANIMALS,
                ToneFamily.HOUSEHOLD_SIGNALS,
                ToneFamily.TRANSPORT_MACHINERY,
                ToneFamily.PEOPLE_BODY,
                ToneFamily.SHOTS_EXPLOSIONS
        };
        for (int program = 121; program <= 128; program++) {
            Tone effect = tone(program, "GM2", "effect", program);
            assertTrue(ToneFamily.ALL_SOUND_EFFECTS.matches(effect));
            assertTrue(effects[program - 121].matches(effect));
        }
    }

    private void assertFamilyCount(List<Tone> catalog, ToneFamily family, int expected) {
        assertEquals(family.name(), expected,
                ToneFilter.apply(catalog, Set.of(), family, "").size());
    }

    private Tone tone(int number, String category, String name, int program) {
        return new Tone(number, category, name, 0, 68, program,
                Tone.Kind.INTERNAL, 0, number - 1);
    }
}
