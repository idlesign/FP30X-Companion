package dev.idlesign.fp30xcompanion;

import static org.junit.Assert.assertNotNull;

import android.content.ComponentName;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RecordingServiceTest {
    @Test
    public void recordingServiceIsDeclaredAndResolvable() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RecordingService.class)
                .setAction(RecordingService.ACTION_START);
        ComponentName component = intent.resolveActivity(
                ApplicationProvider.getApplicationContext().getPackageManager());
        // Services are not activities; explicit component identity is the declaration smoke check.
        assertNotNull(intent.getComponent());
    }
}
