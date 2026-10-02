package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ToneCatalogTest {
    @Test(expected = IOException.class)
    public void catalogMustContainAll321Tones() throws IOException {
        String incomplete = "number,category,name,bank_msb,bank_lsb,program,kind,sysex_category,sysex_number\n"
                + "1,Piano,Concert Piano,0,68,1,INTERNAL,0,0\n";
        ToneCatalog.parse(new ByteArrayInputStream(incomplete.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void findsTonesByStableIdAndName() {
        Tone piano = new Tone(1, "Piano", "Concert Piano", 0, 68, 1,
                Tone.Kind.INTERNAL, 0, 0);
        Tone strings = new Tone(2, "Other", "Epic Strings", 0, 68, 2,
                Tone.Kind.INTERNAL, 2, 0);
        assertSame(piano, ToneCatalog.findByStableId(Arrays.asList(piano, strings), piano.stableId()));
        assertSame(strings, ToneCatalog.findByName(Arrays.asList(piano, strings), "Epic Strings"));
        assertNull(ToneCatalog.findByStableId(Arrays.asList(piano), "missing"));
        assertNull(ToneCatalog.findByName(Arrays.asList(piano), "Missing"));
    }

    @Test
    public void stableIdContainsBankAndName() {
        Tone tone = new Tone(1, "Piano", "Concert Piano", 0, 68, 1,
                Tone.Kind.INTERNAL, 0, 0);
        assertEquals("0:68:1:Concert Piano", tone.stableId());
    }
}
