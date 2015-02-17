package com.wakebyte.rosollie;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lunabot on 1/26/15.
 */
public class RSSI_Service extends Service{


    private static final int RSSI_MEDIAN_LENGTH = 80;

    public static enum RSSI_RATE{FOUR_HZ,FIVE_HZ,TEN_HZ,TWENTY_HZ};
    private static final long RSSI_DEFAULT_SLEEP_TIME = 250;//4Hz

    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private ArrayList<Integer> rssiArray;
    private float[] arrayRSSI = new float[RSSI_MEDIAN_LENGTH];

    private String TAG = RSSI_Service.class.getSimpleName();//"RSSI_Service";

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";
    public final static String RSSI_DATA = "RSSI_DATA";
    public final static String RSSI_DATA_MEDIAN = "RSSI_DATA_MEDIAN";
    private boolean _calculateMedian;

    private Timer _timer;


    public RSSI_Service(){
        rssiArray = new ArrayList<>();
        _calculateMedian=false;
        _timer = new Timer();
    }

    public void collectMedian(boolean enable) {
        _calculateMedian = enable;
    }
    public void startReadRSSI(RSSI_RATE rate){
        long delay = RSSI_DEFAULT_SLEEP_TIME;
        switch(rate){
            case FOUR_HZ:
                delay = 250;
                break;
            case FIVE_HZ:
                delay = 200;
                break;
            case TEN_HZ:
                delay = 100;
                break;
            case TWENTY_HZ:
                delay = 50;
                break;
        }
        Log.i(TAG,"Starting ReadRSSI() at "+rate.toString());
        _timer.scheduleAtFixedRate(new TimerTask() {
            synchronized public void run() {
                Log.w(TAG, "scheduled RSSI");
                readRssi();
            }
        }, delay, delay);
    }


    public class LocalBinder extends Binder {
        RSSI_Service getService() {
            return RSSI_Service.this;
        }
    }
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        Log.d(TAG,"initialize()  Initialized Bluetooth Manager and Adapter");

        return true;
    }
    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        _timer.cancel();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public void readRssi() {
        Log.d(TAG,"Service requested to read remote rssi()");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "WBluetoothAdapter not initialized");
            return;
        }
        try {
            mBluetoothGatt.readRemoteRssi();
        }catch (Exception e){
            //Todo: fix this.
            e.printStackTrace();
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                //todo: was throwing a null excpetion
                // Attempts to discover services after successful connection.
                if(mBluetoothGatt != null)
                    Log.i(TAG, "Attempting to start service discovery:"
                            + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, " BT_GATT_Callback onReadRemoteRssi()");
                broadcastRSSI(ACTION_GATT_RSSI, rssi);
            } else {
                Log.w(TAG, "onReadRemoteRssi received: " + status);
            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered successful: " + status);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                startReadRSSI(RSSI_Service.RSSI_RATE.FIVE_HZ);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastRSSI(final String action, int rssi) {
        Log.d(TAG,"broadcastRSSI()");
        final Intent intent = new Intent(action);
        intent.putExtra(RSSI_DATA, rssi);
        /*for (int i = 0; i < RSSI_MEDIAN_LENGTH-1; i++) {
            arrayRSSI[i]=arrayRSSI[i+1];
        }
        arrayRSSI[RSSI_MEDIAN_LENGTH-1]=rssi;*/
        if (_calculateMedian) {
            rssiArray.add(rssi);
            intent.putExtra(RSSI_DATA_MEDIAN, getMedian().intValue());
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        //Todo: Change characteristic ID or REMOVE
        //if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid())) {
            final byte[] rx = characteristic.getValue();
            intent.putExtra(EXTRA_DATA, rx);
       //}

        sendBroadcast(intent);
    }

    public ArrayList<Integer> getRSSIArray() {        return rssiArray;    }

    public Integer getMedian(){
        return (median(arrayRSSI).intValue());
    }

    private Float median(float[] array){
        Collections.sort(rssiArray);
        if (rssiArray.size() % 2 == 0)
            return ((float)(rssiArray.get(rssiArray.size()/2) + rssiArray.get(rssiArray.size()/2 - 1)))/2;
        else
            return ((float)rssiArray.get(rssiArray.size()/2));
        /*
        Arrays.sort(array.clone());
        if (array.length % 2 == 0)
            return (array[array.length/2] + array[array.length/2 - 1])/2;
        else
            return array[array.length/2];
        */
    }

    public void clearMedian(){
        rssiArray.clear();
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address
     *            The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        Log.d(TAG,String.format("Request to connect to %s",address));
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG,
                    "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;

        return true;
    }

    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RSSI_Service.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RSSI_Service.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RSSI_Service.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RSSI_Service.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RSSI_Service.ACTION_GATT_RSSI);

        return intentFilter;
    }
}
