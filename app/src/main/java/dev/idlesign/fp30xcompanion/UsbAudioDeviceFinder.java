package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UsbAudioDeviceFinder {
    interface Descriptor {
        int id();

        int type();

        String name();

        int[] sampleRates();

        int[] channelCounts();

        AudioDeviceInfo platformDevice();
    }

    private final AudioManager audioManager;

    public UsbAudioDeviceFinder(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public UsbAudioCapabilities findFp30xInput() {
        if (audioManager == null) {
            return null;
        }
        ArrayList<Descriptor> descriptors = new ArrayList<>();
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            descriptors.add(new PlatformDescriptor(device));
        }
        return choose(descriptors);
    }

    static UsbAudioCapabilities choose(List<Descriptor> devices) {
        Descriptor fallback = null;
        for (Descriptor device : devices) {
            if (!isUsb(device.type())) {
                continue;
            }
            if (fallback == null) {
                fallback = device;
            }
            String normalized = device.name().toLowerCase(Locale.ROOT);
            if (normalized.contains("fp-30x") || normalized.contains("fp30x")
                    || normalized.contains("roland")) {
                return capabilities(device);
            }
        }
        return fallback == null ? null : capabilities(fallback);
    }

    private static UsbAudioCapabilities capabilities(Descriptor device) {
        int rate = preferred(device.sampleRates(), 48_000, 44_100);
        int channels = preferred(device.channelCounts(), 2, 1);
        return new UsbAudioCapabilities(
                device.platformDevice(),
                device.id(),
                device.name(),
                rate,
                channels,
                AudioFormat.ENCODING_PCM_16BIT
        );
    }

    private static int preferred(int[] values, int preferred, int fallback) {
        if (values != null) {
            for (int value : values) {
                if (value == preferred) {
                    return preferred;
                }
            }
            for (int value : values) {
                if (value == fallback) {
                    return fallback;
                }
            }
            if (values.length > 0 && values[0] > 0) {
                return values[0];
            }
        }
        return fallback;
    }

    private static boolean isUsb(int type) {
        return type == AudioDeviceInfo.TYPE_USB_DEVICE
                || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                || type == AudioDeviceInfo.TYPE_USB_HEADSET;
    }

    private record PlatformDescriptor(AudioDeviceInfo device) implements Descriptor {
        @Override
        public int id() {
            return device.getId();
        }

        @Override
        public int type() {
            return device.getType();
        }

        @Override
        public String name() {
            CharSequence product = device.getProductName();
            return product == null ? "USB Audio" : product.toString();
        }

        @Override
        public int[] sampleRates() {
            return device.getSampleRates();
        }

        @Override
        public int[] channelCounts() {
            return device.getChannelCounts();
        }

        @Override
        public AudioDeviceInfo platformDevice() {
            return device;
        }
    }
}
