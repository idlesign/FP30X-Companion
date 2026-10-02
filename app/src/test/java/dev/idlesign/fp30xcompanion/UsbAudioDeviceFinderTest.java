package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.media.AudioDeviceInfo;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UsbAudioDeviceFinderTest {
    @Test
    public void prefersNamedRolandDeviceOverGenericUsbInput() {
        UsbAudioCapabilities result = UsbAudioDeviceFinder.choose(Arrays.asList(
                descriptor(1, AudioDeviceInfo.TYPE_USB_DEVICE, "Generic USB", new int[]{44100}, new int[]{1}),
                descriptor(2, AudioDeviceInfo.TYPE_USB_DEVICE, "Roland FP-30X", new int[]{44100, 48000}, new int[]{1, 2})
        ));
        assertEquals(2, result.deviceId);
        assertEquals("Roland FP-30X", result.productName);
        assertEquals(48000, result.sampleRate);
        assertEquals(2, result.channelCount);
    }

    @Test
    public void fallsBackToUsbAndRejectsBuiltInMicrophone() {
        UsbAudioCapabilities fallback = UsbAudioDeviceFinder.choose(List.of(
                descriptor(7, AudioDeviceInfo.TYPE_USB_HEADSET, "USB Audio", new int[0], new int[0])));
        assertEquals(7, fallback.deviceId);
        assertEquals(44100, fallback.sampleRate);
        assertEquals(1, fallback.channelCount);
        assertNull(UsbAudioDeviceFinder.choose(List.of(
                descriptor(8, AudioDeviceInfo.TYPE_BUILTIN_MIC, "Phone", new int[]{48000}, new int[]{1}))));
    }

    private UsbAudioDeviceFinder.Descriptor descriptor(
            int id, int type, String name, int[] rates, int[] channels) {
        return new UsbAudioDeviceFinder.Descriptor() {
            @Override
            public int id() {
                return id;
            }

            @Override
            public int type() {
                return type;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public int[] sampleRates() {
                return rates;
            }

            @Override
            public int[] channelCounts() {
                return channels;
            }

            @Override
            public AudioDeviceInfo platformDevice() {
                return null;
            }
        };
    }
}
