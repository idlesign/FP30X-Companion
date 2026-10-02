package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PresetWorkflowTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final List<Tone> tones = Arrays.asList(piano, strings);

    @Test
    public void saveUpdatesNameAndStateAndClearsDirtyFlag() throws Exception {
        Fixture fixture = fixture();
        fixture.controller.assignLayer(strings);
        assertTrue(fixture.controller.isDirty());
        Preset saved = fixture.controller.save("Piano + Strings");
        assertEquals("Piano + Strings", saved.name);
        assertEquals(PerformanceMode.DUAL, saved.state.mode());
        assertFalse(fixture.controller.isDirty());

        fixture.controller.assignSingle(strings);
        Preset updated = fixture.controller.save("Strings");
        assertEquals(saved.id, updated.id);
        assertEquals("Strings", updated.name);
        assertEquals("Epic Strings", updated.state.primaryTone.name);
    }

    @Test
    public void duplicateBecomesActiveAndDeleteFallsBackToDefault() throws Exception {
        Fixture fixture = fixture();
        Preset saved = fixture.controller.save("Stage");
        Preset copy = fixture.controller.duplicate();
        assertEquals("[C] Stage", copy.name);
        assertNotEquals(saved.id, copy.id);
        assertEquals(copy.id, fixture.controller.activePreset().id);

        fixture.controller.delete(copy.id);
        assertEquals(PresetRepository.DEFAULT_ID, fixture.controller.activePreset().id);
        assertEquals(1, fixture.controller.presets().size());
    }

    @Test
    public void resetCanBeUndoneWithoutChangingSavedPreset() throws Exception {
        Fixture fixture = fixture();
        fixture.controller.assignLayer(strings);
        Preset saved = fixture.controller.save("Dual");
        fixture.controller.assignSingle(strings);
        assertTrue(fixture.controller.isDirty());

        fixture.controller.resetToDefault();
        assertEquals(PresetRepository.DEFAULT_ID, fixture.controller.activePreset().id);
        assertEquals("Concert Piano", fixture.controller.state().primaryTone.name);

        assertTrue(fixture.controller.undoReset());
        assertEquals(saved.id, fixture.controller.activePreset().id);
        assertEquals("Epic Strings", fixture.controller.state().primaryTone.name);
        assertTrue(fixture.controller.isDirty());
        assertFalse(fixture.controller.undoReset());
    }

    @Test
    public void reconnectAppliesLastLoadedSavedPreset() throws Exception {
        Fixture fixture = fixture();
        fixture.controller.assignLayer(strings);
        Preset saved = fixture.controller.save("Dual");
        fixture.controller.resetToDefault();
        fixture.controller.load(saved.id);
        fixture.output.connected = true;
        fixture.controller.connectionReady();
        assertEquals(saved.id, fixture.controller.activePreset().id);
        assertEquals(PerformanceMode.DUAL, fixture.output.last.mode());
    }

    private Fixture fixture() throws Exception {
        File file = temporaryFolder.newFile();
        assertTrue(file.delete());
        Store store = new Store();
        PresetRepository repository = new PresetRepository(file, store, System::currentTimeMillis, tones);
        FakeOutput output = new FakeOutput();
        PerformanceController controller = new PerformanceController(repository, output, new Listener());
        return new Fixture(controller, output);
    }

    private Tone tone(int number, String name) {
        return new Tone(number, number == 1 ? "Piano" : "Other", name, 0, 68, number,
                Tone.Kind.INTERNAL, number == 1 ? 0 : 2, number - 1);
    }

    private record Fixture(PerformanceController controller, FakeOutput output) {
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

    private static final class FakeOutput implements PerformanceOutput {
        boolean connected;
        PerformanceState last;

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void applyPerformanceState(PerformanceState state) {
            last = state;
        }
    }

    private static final class Listener implements PerformanceController.Listener {
        @Override
        public void onPerformanceChanged(PerformanceState state, Preset activePreset, boolean dirty) {
        }

        @Override
        public void onPerformanceMessage(String message) {
        }

        @Override
        public void onPerformanceError(String message) {
            throw new AssertionError(message);
        }
    }
}
