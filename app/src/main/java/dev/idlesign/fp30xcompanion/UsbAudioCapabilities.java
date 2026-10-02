package dev.idlesign.fp30xcompanion;

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;

public final class UsbAudioCapabilities {
    public final AudioDeviceInfo device;
    public final int deviceId;
    public final String productName;
    public final int sampleRate;
    public final int channelCount;
    public final int encoding;

    public UsbAudioCapabilities(
            AudioDeviceInfo device,
            int deviceId,
            String productName,
            int sampleRate,
            int channelCount,
            int encoding
    ) {
        this.device = device;
        this.deviceId = deviceId;
        this.productName = productName;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.encoding = encoding;
    }

    public int channelMask() {
        return channelCount >= 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
    }
}
