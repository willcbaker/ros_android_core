package com.wakebyte.rosollie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by lunabot on 2/17/15.
 */
public class RssiReceiver extends BroadcastReceiver{


    private final String TAG = RssiReceiver.class.getSimpleName();//"RSSI_Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive()" + action);

        if (RSSI_Service.ACTION_GATT_DISCONNECTED.equals(action)) {
            Log.d(TAG,"GATT Disconnected");
        } else if (RSSI_Service.ACTION_GATT_SERVICES_DISCOVERED
                .equals(action)) {
            Log.d(TAG,"GATT Services Discovered");

        } else if (RSSI_Service.ACTION_DATA_AVAILABLE.equals(action)) {
            Log.d(TAG,"DATA available");
            onData(intent);
        }else if (RSSI_Service.ACTION_GATT_RSSI.equals(action)) {
            Log.d(TAG,"RSSI available");
            onRSSI(intent);
        }

    }

    protected void onRSSI(Intent intent) {
        Log.w(TAG,"No override for onRSSI()");
    }

    protected void onData(Intent intent) {
        Log.w(TAG,"No override for onData()");
    }

}