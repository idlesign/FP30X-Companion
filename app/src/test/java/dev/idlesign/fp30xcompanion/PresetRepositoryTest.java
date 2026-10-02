package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PresetRepositoryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final Tone bass = tone(3, "Acoustic Bass");
    private final List<Tone> tones = Arrays.asList(piano, strings, bass);

    @Test
    public void savesUpdatesAndRestoresLastActivePreset() throws Exception {
        File file = temporaryFolder.newFile("presets.json");
        assertTrue(file.delete());
        Store store = new Store();
        CounterClock clock = new CounterClock();
        PresetRepository repository = repository(file, store, clock);

        Preset saved = repository.save(PresetRepository.DEFAULT_ID, "My Piano", state(piano));
        assertEquals(saved.id, repository.activeId());
        Preset updated = repository.save(saved.id, "Piano and Strings",
                state(piano).assignLayer(strings));
        assertEquals(saved.id, updated.id);
        assertEquals("Piano and Strings", updated.name);
        assertEquals(PerformanceMode.DUAL, updated.state.mode());

        PresetRepository restored = repository(file, store, clock);
        assertEquals(saved.id, restored.activeId());
        assertEquals("Piano and Strings", restored.activePreset().name);
        assertEquals(PerformanceMode.DUAL, restored.activePreset().state.mode());
    }

    @Test
    public void duplicateAddsCopyPrefixAndDistinctIds() throws Exception {
        PresetRepository repository = freshRepository();
        Preset source = repository.save(PresetRepository.DEFAULT_ID, "Stage", state(piano));
        Preset first = repository.duplicate(source.id);
        Preset second = repository.duplicate(first.id);

        assertEquals("[C] Stage", first.name);
        assertEquals("[C] [C] Stage", second.name);
        assertNotEquals(source.id, first.id);
        assertNotEquals(first.id, second.id);
        assertEquals(second.id, repository.activeId());
    }

    @Test
    public void resetAndDeletingActivePresetFallBackToDefault() throws Exception {
        PresetRepository repository = freshRepository();
        Preset saved = repository.save(PresetRepository.DEFAULT_ID, "Bass", state(piano).assignLeft(bass));
        assertEquals(PerformanceMode.SPLIT, repository.activePreset().state.mode());

        repository.resetToDefault();
        assertEquals(PresetRepository.DEFAULT_ID, repository.activeId());
        assertEquals("Concert Piano", repository.activePreset().state.primaryTone.name);

        repository.activate(saved.id);
        repository.delete(saved.id);
        assertEquals(PresetRepository.DEFAULT_ID, repository.activeId());
        assertTrue(repository.all().isEmpty());
    }

    @Test
    public void malformedJsonAndMissingActiveIdFallBackToDefault() throws Exception {
        File file = temporaryFolder.newFile("presets.json");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write("not json".getBytes(StandardCharsets.UTF_8));
        }
        Store store = new Store();
        store.value = "missing";
        PresetRepository repository = repository(file, store, new CounterClock());
        assertTrue(repository.all().isEmpty());
        assertEquals(PresetRepository.DEFAULT_ID, repository.activeId());
        assertEquals(PerformanceMode.SINGLE, repository.activePreset().state.mode());
    }

    @Test
    public void duplicateVisibleNamesRemainSeparatePresets() throws Exception {
        PresetRepository repository = freshRepository();
        Preset source = repository.save(PresetRepository.DEFAULT_ID, "Same", state(piano));
        Preset one = repository.duplicate(source.id);
        repository.activate(source.id);
        Preset two = repository.duplicate(source.id);
        assertEquals(one.name, two.name);
        assertNotEquals(one.id, two.id);
        assertEquals(3, repository.all().size());
        assertFalse(one.createdAtMillis == two.createdAtMillis);
    }

    private PresetRepository freshRepository() throws Exception {
        File file = temporaryFolder.newFile();
        assertTrue(file.delete());
        return repository(file, new Store(), new CounterClock());
    }

    private PresetRepository repository(
            File file,
            PresetRepository.ActivePresetStore store,
            PresetRepository.Clock clock
    ) {
        return new PresetRepository(file, store, clock, tones);
    }

    private PerformanceState state(Tone primary) {
        return new PerformanceState(primary, null, null, 9, 54, 0, 0, 0, 3, 1, 0);
    }

    private Tone tone(int number, String name) {
        return new Tone(number, number == 1 ? "Piano" : "Other", name, 0, 68, number,
                Tone.Kind.INTERNAL, number == 1 ? 0 : 2, number - 1);
    }

    private static final class Store implements PresetRepository.ActivePresetStore {
        String value = PresetRepository.DEFAULT_ID;

        @Override
        public String get() {
            return value;
        }

        @Override
        public void set(String id) {
            value = id;
        }
    }

    private static final class CounterClock implements PresetRepository.Clock {
        long value = 1000;

        @Override
        public long nowMillis() {
            return value++;
        }
    }
}
