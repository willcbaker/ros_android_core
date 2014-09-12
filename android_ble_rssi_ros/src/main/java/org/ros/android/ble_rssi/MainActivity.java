/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.ble_rssi;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MainActivity extends RosActivity {

    private final static String TAG = "ROS.BLE.MainActivity";

    private static final int CONNECTED_COMMAND = 1;
    private static final int DISCONNECTED_COMMAND = 2;
    private static final int START_COMMAND = 3;
    private static final int PAUSE_COMMAND = 4;
    private static final int STOP_COMMAND = 5;
    public static final int RSSI_SLEEP_TIME = 250;

    private Talker talker;
    Toast t;

    private Button spinBtn, stopBtn;
    private Button connectBtn = null;
    private ToggleButton startBtn = null;
    private ToggleButton pauseBtn = null;
    private Button setBtn = null;
    private NumberPicker distancePick = null;
    private NumberPicker distanceDecPick = null;
    private TextView rssiValue = null;
    private SeekBar seekBar, seekBar1, seekBar2;

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;
    private String mDeviceName;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;

    private byte[] data = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    private float distance = 0;

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };


    final static byte LED_CONTROL = 0x01;
    final static byte MOTOR_CONTROL = 0x02;

    final static byte MOTOR_STOP = 0x00;
    final static byte MOTOR_L_FORWARD = 0x01;
    final static byte MOTOR_L_BACK = 0x02;
    final static byte MOTOR_R_FORWARD = 0x04;
    final static byte MOTOR_R_BACK = 0x08;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                talker.sendCmnd(DISCONNECTED_COMMAND);
                rssiValue.setText("---");
                t=Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT);t.show();
                        flag = false;
                //setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                //sendOrientation();
                talker.sendCmnd(CONNECTED_COMMAND);
                t=Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT);t.show();
                        flag = true;

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);
                parseData(data);
                //readAnalogInValue(data);
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                String rssiInfo = intent.getStringExtra(RBLService.EXTRA_DATA);
                displayData(rssiInfo);
                int rssi = Integer.parseInt(rssiInfo);
                if (rssi > 0){
                    talker.sendString(String.format("WARN: received RSSI > 0: (%s)",rssiInfo));
                }
                else {
                    talker.sendRSSI(rssi);
                }
            }
        }
    };

    private void parseData(byte[] data) {
        Log.d("BleReceived",String.format("data[%d]",data.length));
    }

    public MainActivity() {
    // The RosActivity constructor configures the notification title and ticker
    // messages.
    super("ROSSI BLE", "BLE RSSI Publisher");
  }
