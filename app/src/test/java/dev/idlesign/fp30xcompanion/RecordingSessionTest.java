package dev.idlesign.fp30xcompanion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class RecordingSessionTest {
    @Test
    public void plansAudioOnlyOutput() {
        assertEquals(Collections.singletonList("fp30x_20261001_120000.wav"),
                RecordingSession.outputNames("fp30x_20261001_120000", true, false));
    }

    @Test
    public void plansMidiOnlyOutput() {
        assertEquals(Collections.singletonList("fp30x_20261001_120000.mid"),
                RecordingSession.outputNames("fp30x_20261001_120000", false, true));
    }

    @Test
    public void simultaneousOutputsShareOneStem() {
        assertEquals(Arrays.asList(
                        "fp30x_20261001_120000.mid",
                        "fp30x_20261001_120000.wav"),
                RecordingSession.outputNames("fp30x_20261001_120000", true, true));
    }
}
