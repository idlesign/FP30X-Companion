package dev.idlesign.fp30xcompanion;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public final class RecordViewTest {
    @Test
    public void constructsForDisconnectedAndUsbCapableStates() {
        Context context = ApplicationProvider.getApplicationContext();
        RecordView view = new RecordView(context, new RecordView.Listener() {
            @Override public void onStart(boolean audio, boolean midi) { }
            @Override public void onStop() { }
            @Override public void onChangeDestination() { }
        });
        view.setCapabilities(false, false, false);
        view.setCapabilities(true, true, true);
        assertNotNull(view);
    }
}
