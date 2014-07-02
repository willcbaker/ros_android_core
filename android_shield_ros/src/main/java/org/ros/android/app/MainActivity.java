package org.ros.android.app;

import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import org.ros.address.InetAddressFactory;
import org.ros.android.MessageCallable;
import org.ros.android.R;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


public class MainActivity extends RosActivity{//ActionBarActivity {

    Talker talker;
    Shield shield;
    private RosTextView<std_msgs.String> rosTextView;

    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("ROS Shield", "ROS Shield");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rosTextView = (RosTextView<std_msgs.String>) findViewById(R.id.text);
        rosTextView.setTopicName("chatter");
        rosTextView.setMessageType(std_msgs.String._TYPE);
        rosTextView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
            @Override
            public String call(std_msgs.String message) {
                return message.getData();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        shield = new Shield();
        talker = new Talker(shield);
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress().toString());
        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(talker, nodeConfiguration);
        // The RosTextView is also a NodeMain that must be executed in order to
        // start displaying incoming messages.
        nodeMainExecutor.execute(rosTextView, nodeConfiguration);
    }

    public void sendShield(View view){
        talker.send("Shield_Button");

    }
    public void sendClose(View view){
        talker.send("Close_Button");
    }
    public void sendOne(View view){
        talker.send(1);
    }
    public void sendTwo(View view){
        talker.send(2);
    }
    public void sendThree(View view){
        talker.send(3);
    }
    public void sendFour(View view){
        talker.send(4);
    }
    public void sendFive(View view){
        talker.send(5);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        Log.d("MOTION",String.format("eventSource: %d", event.getSource()));
        if (((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) == 0)
                || (event.getAction() != MotionEvent.ACTION_MOVE)
                || shield == null) {
            return super.dispatchGenericMotionEvent(event);
        }

        Log.i("JOYSTICK","onMotion");
        shield.onMotion(event);//TODO: Unused boolean

        return super.dispatchGenericMotionEvent(event);
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (shield == null) {
            return super.dispatchKeyEvent(event);
        }
        if ( shield.onKey(event) )
            return true;

        return super.dispatchKeyEvent(event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        Log.d("TEST", "OBSERVER-onTouch");
        talker.update(shield,event);
        return true;
    }
}
