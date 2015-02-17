package com.wakebyte.rosollie;

import android.util.Log;

import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.common.sensor.AccelerometerData;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.GyroData;
import com.orbotix.common.sensor.LocatorSensor;

import java.util.ArrayList;

/**
 * Created by lunabot on 2/17/15.
 */
public class LocationHandler implements ResponseListener{

    public LocationHandler() {
        this.mNormalizedLocation = updateLocation(new LocatorSensor(),0,0);
        this.mLastLocation = updateLocation(new LocatorSensor(),0,0);
    }

    private final String TAG = LocationHandler.class.getSimpleName();//

    private LocatorSensor mNormalizedLocation;
    private LocatorSensor mLastLocation;

    @Override
    public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

    }

    @Override
    public void handleStringResponse(String s, Robot robot) {

    }

    public void normalizeLocation() {
        updateLocation(mNormalizedLocation,mLastLocation);
        //updateLocation(mLastLocation,0,0);
    }

    public LocatorSensor normalizeLocation(LocatorSensor current, LocatorSensor storedLocation){
        current.x=current.x-storedLocation.x;
        current.y=current.y-storedLocation.y;
        return current;
    }

    public LocatorSensor updateLocation(LocatorSensor location, float x, float y){
        location.x = x;
        location.y = y;
        return location;
    }
    public LocatorSensor updateLocation(LocatorSensor location, LocatorSensor newLocation){
        location.x = newLocation.x;
        location.y = newLocation.y;
        return location;
    }

    @Override
    public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {
        Log.d(TAG, "Async found:");
        if(asyncMessage instanceof DeviceSensorAsyncMessage){
            //Log.d(TAG,"SensorAsync found:");
            //only seems to be one data
            //ArrayList<DeviceSensorsData> datas = ((DeviceSensorAsyncMessage) asyncMessage).getAsyncData();
            //for (DeviceSensorsData data : datas){

            ArrayList<DeviceSensorsData> asyncData = ((DeviceSensorAsyncMessage) asyncMessage).getAsyncData();
            if(asyncData != null) {
                DeviceSensorsData data = asyncData.get(0);
                if(data != null) {
                    LocatorSensor location = data.getLocatorData().getPosition();
                    if (location != null){
                        if(mNormalizedLocation != null){
                            updateLocation(mLastLocation,location);
                            normalizeLocation(location,mNormalizedLocation);
                        }
                        else{
                            Log.w(TAG,"Unable to normalize Location data.");
                        }
                        onLocationUpdate(location);
                    }
                    else{
                        //todo:onLocationUpdate(location);
                    }
                    onGyroUpdate(data.getGyroData());
                    onAccelUpdate(data.getAccelerometerData());
                }
            }
            /*Log.d(TAG,String.format("SensorData found(%d): [%s %s %s]",
                    location.getTimeStamp(),
                    location.toString(),
                    gyro.toString(),
                    accel.toString()
            ));*/

        }
    }

    public void onGyroUpdate(GyroData gyroData) {
        Log.i(TAG,"gyro updated.");
    }
    public void onAccelUpdate(AccelerometerData accelData) {
        Log.i(TAG,"accelerometer updated.");
    }

    public void onLocationUpdate(LocatorSensor location) {
        Log.i(TAG,"location updated.");
    }

}
