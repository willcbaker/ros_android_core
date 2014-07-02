package org.ros.android.app;

import android.util.Log;

/**
 * Created by robotcontrol on 6/27/14.
 */
public class GamePadState {
    private int numButtons;
    private int numAxes;
    private int[] buttons;
    private float[] axes;
    private String name;

    GamePadState(String name, int numAxes, int numButtons){
        Log.v("BUTTONS",String.format("Creating new GamePadState for %s ( %d , %d )",name, numAxes,numButtons));
        this.name = name;
        this.numButtons = numButtons;
        this.numAxes=numAxes;
        buttons = new int[numButtons];
        axes = new float[numAxes];
    }

    public void setButtons(int[] buttons) {
        this.buttons = buttons;
    }

    public void setAxes(float[] axes) {
        this.axes = axes;
    }
    public void setButton(int button, int value) {
        if(button >= 0 && button < numButtons) {
            this.buttons[button] = value;
            Log.d("BUTTONS", String.format("set button %d to %d.", button, value));
        }
        else {
            Log.e("BUTTON",String.format("Unable to set button %d",button));
        }
    }

    public void setAxis(int axis, float value) {
        if(axis >= 0 && axis < numAxes) {
            this.axes[axis] = value;
            Log.e("AXES", String.format("Setting axis %d to %f", axis,value));
        }
        else {
            Log.e("AXES",String.format("Unable to set axis %d",axis));
        }
    }

    public int[] getButtons() {
        return buttons;
    }

    public float[] getAxes() {
        return axes;
    }

    public String getDeviceName() {
        return name;
    }
}
