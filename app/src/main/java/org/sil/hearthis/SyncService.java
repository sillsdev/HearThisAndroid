package org.sil.hearthis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

// Service that runs a simple 'web server' that HearThis desktop can talk to.
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final String CHANNEL_ID = "SyncServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private static SyncService sInstance;

    public SyncService() {
    }

    private SyncServer _server;

    public static SyncService getInstance() {
        return sInstance;
    }

    public SyncServer getServer() {
        return _server;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        createNotificationChannel();
        _server = new SyncServer(this);
    }

    @Override
    public void onDestroy() {
        if (_server != null) {
            _server.stopThread();
            _server = null;
        }
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, SyncActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.sync_title))
                .setContentText(getString(R.string.sync_message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
        }

        if (_server != null) {
            _server.startThread();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (fgsType == ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
                Log.w(TAG, "SyncService timed out (6 hour limit reached). Cleaning up...");
                stopSelf();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.sync_service_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
