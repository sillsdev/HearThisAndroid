package org.sil.hearthis;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

// Service that runs a simple 'web server' that HearThis desktop can talk to.
public class SyncService extends Service {
    public SyncService() {
    }

    SyncServer _server;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("Sync", "SyncService, calling super.onCreate()");
        super.onCreate();

        Log.d("Sync", "SyncService, _server = new SyncServer");
        _server = new SyncServer(this);
    }

    @Override
    public void onDestroy() {
        Log.d("Sync", "SyncService, calling _server.stopThread()");
        _server.stopThread();
        Log.d("Sync", "SyncService, calling super.onDestroy()");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("Sync", "SyncService, calling _server.startThread()");
        _server.startThread();
        return START_STICKY;
    }
}
