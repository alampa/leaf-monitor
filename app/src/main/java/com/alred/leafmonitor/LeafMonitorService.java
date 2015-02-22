package com.alred.leafmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.SaveCallback;

public class LeafMonitorService extends Service {

    final String TAG = "LeafMonitorService";

    public LeafMonitorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, getResources().getText(R.string.parse_app_id).toString(), getResources().getText(R.string.parse_app_key).toString());

        ParsePush.subscribeInBackground("alert-leaf-monitor-start", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "successfully subscribed to alert-leaf-monitor-start.");
                } else {
                    Log.e(TAG, "failed to subscribe for push", e);
                }
            }
        });


        ParsePush.subscribeInBackground("alert-leaf-monitor-end", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d(TAG, "successfully subscribed to alert-leaf-monitor-end.");
                } else {
                    Log.e(TAG, "failed to subscribe for push", e);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
