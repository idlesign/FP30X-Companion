package dev.idlesign.fp30xcompanion;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UsbAudioRecorder {
    public interface Listener {
        void onLevel(float level);

        void onError(String message);
    }

    private final UsbAudioCapabilities capabilities;
    private final Listener listener;
    private final AtomicBoolean running = new AtomicBoolean();
    private AudioRecord audioRecord;
    private WavWriter writer;
    private Thread thread;

    public UsbAudioRecorder(UsbAudioCapabilities capabilities, Listener listener) {
        this.capabilities = capabilities;
        this.listener = listener;
    }

    @SuppressLint("MissingPermission")
    public void start(File target) throws IOException {
        int minimum = AudioRecord.getMinBufferSize(
                capabilities.sampleRate,
                capabilities.channelMask(),
                capabilities.encoding);
        if (minimum <= 0) {
            throw new IOException("Unsupported USB audio format");
        }
        int source = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? MediaRecorder.AudioSource.UNPROCESSED : MediaRecorder.AudioSource.DEFAULT;
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(capabilities.sampleRate)
                .setChannelMask(capabilities.channelMask())
                .setEncoding(capabilities.encoding)
                .build();
        audioRecord = new AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minimum * 2)
                .build();
        if (capabilities.device != null && !audioRecord.setPreferredDevice(capabilities.device)) {
            audioRecord.release();
            audioRecord = null;
            throw new IOException("Android rejected the FP-30X USB audio input");
        }
        writer = new WavWriter(target, capabilities.sampleRate, capabilities.channelCount, 16);
        audioRecord.startRecording();
        if (capabilities.device != null && audioRecord.getRoutedDevice() != null
                && audioRecord.getRoutedDevice().getId() != capabilities.deviceId) {
            stop();
            throw new IOException("Audio was routed to a different input device");
        }
        running.set(true);
        thread = new Thread(() -> capture(minimum * 2), "fp30x-usb-audio");
        thread.start();
    }

    public void stop() throws IOException {
        running.set(false);
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {
            }
        }
        if (thread != null && Thread.currentThread() != thread) {
            try {
                thread.join(2000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    private void capture(int bufferSize) {
        byte[] buffer = new byte[bufferSize];
        try {
            while (running.get()) {
                int read = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                if (read < 0) {
                    throw new IOException("USB audio read failed: " + read);
                }
                if (read > 0) {
                    writer.write(buffer, 0, read);
                    listener.onLevel(peak(buffer, read));
                }
            }
        } catch (IOException error) {
            running.set(false);
            listener.onError(error.getMessage());
        }
    }

    private float peak(byte[] data, int count) {
        int peak = 0;
        for (int index = 0; index + 1 < count; index += 2) {
            int sample = (short) ((data[index] & 0xFF) | (data[index + 1] << 8));
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak / 32768f;
    }
}
