package com.wakebyte.rosollie;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.ArrayList;
import java.util.Collections;

import std_msgs.Int32;


/**
 * Created by lunabot on 2/17/15.
 */
public class RSSI_Node extends AbstractNodeMain {MainActivity activity = null;

    private  long RATE_RSSI_REFRESH = 200;
    private  long RATE_WAIT_FOR_ROBOT = 800;


    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";
    public final static String RSSI_DATA = "RSSI_DATA";
    public final static String RSSI_DATA_MEDIAN = "RSSI_DATA_MEDIAN";
    private String _mBTaddress = null;// a string to hold the address

    public RSSI_Node(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ollie/rssi");
    }

    Publisher<std_msgs.Int32> pubRSSI;

    private final String TAG = "RSSI_NODE";

    private volatile boolean _connected = false;

    @Override
    public void onStart(final ConnectedNode connectedNode) {

        pubRSSI = connectedNode.newPublisher("rssi", Int32._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void setup() {
                //some setup stuff here
                initialize_BT();
                try{
                    while(!_connected){
                        _connected = connect(_mBTaddress);
                        Thread.sleep(RATE_WAIT_FOR_ROBOT);
                    }
                    while(activity._connectedRobot == null){
                        Thread.sleep(RATE_WAIT_FOR_ROBOT);
                    }
                    _connected = connect(activity._connectedRobot.getRobot().getIdentifier());
                }catch (InterruptedException e){
                    Log.w(TAG,"Unable to locate _connectedRobot.");
                }

            }

            @Override
            protected void loop() throws InterruptedException {
                if(_connected){
                    readRssi();
                }
                Thread.sleep(RATE_RSSI_REFRESH);
            }

        });
    }

    /////
    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;

    private boolean initialize_BT() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
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

    public void addConnection(String address){
        _mBTaddress = address;
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
    protected boolean connect(final String address) {
        if (address == null){
            return false;
        }
        Log.d(TAG, String.format("Request to connect to %s", address));
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
        mBluetoothGatt = device.connectGatt(activity, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;

        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        _connected = false;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * A function to read the RSSI from the BT device
     */
    public void readRssi() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "WBluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readRemoteRssi();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                /*intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.*/
                if(mBluetoothGatt != null)
                    Log.i(TAG, "Attempting to start service discovery:"
                            + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                //intentAction = ACTION_GATT_DISCONNECTED;
                //broadcastUpdate(intentAction);
            }
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, " BT_GATT_Callback onReadRemoteRssi()");
                broadcastRSSI(ACTION_GATT_RSSI, rssi);
                Int32 msg_rssi = pubRSSI.newMessage();
                msg_rssi.setData(rssi);
                pubRSSI.publish(msg_rssi);
            } else {
                Log.w(TAG, "onReadRemoteRssi received: " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered successful: " + status);
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                //startReadRSSI(RSSI_Service.RSSI_RATE.FIVE_HZ);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private ArrayList<Integer> rssiArray = new ArrayList<>();
    private boolean _calculateMedian = false;
    public void collectMedian(boolean enable) {
        _calculateMedian = enable;
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
        activity.sendBroadcast(intent);
    }
    public Integer getMedian(){
        return (median(rssiArray).intValue());
    }

    private Float median(ArrayList<Integer> array){
        Collections.sort(array);
        if (array.size() % 2 == 0)
            return ((float)(array.get(array.size() / 2) + array.get(array.size() / 2 - 1)))/2;
        else
            return ((float) array.get(array.size() / 2));
    }

    public void clearMedian(){
        rssiArray.clear();
    }
}
