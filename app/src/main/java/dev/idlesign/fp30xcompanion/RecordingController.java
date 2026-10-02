package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;

public final class RecordingController {
    public interface Listener {
        void onRecordingStarted(String summary);

        void onRecordingProgress(long elapsedMillis, float level);

        void onRecordingStopped(List<String> files);

        void onRecordingError(String message);
    }

    private final Context context;
    private final MidiConnectionManager connection;
    private final RecordingFileStore fileStore;
    private final UsbAudioDeviceFinder audioFinder;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecordingSession session;
    private long startedAt;
    private float level;

    public RecordingController(
            Context context,
            MidiConnectionManager connection,
            Listener listener
    ) {
        this.context = context.getApplicationContext();
        this.connection = connection;
        this.listener = listener;
        fileStore = new RecordingFileStore(context);
        audioFinder = new UsbAudioDeviceFinder(context);
    }

    public UsbAudioCapabilities audioCapabilities() {
        return audioFinder.findFp30xInput();
    }

    public boolean isRecording() {
        return session != null;
    }

    public void start(boolean audio, boolean midi, RecordingDestination destination) {
        if (session != null) {
            return;
        }
        try {
            session = new RecordingSession(
                    context,
                    connection,
                    fileStore,
                    destination,
                    audio,
                    midi,
                    new RecordingNames().newStem(),
                    audio ? audioCapabilities() : null,
                    new UsbAudioRecorder.Listener() {
                        @Override
                        public void onLevel(float value) {
                            level = value;
                        }

                        @Override
                        public void onError(String message) {
                            handler.post(() -> fail(message));
                        }
                    });
            session.start();
            startedAt = System.currentTimeMillis();
            RecordingService.setStopListener(this::stop);
            Intent service = new Intent(context, RecordingService.class)
                    .setAction(RecordingService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
            listener.onRecordingStarted(summary(audio, midi));
            handler.post(tick);
        } catch (IOException | RuntimeException error) {
            if (session != null) {
                session.abort();
            }
            session = null;
            RecordingService.setStopListener(null);
            context.stopService(new Intent(context, RecordingService.class));
            listener.onRecordingError(error.getMessage());
        }
    }

    public void stop() {
        RecordingSession active = session;
        if (active == null) {
            return;
        }
        session = null;
        handler.removeCallbacks(tick);
        try {
            List<String> files = active.stop();
            listener.onRecordingStopped(files);
        } catch (IOException error) {
            listener.onRecordingError(error.getMessage());
        } finally {
            RecordingService.setStopListener(null);
            context.stopService(new Intent(context, RecordingService.class));
        }
    }

    private void fail(String message) {
        RecordingSession active = session;
        if (active == null) {
            return;
        }
        session = null;
        handler.removeCallbacks(tick);
        try {
            List<String> files = active.stop();
            if (!files.isEmpty()) {
                message = context.getString(
                        R.string.recording_error_after_save,
                        message,
                        String.join(", ", files));
            }
        } catch (IOException finalizeError) {
            message = message + "; " + finalizeError.getMessage();
        } finally {
            RecordingService.setStopListener(null);
            context.stopService(new Intent(context, RecordingService.class));
        }
        listener.onRecordingError(message);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (session == null) {
                return;
            }
            listener.onRecordingProgress(System.currentTimeMillis() - startedAt, level);
            handler.postDelayed(this, 250);
        }
    };

    private String summary(boolean audio, boolean midi) {
        if (audio && midi) {
            return context.getString(R.string.recording_summary_audio_midi);
        }
        return context.getString(audio
                ? R.string.recording_summary_audio : R.string.recording_summary_midi);
    }
}
