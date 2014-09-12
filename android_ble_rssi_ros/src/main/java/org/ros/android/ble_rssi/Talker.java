package org.ros.android.ble_rssi;

import android.app.Activity;
import android.util.Log;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.android.MessageCallable;
import org.ros.message.MessageListener;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import geometry_msgs.Point32;
import geometry_msgs.PolygonStamped;
import sensor_msgs.Joy;
import std_msgs.Float64;
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
        return GraphName.of("rosjava_ble_rssi/talker");
    }
    Publisher<std_msgs.String> pubString;
    Publisher<std_msgs.Int32> pubRSSI;
    Publisher<std_msgs.Int32> pubCmnd;
    Publisher<std_msgs.Int32> pubOrient;
    Publisher<std_msgs.Float32> pubDistance;


    int orient = 0;
    @Override
    public void onStart(final ConnectedNode connectedNode) {
        pubString = connectedNode.newPublisher("chatter", std_msgs.String._TYPE);
        pubRSSI = connectedNode.newPublisher("rssi", std_msgs.Int32._TYPE);
        pubCmnd = connectedNode.newPublisher("command", std_msgs.Int32._TYPE);
        pubOrient = connectedNode.newPublisher("orientation", std_msgs.Int32._TYPE);
        pubDistance = connectedNode.newPublisher("distance", std_msgs.Float32._TYPE);

        Subscriber<std_msgs.Int32> subscriber = connectedNode.newSubscriber("orientation", std_msgs.Int32._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.Int32>() {
            @Override
            public void onNewMessage(final std_msgs.Int32 message) {
                Log.d("Talker",String.format("sendOrientation(%d)",message.getData()));
                if (activity != null) {
                    activity.sendOrientation(message.getData());
                }
            }
        });

        Subscriber<std_msgs.String> subscriber2 = connectedNode.newSubscriber("newchat", std_msgs.String._TYPE);
        subscriber2.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(final std_msgs.String message) {
                Log.d("Talker",String.format("heard(%s)",message.getData()));
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
    /*
    @Override
    public void update(Observable observable, Object data) {
        Log.d("UPDATE", observable.getClass().getName());
        if(observable instanceof Shield){
            Log.d("OBSERVER","isShield");

            if( data instanceof String){
                Log.d("OBSERVER","isString");
                Log.i("OBSERVER", (String) data);
                send( (String) data);
            }
            else{
                Log.d("OBSERVER",String.format("Received data of type %s.",data.getClass().getName()));
            }
        }
        else
            Log.d("OBSERVER","notShield");
    }*/

    public boolean sendString(String str){
        if (pubString != null){
            Log.i("org.ros.android.ble_rssi.Talker", String.format("Publishing chatter: %s",str));
            std_msgs.String string = pubString.newMessage();
            string.setData(str);
            pubString.publish(string);
            return true;
        }
        return false;
    }
    public boolean sendCmnd(int command){
        if (pubCmnd != null){
            Log.i("org.ros.android.ble_rssi.Talker", String.format("Publishing Command: %d",command));
            std_msgs.Int32 val = pubCmnd.newMessage();
            val.setData(command);
            pubCmnd.publish(val);
            return true;
        }
        return false;
    }
    public boolean sendRSSI(int rssi){
        if (pubRSSI != null){
            Log.i("org.ros.android.ble_rssi.Talker", String.format("Publishing RSSI: %d",rssi));
            std_msgs.Int32 val = pubRSSI.newMessage();
            val.setData(rssi);
            pubRSSI.publish(val);
            return true;
        }
        return false;
    }
    public boolean sendOrientation(int angle){
        if (pubOrient != null){
            Log.i("org.ros.android.ble_rssi.Talker", String.format("Publishing RSSI: %d",angle));
            std_msgs.Int32 val = pubOrient.newMessage();
            val.setData(angle);
            pubOrient.publish(val);
            return true;
        }
        return false;
    }

    public boolean sendDistance(float distance) {
        if (pubDistance != null){
            Log.i("org.ros.android.ble_rssi.Talker", String.format("Publishing Distance: %f",distance));
            std_msgs.Float32 val = pubDistance.newMessage();
            val.setData(distance);
            pubDistance.publish(val);
            return true;
        }
        return false;
    }
}
