package org.ros.android.app;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

/**
 * Created by robotcontrol on 6/25/14.
 */
public class Shield extends Observable {
    final static int axisX_L = MotionEvent.AXIS_X;
    final static int axisY_L = MotionEvent.AXIS_Y;
    final static int axisX_R = MotionEvent.AXIS_Z;
    final static int axisY_R = MotionEvent.AXIS_RZ;
    final static int triggerLeft = MotionEvent.AXIS_LTRIGGER;
    final static int triggerRight = MotionEvent.AXIS_RTRIGGER;
    private static final float DEADZONE = 0.001f;
    private static final String DEVICE_NAME = "Shield";

    public int[] gamePadAxisIndices = null;
    public float[] gamePadAxisMinVals = null;
    public float[] gamePadAxisMaxVals = null;


    private InputDevice mDevice;
    private SparseIntArray mAxes;
    private SparseIntArray mKeys;

    private GamePadState state;
    private SparseArray<String> axesNames;

    public GamePadState getGamePadState(){
        return state;
    }

    private static final String TAG = "NativeGamepad";
    static int[] gamePadButtonMapping =
            {
                    KeyEvent.KEYCODE_BUTTON_1,
                    KeyEvent.KEYCODE_BUTTON_2,
                    KeyEvent.KEYCODE_BUTTON_3,
                    KeyEvent.KEYCODE_BUTTON_4,
                    KeyEvent.KEYCODE_BUTTON_5,
                    KeyEvent.KEYCODE_BUTTON_6,
                    KeyEvent.KEYCODE_BUTTON_7,
                    KeyEvent.KEYCODE_BUTTON_8,
                    KeyEvent.KEYCODE_BUTTON_9,
                    KeyEvent.KEYCODE_BUTTON_10,
                    KeyEvent.KEYCODE_BUTTON_11,
                    KeyEvent.KEYCODE_BUTTON_12,
                    KeyEvent.KEYCODE_BUTTON_13,
                    KeyEvent.KEYCODE_BUTTON_14,
                    KeyEvent.KEYCODE_BUTTON_15,
                    KeyEvent.KEYCODE_BUTTON_16,
                    KeyEvent.KEYCODE_BUTTON_A,
                    KeyEvent.KEYCODE_BUTTON_B,
                    KeyEvent.KEYCODE_BUTTON_C,
                    KeyEvent.KEYCODE_BUTTON_X,
                    KeyEvent.KEYCODE_BUTTON_Y,
                    KeyEvent.KEYCODE_BUTTON_Z,
                    KeyEvent.KEYCODE_BUTTON_L1,
                    KeyEvent.KEYCODE_BUTTON_L2,
                    KeyEvent.KEYCODE_BUTTON_R1,
                    KeyEvent.KEYCODE_BUTTON_R2,
                    KeyEvent.KEYCODE_BUTTON_START,
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    KeyEvent.KEYCODE_BUTTON_MODE,
                    KeyEvent.KEYCODE_BUTTON_THUMBL,
                    KeyEvent.KEYCODE_BUTTON_THUMBR,
                    KeyEvent.KEYCODE_HOME,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER
            };