/*
  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.second);
    rosTextView = (RosTextView<std_msgs.String>) findViewById(R.id.text);
    rosTextView.setTopicName("chatter");
    rosTextView.setMessageType(std_msgs.String._TYPE);
    rosTextView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
      @Override
      public String call(std_msgs.String message) {
        return message.getData();
      }
    });

  }*/

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    talker = new Talker(this);

      NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress().toString());
      // At this point, the user has already been prompted to either enter the URI
      // of a master to use or to start a master locally.
      nodeConfiguration.setMasterUri(getMasterUri());
      nodeMainExecutor.execute(talker, nodeConfiguration);
  }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

 //       requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.second);
  //      getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
        rssiValue = (TextView) findViewById(R.id.rssiValue);


        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar1.setEnabled(true);
        seekBar1.setMax(255);
        seekBar1.setProgress(30);

        spinBtn = (Button) findViewById(R.id.spin);
        spinBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                byte[] buf = new byte[]{ MOTOR_CONTROL, (byte) 0x00, (byte) 0x00};

                buf[1] = (byte) (MOTOR_R_FORWARD | MOTOR_L_BACK );
                buf[2] = (byte) seekBar1.getProgress();
                Log.d("BLE_RFCOMM",String.format("Send(%d,%d,%d)",buf[0],buf[1],buf[2]));

                if (!bleSend(buf)) {
                    warnNotConnected();
                }
            }
        });
        stopBtn = (Button) findViewById(R.id.stop);
        stopBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                byte[] buf = new byte[]{ MOTOR_CONTROL, (byte) 0x00, (byte) 0x00};

                buf[1] = MOTOR_STOP;

                if (!bleSend(buf)) {
                    warnNotConnected();
                }
            }
        });

        connectBtn = (Button) findViewById(R.id.connect);
        connectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (scanFlag == false) {
                    scanLeDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                scanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        t = Toast
                                                .makeText(
                                                        MainActivity.this,
                                                        "Couldn't find BLE Shield device!",
                                                        Toast.LENGTH_SHORT);
                                        t.setGravity(0, 0, Gravity.CENTER);
                                        t.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(connState);
                if (connState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    //setButtonDisable();
                }
            }
        });

        pauseBtn = (ToggleButton) findViewById(R.id.pause);
        pauseBtn.setClickable(false);
        pauseBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    talker.sendCmnd(PAUSE_COMMAND);
                } else {
                    talker.sendOrientation(seekBar.getProgress());
                    talker.sendDistance(distance);
                    talker.sendCmnd(START_COMMAND);
                }
            }
        });

        startBtn = (ToggleButton) findViewById(R.id.start);
        startBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    talker.sendCmnd(START_COMMAND);
                    talker.sendOrientation(seekBar.getProgress());
                    talker.sendDistance(distance);
                    pauseBtn.setClickable(true);
                } else {
                    talker.sendCmnd(STOP_COMMAND);
                    pauseBtn.setChecked(false);
                    pauseBtn.setClickable(false);
                }
            }
        });

        distancePick = (NumberPicker) findViewById(R.id.numberPicker1);
        distancePick.setMinValue(0);
        distancePick.setMaxValue(50);
        distancePick.setWrapSelectorWheel(false);
        distancePick.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                distance += (newVal-oldVal);
            }
        });
        distanceDecPick = (NumberPicker) findViewById(R.id.numberPicker2);
        distanceDecPick.setMinValue(0);
        distanceDecPick.setMaxValue(99);
        distanceDecPick.setWrapSelectorWheel(true);
        distanceDecPick.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                distance += ((float)(newVal-oldVal))/100.0f;
            }
        });

        setBtn = (Button) findViewById(R.id.set);
        setBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                talker.sendDistance(distance);
            }
        });

        seekBar = (SeekBar) findViewById(R.id.twistBar);
        seekBar.setEnabled(true);
        seekBar.setMax(360);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = MainActivity.this.seekBar.getProgress();
                //sendOrientation(value);

                talker.sendOrientation(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

            }
        });



        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            t=Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT);
                    t.show();
            finish();
        }


        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            t=Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT);
                    t.show();
            finish();
            return;
        }

        Intent intent = getIntent();

        mDeviceAddress = intent.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
        mDeviceName = intent.getStringExtra(Device.EXTRA_DEVICE_NAME);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        /*
        Intent gattServiceIntent = new Intent(MainActivity.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        */
    }

    public void sendOrientation(int value) {
        Log.d("MainActivity",String.format("sendOrientation(%d)",value));
        byte[] buf = new byte[]{(byte) 0x05, (byte) 0x00, (byte) 0x00};
        buf[1] = (byte) ((value >> 8) & 0xFF);
        buf[2] = (byte) ((value & 0xFF));
        if (!bleSend(buf)) {
            warnNotConnected();
        }
    }


    private boolean bleSend( byte[] buf) {
           Log.d("BleSend",String.format("buff[%d]",buf.length));
        if(characteristicTx != null && mBluetoothLeService != null) {
            characteristicTx.setValue(buf);
            mBluetoothLeService.writeCharacteristic(characteristicTx);
            return true;
        }
        return false;
    }

    private int constrain(int valueIn, int valueMin, int valueMax) {
        if(valueIn<valueMin){
            return valueMin;
        }
        if(valueIn>valueMax){
            return valueMax;
        }
        return valueIn;
    }

    private void warnNotConnected() {
        if(t != null && (! t.getView().isShown() )){
            t=Toast.makeText(this, "Not yet connected...", Toast.LENGTH_SHORT);
            t.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void displayData(String data) {
        if (data != null) {
            rssiValue.setText(data);
        }
    }
    /*
        private void readAnalogInValue(byte[] data) {
            for (int i = 0; i < data.length; i += 3) {
                if (data[i] == 0x0A) {
                    if (data[i + 1] == 0x01)
                        digitalInBtn.setChecked(false);
                    else
                        digitalInBtn.setChecked(true);
                } else if (data[i] == 0x0B) {
                    int Value;

                    Value = ((data[i + 1] << 8) & 0x0000ff00)
                            | (data[i + 2] & 0x000000ff);

                    AnalogInValue.setText(Value + "");
                }
            }
        }
    */
    /*
	private void setButtonEnable() {
		flag = true;
		connState = true;

		digitalOutBtn.setEnabled(flag);
		AnalogInBtn.setEnabled(flag);
		servoSeekBar.setEnabled(flag);
		PWMSeekBar.setEnabled(flag);
		connectBtn.setText("Disconnect");
	}

	private void setButtonDisable() {
		flag = false;
		connState = false;

		digitalOutBtn.setEnabled(flag);
		AnalogInBtn.setEnabled(flag);
		servoSeekBar.setEnabled(flag);
		PWMSeekBar.setEnabled(flag);
		connectBtn.setText("Connect");
	}
*/
    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(RSSI_SLEEP_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {


        if (gattService == null)
            return;

        //setButtonEnable();
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 32, j = 0; i >= 17; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    if (stringToUuidString(serviceUuid).equals(
                            RBLGattAttributes.BLE_SHIELD_SERVICE
                                    .toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("BLE","askedToStop");
        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == RosActivity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
