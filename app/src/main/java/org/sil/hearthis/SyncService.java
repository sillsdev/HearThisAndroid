package org.sil.hearthis;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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
        super.onCreate();

        _server = new SyncServer(this);
    }

    @Override
    public void onDestroy() {
        _server.stopThread();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        _server.startThread();
        return START_STICKY;
    }
}