    public Shield() {


        Log.v("NativeGamepad", "Calling subclass onCreate");

        mAxes = new SparseIntArray();//int[numAxes];
        mKeys = new SparseIntArray();
        axesNames = new SparseArray<String>();

        // Before we go and start using API12 functions, we'd better
        // check for them being there!!
        boolean hasJoystickMethods = false;
        try {
            Method level12Method = KeyEvent.class.getMethod(
                    "keyCodeToString", new Class[]{int.class});
            hasJoystickMethods = (level12Method != null);
            Log.d(TAG, "****** Found API level 12 function! Joysticks supported");
        } catch (NoSuchMethodException nsme) {
            Log.d(TAG, "****** Did not find API level 12 function! Joysticks NOT supported!");
        }

        if (hasJoystickMethods) {
            String deviceInfoText = "";//dumpDeviceInfo();

            deviceInfoText += "\n";

            InputDevice joystick = findJoystick();
            if (joystick != null) {
                mDevice = joystick;//TODO:may need to consider multiple joysticks...
                deviceInfoText += "Joystick attached: " + joystick.getName() + "\n";
                deviceInfoText += dumpJoystickInfo(joystick);
            }

            deviceInfoText += "\n";

            deviceInfoText += dumpGamePadButtons();

            Log.i(TAG, deviceInfoText);

            // CONFIGURE the axes//TODO:check mDevice for null
            int numAxes = 0;
            for (InputDevice.MotionRange range : mDevice.getMotionRanges()) {
                if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                    //numAxes += 1;
                    Log.d("AXES",String.format("Putting axis %d -> %d",range.getAxis(),numAxes));
                    mAxes.put(range.getAxis(),numAxes++);
                }
            }
            // CONFIGURE the buttons
            int numButtons = 0;
            for (int j = 0; j < gamePadButtonMapping.length; j++) {
                int button = gamePadButtonMapping[j];

                if (KeyCharacterMap.deviceHasKey(button)) {
                    Log.d("BUTTONS",String.format("Putting button %d -> %d",button,numButtons));
                    mKeys.put(button,numButtons++);
                }
            }
            //for some reason does not add DPAD buttons:
            mKeys.put(KeyEvent.KEYCODE_DPAD_UP,numButtons++);
            mKeys.put(KeyEvent.KEYCODE_DPAD_RIGHT,numButtons++);
            mKeys.put(KeyEvent.KEYCODE_DPAD_DOWN,numButtons++);
            mKeys.put(KeyEvent.KEYCODE_DPAD_LEFT,numButtons++);

            for (int i = 0; i < numAxes; i++) {
                Log.d("AXES",String.format("mAxes.valueAt(%d) is %d maps to %d.",i,mAxes.keyAt(i), mAxes.valueAt(i)));
            }
            for (int i = 0; i < numButtons; i++) {
                Log.d("BUTTONS",String.format("mKeys.valueAt(%d) is %d maps to %d.",i,mKeys.keyAt(i), mKeys.valueAt(i)));
            }
            state = new GamePadState(DEVICE_NAME,numAxes,numButtons);
            setChanged();notifyObservers(state);
        } else {
            Log.e("JOYSTICK", "Failed to initialize.");
            //TODO: Handle exception
        }

    }

    public InputDevice getDevice() {
        return mDevice;
    }

    public SparseIntArray getAxes() {
        return mAxes;
    }

    public String dumpJoystickInfo(InputDevice dev) {
        String infoString = "";
        boolean firstAxis = true;

        List<InputDevice.MotionRange> ranges = dev.getMotionRanges();

        int arrayCount = ranges.size();

        gamePadAxisIndices = new int[arrayCount];
        gamePadAxisMinVals = new float[arrayCount];
        gamePadAxisMaxVals = new float[arrayCount];

        int arrayIndex = 0;

        Iterator<InputDevice.MotionRange> iterator = ranges.iterator();
        while (iterator.hasNext()) {
            InputDevice.MotionRange range = iterator.next();
            if (firstAxis)
                infoString += "\tAxes:\n";
            infoString += "\t\t" + MotionEvent.axisToString(range.getAxis()) +
                    " (" + range.getMin() + ", " + range.getMax() + ")\n";
            firstAxis = false;

            gamePadAxisIndices[arrayIndex] = range.getAxis();
            gamePadAxisMinVals[arrayIndex] = range.getMin();
            gamePadAxisMaxVals[arrayIndex] = range.getMax();

            arrayIndex++;
        }
        infoString += "\n";

        return infoString;
    }


    public InputDevice findBySource(int sourceType) {
        int[] ids = InputDevice.getDeviceIds();

        // Return the first matching source we find...
        int i = 0;
        for (i = 0; i < ids.length; i++) {
            InputDevice dev = InputDevice.getDevice(ids[i]);
            int sources = dev.getSources();

            if ((sources & ~InputDevice.SOURCE_CLASS_MASK & sourceType) != 0) {
                return dev;
            }
        }

        return null;
    }


    // Sadly, this Android key-mapping call is _static_; it can only tell us
    // that _SOME_ device can generate the events.  It cannot be limited to a
    // specific device, so we cannot tell if the gamePad device is the one that
    // generates them...
    public String dumpGamePadButtons() {
        String infoString = "";
        boolean buttonPrinted = false;

        for (int j = 0; j < gamePadButtonMapping.length; j++) {
            int button = gamePadButtonMapping[j];
            if (KeyCharacterMap.deviceHasKey(button)) {
                if (!buttonPrinted)
                    infoString += "Has Buttons: ";

                infoString += KeyEvent.keyCodeToString(button) + " ";
                buttonPrinted = true;
            }
        }
        return infoString;
    }

    public InputDevice findJoystick() {
        return findBySource(InputDevice.SOURCE_JOYSTICK);
    }

    public boolean onMotion(MotionEvent event) {

        Log.i("JOYSTICK", "onMotion-ENTER");
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0) {
            return false;
        }

        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            processJoystickInput( event, i);
        }

        processJoystickInput( event, -1);
        setChanged();
        notifyObservers(state);//TODO: create a joystick event class?
        return true;

    }

    private void processJoystickInput( MotionEvent event, int historyPos) {

        int key = 0;
        for(int i = 0; i < mAxes.size(); i++) {
            key = mAxes.keyAt(i);
            float val = processAxis(mAxes.keyAt(i), event, historyPos);
            Log.v(TAG, String.format("AXES_%d - is %d - %f", key, mAxes.get(key), val));
        }
        if (historyPos >= 0) {
            Log.i(TAG, String.format("JOYSTICK_HIST - %d", historyPos));
        } else {
            Log.i(TAG, String.format("JOYSTICK_TIME - %d", event.getEventTime()));
        }
    }


    private float processAxis(int axis, MotionEvent event, int historyPos) {

        InputDevice.MotionRange range = mDevice.getMotionRange(axis, event.getSource());
        if (range != null) {
            float axisValue;
            if (historyPos >= 0) {
                axisValue = event.getHistoricalAxisValue(axis, historyPos);
            } else {
                axisValue = event.getAxisValue(axis);
            }
            float absAxisValue = Math.abs(axisValue);
            float deadZone = range.getFlat() + DEADZONE;
            if (absAxisValue <= deadZone) {
                state.setAxis(mAxes.get(axis),0.0f);
                return 0.0f;
            }

            float normalizedValue;
            if (axisValue < 0.0f) {
                normalizedValue = absAxisValue / range.getMin();
            } else {
                normalizedValue = absAxisValue / range.getMax();
            }

            state.setAxis(mAxes.get(axis), normalizedValue);
            return normalizedValue;
        }
        state.setAxis(mAxes.get(axis), 0.0f);
        return 0.0f;

    }

    public boolean onKey(KeyEvent event) {

        if (buttonMapped(event)) {
            setChanged();
            notifyObservers(state);
            return true;
        }
        return false;
    }
