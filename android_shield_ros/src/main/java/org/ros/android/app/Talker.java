package org.ros.android.app;

import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import geometry_msgs.Point32;
import geometry_msgs.PolygonStamped;
import sensor_msgs.Joy;
import std_msgs.Int32;

/**
 * Created by robotcontrol on 6/25/14.
 */
public class Talker extends AbstractNodeMain implements Observer{
    Shield shield;
    public Talker(Shield _shield) {
        shield = _shield;
        shield.addObserver(this);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_shield_controller/talker");
    }
    Publisher<std_msgs.String> pubString;
    Publisher<Joy> pubJoy;
    Publisher<PolygonStamped> pubTouch;
    Publisher<Int32> pubInt;
    Publisher<Point32> pubPoint;


    private int touchEvents;
    private int gamePadEvents;
    private int shieldCommandEvents;

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        pubString = connectedNode.newPublisher("chatter", std_msgs.String._TYPE);
        pubJoy = connectedNode.newPublisher("joy", Joy._TYPE);
        pubTouch = connectedNode.newPublisher("touch", PolygonStamped._TYPE);
        pubPoint = connectedNode.newPublisher("points", Point32._TYPE);
        pubInt = connectedNode.newPublisher("shield_commands", Int32._TYPE);
        // This CancellableLoop will be canceled automatically when the node shuts
        // down.
        connectedNode.executeCancellableLoop(new CancellableLoop() {

            @Override
            protected void setup() {
                touchEvents = 0;gamePadEvents = 0;
            }

            @Override
            protected void loop() throws InterruptedException {
                std_msgs.String str = pubString.newMessage();
                str.setData("TouchEvents: " + touchEvents
                        + "\nGamePadEvents: " + gamePadEvents
                        + "\nShieldCommands: " + shieldCommandEvents);
                pubString.publish(str);
                Thread.sleep(1000);
            }
        });
    }
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
            else if (data instanceof GamePadState){
                Log.d("OBSERVER","isGamePadEvent");
                send((GamePadState) data);
            }
            else if (data instanceof MotionEvent){
                send((MotionEvent) data);

            }
            else{
                Log.d("OBSERVER",String.format("Received data of type %s.",data.getClass().getName()));
            }
        }
        else
            Log.d("OBSERVER","notShield");
    }

    public boolean send(String str){
        if (pubString != null){
            Log.i("Talker", str);
            std_msgs.String string = pubString.newMessage();
            string.setData(str);
            pubString.publish(string);
            return true;
        }
        return false;
    }

    public boolean send(int num){
        if (pubString != null){
            Log.i("Talker", String.format("sendInt(%d)",num));
            Int32 msg = pubInt.newMessage();
            msg.setData(num);
            pubInt.publish(msg);
            shieldCommandEvents++;
            return true;
        }
        return false;
    }
    private void send(MotionEvent event) {
        int eventSource = (event.getSource());
        if( ((eventSource & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) ) {
            Log.d("OBSERVER","isMotionJoyEvent");
        }
        else if((eventSource & InputDevice.SOURCE_TOUCHSCREEN) != 0){
            touchEvents++;
            if(pubTouch != null) {
                PolygonStamped msg = pubTouch.newMessage();
                List<Point32> points = msg.getPolygon().getPoints();
                Log.d("POINTERS", String.format("Pointers Length: %d", event.getPointerCount()));

                for (int i = 0; i < event.getPointerCount(); i++) {
                    int mActivePointerId = event.getPointerId(0);
                    int pointerIndex = event.findPointerIndex(mActivePointerId);
                    Log.d("POINTERS", String.format("(%d-%d-%d)Pointers Length: %d", i, mActivePointerId, pointerIndex, event.getPointerCount()));
                    Point32 point = pubPoint.newMessage();
                    point.setX(event.getX(pointerIndex));
                    point.setY(event.getY(pointerIndex));
                    point.setZ(pointerIndex);
                    pubPoint.publish(point);
                    points.add(point);
                }

                Log.d("OBSERVER", String.format("isMotionTouchEvent(%d)", event.getPointerCount()));

                Log.d("POINTS", points.toArray().toString());
                pubTouch.publish(msg);
            }

        }
        else{
            Log.e("OBSERVER","Unknown motion event source.");
        }
    }

    private void send(GamePadState state) {
        if(pubJoy != null){
            Joy msg = pubJoy.newMessage();
            msg.getHeader().setFrameId(state.getDeviceName());
            msg.setAxes(state.getAxes());
            msg.setButtons(state.getButtons());
            pubJoy.publish(msg);
            gamePadEvents++;
        }
    }
}
