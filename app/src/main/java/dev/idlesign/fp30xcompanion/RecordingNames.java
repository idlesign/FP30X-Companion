package dev.idlesign.fp30xcompanion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class RecordingNames {
    public interface Clock {
        long nowMillis();
    }

    private final Clock clock;

    public RecordingNames() {
        this(System::currentTimeMillis);
    }

    RecordingNames(Clock clock) {
        this.clock = clock;
    }

    public String newStem() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
        return "fp30x_" + format.format(new Date(clock.nowMillis()));
    }
}
