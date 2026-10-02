package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PerformanceControllerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Tone piano = tone(1, "Concert Piano");
    private final Tone strings = tone(2, "Epic Strings");
    private final List<Tone> tones = Arrays.asList(piano, strings);

    @Test
    public void offlineChangesStayDirtyWithoutSending() throws Exception {
        Fixture fixture = fixture(false);
        fixture.controller.assignLayer(strings);
        assertEquals(PerformanceMode.DUAL, fixture.controller.state().mode());
        assertTrue(fixture.controller.isDirty());
        assertEquals(0, fixture.output.applyCount);
    }

    @Test
    public void connectedChangesApplyImmediately() throws Exception {
        Fixture fixture = fixture(true);
        fixture.controller.assignLayer(strings);
        assertEquals(1, fixture.output.applyCount);
        assertEquals(PerformanceMode.DUAL, fixture.output.last.mode());
    }

    @Test
    public void connectionReadyAppliesOfflineChanges() throws Exception {
        Fixture fixture = fixture(false);
        fixture.controller.assignLayer(strings);
        Preset saved = fixture.controller.save("Piano Strings");
        fixture.controller.assignSingle(piano);
        fixture.output.connected = true;

        fixture.controller.connectionReady();

        assertEquals(1, fixture.output.applyCount);
        assertEquals(saved.id, fixture.controller.activePreset().id);
        assertEquals(PerformanceMode.SINGLE, fixture.controller.state().mode());
        assertTrue(fixture.controller.isDirty());
        assertSame(fixture.controller.state(), fixture.output.last);
    }

    private Fixture fixture(boolean connected) throws Exception {
        File file = temporaryFolder.newFile();
        assertTrue(file.delete());
        Store store = new Store();
        PresetRepository repository = new PresetRepository(file, store, System::currentTimeMillis, tones);
        FakeOutput output = new FakeOutput();
        output.connected = connected;
        FakeListener listener = new FakeListener();
        return new Fixture(output, new PerformanceController(repository, output, listener));
    }

    private Tone tone(int number, String name) {
        return new Tone(number, number == 1 ? "Piano" : "Other", name, 0, 68, number,
                Tone.Kind.INTERNAL, number == 1 ? 0 : 2, number - 1);
    }

    private record Fixture(FakeOutput output, PerformanceController controller) {
    }

    private static final class FakeOutput implements PerformanceOutput {
        boolean connected;
        int applyCount;
        PerformanceState last;

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void applyPerformanceState(PerformanceState state) {
            applyCount++;
            last = state;
        }
    }

    private static final class FakeListener implements PerformanceController.Listener {
        @Override
        public void onPerformanceChanged(PerformanceState state, Preset activePreset, boolean dirty) {
        }

        @Override
        public void onPerformanceMessage(String message) {
        }

        @Override
        public void onPerformanceError(String message) {
        }
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
}
