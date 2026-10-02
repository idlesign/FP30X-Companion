package dev.idlesign.fp30xcompanion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class WavWriter implements AutoCloseable {
    private final File file;
    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;
    private final FileOutputStream output;
    private long pcmBytes;
    private boolean closed;

    public WavWriter(File file, int sampleRate, int channels, int bitsPerSample) throws IOException {
        this.file = file;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        output = new FileOutputStream(file);
        output.write(new byte[44]);
    }

    public synchronized void write(byte[] data, int offset, int count) throws IOException {
        if (closed) {
            throw new IOException("WAV writer is closed");
        }
        output.write(data, offset, count);
        pcmBytes += count;
    }

    public synchronized long pcmBytes() {
        return pcmBytes;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        output.getFD().sync();
        output.close();
        try (RandomAccessFile header = new RandomAccessFile(file, "rw")) {
            header.writeBytes("RIFF");
            writeLittleInt(header, 36 + pcmBytes);
            header.writeBytes("WAVEfmt ");
            writeLittleInt(header, 16);
            writeLittleShort(header, 1);
            writeLittleShort(header, channels);
            writeLittleInt(header, sampleRate);
            int blockAlign = channels * bitsPerSample / 8;
            writeLittleInt(header, (long) sampleRate * blockAlign);
            writeLittleShort(header, blockAlign);
            writeLittleShort(header, bitsPerSample);
            header.writeBytes("data");
            writeLittleInt(header, pcmBytes);
        }
    }

    private static void writeLittleInt(RandomAccessFile file, long value) throws IOException {
        file.write((int) value & 0xFF);
        file.write((int) (value >> 8) & 0xFF);
        file.write((int) (value >> 16) & 0xFF);
        file.write((int) (value >> 24) & 0xFF);
    }

    private static void writeLittleShort(RandomAccessFile file, int value) throws IOException {
        file.write(value & 0xFF);
        file.write((value >> 8) & 0xFF);
    }
}