/*
    public boolean addAction(int buttonKeyCode, int actionCode) {
        actionMap.put(buttonKeyCode, actionCode);
        Log.d("BUTTON_MAP",String.format("added %d to %d",buttonKeyCode,actionCode));
        return false;
    }
*/
    private boolean buttonMapped(KeyEvent event) {
        /*int action = actionMap.get(event.getKeyCode(), -1);
        if (action < 0) {
            Log.d("BUTTON",String.format("NO MAPPING FOUND for Button_%d",event.getKeyCode()));
            for( int i = 0; i < actionMap.size(); i++)
                Log.d("BUTTON",String.format("Have: %d | %d",i,actionMap.get(i)));
            return false;
        }*/
        if (event.getKeyCode() == event.KEYCODE_BACK){
            return false;
        }
        //TODO:invoke the method somehow?
        switch (event.getAction()) {

            case KeyEvent.ACTION_DOWN:
                state.setButton(mKeys.get(event.getKeyCode(), -1),1);
                String thing = String.format("KEY_%d - DOWN", event.getKeyCode());
                Log.i("BUTTONS", thing);
                break;
            case KeyEvent.ACTION_UP:
                state.setButton(mKeys.get(event.getKeyCode(), -1),0);
                Log.i("BUTTONS", String.format("KEY_%d - UP", event.getKeyCode()));
                break;
            case KeyEvent.ACTION_MULTIPLE:
                Log.i("BUTTONS", String.format("KEY_%d - MULTIPLE", event.getKeyCode()));
                break;
            default:
                Log.i("BUTTONS", String.format("KEY_%d - OTHER", event.getKeyCode()));
                break;
        }
        return true;
    }
}
