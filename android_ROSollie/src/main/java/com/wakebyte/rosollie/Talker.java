package com.wakebyte.rosollie;

import android.content.Intent;
import android.util.Log;

import com.orbotix.common.sensor.AccelerometerData;
import com.orbotix.common.sensor.GyroData;

import org.ros.internal.message.RawMessage;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Quaternion;
import geometry_msgs.Vector3;
import sensor_msgs.Imu;
import std_msgs.Header;
import std_msgs.Int32;

/**
 * Created by robotcontrol on 6/25/14.
 */
public class Talker extends AbstractNodeMain{
    MainActivity activity = null;

    public Talker(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ollie/talker");
    }
    Publisher<std_msgs.String> pubCommand;
    Publisher<Imu> pubImu;
    Publisher<Int32> pubRSSI;
    Publisher<Int32> pubCmnd;
    Publisher<Int32> pubOrient;
    Publisher<std_msgs.Float32> pubDistance;
    private final String TAG = "TalkerX";


    int orient = 0;
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        pubCommand = connectedNode.newPublisher("ollie/commands", std_msgs.String._TYPE);
        pubImu = connectedNode.newPublisher("ollie/IMU", Imu._TYPE);
        pubImu = connectedNode.newPublisher("chatter", std_msgs.String._TYPE);
        pubOrient = connectedNode.newPublisher("ollie/orientation", Int32._TYPE);
        pubDistance = connectedNode.newPublisher("ollie/distance", std_msgs.Float32._TYPE);

        Subscriber<Int32> subscriber = connectedNode.newSubscriber("orientation", Int32._TYPE);
        subscriber.addMessageListener(new MessageListener<Int32>() {
            @Override
            public void onNewMessage(final Int32 message) {
                Log.d(TAG,String.format("sendOrientation(%d)",message.getData()));
                if (activity != null) {
                    Log.d(TAG, String.format("orient(%d)", message.getData()));
                }
            }
        });

        Subscriber<std_msgs.String> subscriber2 = connectedNode.newSubscriber("newchat", std_msgs.String._TYPE);
        subscriber2.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(final std_msgs.String message) {
                Log.d(TAG,String.format("heard(%s)",message.getData()));
            }
        });

/*
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {

            @Override
            protected void setup() {
                //some setup stuff here
            }

            @Override
            protected void loop() throws InterruptedException {
                std_msgs.Int32 val = pubOrient.newMessage();
                str.setData(orient);
                pubOrient.publish(val);
                Thread.sleep(1000);
                orient += 5;
                if( orient > 360){
                    orient = 0;
                }
            }

        });
    */
    }

    public void sendIMU(AccelerometerData accelerometerData, GyroData gyroData, int orientation){
        /*Imu message = pubImu.newMessage();
        message.setAngularVelocity(Vector3.zero());
        message.setLinearAcceleration();
        message.setOrientation();*/
    }
    public boolean sendString(String str){
        if (pubCommand != null){
            Log.i(TAG, String.format("Publishing chatter: %s",str));
            std_msgs.String string = pubCommand.newMessage();
            string.setData(str);
            pubCommand.publish(string);
            return true;
        }
        return false;
    }
    public boolean sendCmnd(int command){
        if (pubCmnd != null){
            Log.i(TAG, String.format("Publishing Command: %d",command));
            Int32 val = pubCmnd.newMessage();
            val.setData(command);
            pubCmnd.publish(val);
            return true;
        }
        return false;
    }
    public boolean sendRSSI(int rssi){
        if (pubRSSI != null){
            Log.i(TAG, String.format("Publishing RSSI: %d",rssi));
            Int32 val = pubRSSI.newMessage();
            val.setData(rssi);
            pubRSSI.publish(val);
            return true;
        }
        return false;
    }
    public boolean sendOrientation(int angle){
        if (pubOrient != null){
            Log.i(TAG, String.format("Publishing RSSI: %d",angle));
            Int32 val = pubOrient.newMessage();
            val.setData(angle);
            pubOrient.publish(val);
            return true;
        }
        return false;
    }

    public boolean sendDistance(float distance) {
        if (pubDistance != null){
            Log.i(TAG, String.format("Publishing Distance: %f",distance));
            std_msgs.Float32 val = pubDistance.newMessage();
            val.setData(distance);
            pubDistance.publish(val);
            return true;
        }
        return false;
    }

    private RssiReceiver rssiReceiver = new RssiReceiver(){
        @Override
        protected void onRSSI(Intent intent) {
            //super.onRSSI(intent);
            Log.d(TAG,"onRSSI()");
            Integer rssi = intent.getIntExtra(RSSI_Service.RSSI_DATA, 1);
            Integer median = intent.getIntExtra(RSSI_Service.RSSI_DATA_MEDIAN, 1);
            if (median < 0){
                //sendMedRSSI(median);
            }
            if (rssi < 0){
                sendRSSI(rssi);
            }
        }
    };
}
