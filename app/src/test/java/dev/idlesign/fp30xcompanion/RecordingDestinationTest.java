package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RecordingDestinationTest {
    @Test
    public void downloadsIsThePermissionFreeDefaultDestination() {
        RecordingDestination destination = RecordingDestination.downloads();
        assertEquals(RecordingDestination.Kind.DOWNLOADS, destination.kind);
        assertNull(destination.treeUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void documentTreeRequiresUri() {
        RecordingDestination.tree(null);
    }
}
