package dev.idlesign.fp30xcompanion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public final class RecordingService extends Service {
    public interface StopListener {
        void onStopRequested();
    }

    public static final String ACTION_START = "dev.idlesign.fp30xcompanion.START_RECORDING";
    public static final String ACTION_STOP = "dev.idlesign.fp30xcompanion.STOP_RECORDING";
    private static final String CHANNEL_ID = "fp30x_recording";
    private static final int NOTIFICATION_ID = 30;
    private static volatile StopListener stopListener;

    public static void setStopListener(StopListener listener) {
        stopListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.recording_notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            StopListener listener = stopListener;
            if (listener != null) {
                listener.onStopRequested();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, notification());
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification notification() {
        Intent stop = new Intent(this, RecordingService.class).setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(
                this,
                1,
                stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.presence_audio_online)
                .setContentTitle(getString(R.string.recording_notification_title))
                .setContentText(getString(R.string.recording_notification_text))
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        null,
                        getString(R.string.button_stop),
                        stopAction).build())
                .build();
    }
}
