package com.softbankrobotics.followasyncdemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.autonomousabilities.DegreeOfFreedom;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.human.Human;
import com.aldebaran.qi.sdk.object.humanawareness.EngageHuman;
import com.aldebaran.qi.sdk.object.humanawareness.HumanAwareness;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "MainActivityFollow";
    private QiContext qiContext;

    final int STATE_INITIALIZING = 0;
    final int STATE_ALONE = 1;
    final int STATE_ENGAGING = 2;
    final int STATE_MOVING = 3;
    final int STATE_ARRIVED = 4;
    final int STATE_PAUSED = 5;
    private int state = STATE_INITIALIZING;
    private HumanAwareness humanAwareness = null;
    private Human engagedHuman;
    private Human nextHuman;
    private Say sayWait;
    private Future<Void> goToFuture;
    private Say sayComing;

    private Holder holder;

    private void showToast(String msg) {
        Log.i(TAG, "Toast: " + msg);
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);
        Button button = findViewById(R.id.sayhello);
        // TODO 2 add a button  to pause
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        QiSDK.unregister(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.i(TAG, "onRobotFocusGained()");

        this.qiContext = qiContext;
        sayWait = SayBuilder.with(qiContext)
                .withText("Wait for me!")
                .build();
        sayComing = SayBuilder.with(qiContext)
                .withText("I'm coming!")
                .build();
        holder = HolderBuilder.with(qiContext)
                .withDegreesOfFreedom(DegreeOfFreedom.ROBOT_FRAME_ROTATION)
                .build();

        state = STATE_ALONE;
        humanAwareness = qiContext.getHumanAwareness();
        humanAwareness.addOnRecommendedHumanToEngageChangedListener(this::onRecommendedHuman);
        onRecommendedHuman(humanAwareness.getRecommendedHumanToEngage()); // init
    }

    public void onRecommendedHuman(Human human) {
        Log.i(TAG, "onRecommendedHuman() -> state = " + state);

        // you could log face characteristics of the recommendedHuman like age, emotion, etc.
        // Log.i(TAG, "Emootion  and Age" )

        if ( (human!=null) && (state == STATE_ALONE)) {
            setState(STATE_ENGAGING);
            Log.i(TAG, "onRecommendedHuman() -> state = " + state);
            engagedHuman = human;
            // TODO : create an "engage" EngageHuman action

            // TODO Add the listeners to this "engage" action when human is engaged and disengaging
            // what do you want to do when the human is engaged
            // and when it disengages

            // Run the engage action
            Log.i(TAG, "Engage started.");
            engage.async().run().thenConsume((engageFuture) -> {
                Log.i(TAG, "Engage finished.");

                // TODO handle the cancellation, error and success of the future
                // make use of toast message

                engagedHuman = null;
                setState(STATE_ALONE);
                onRecommendedHuman(nextHuman);
            });
        } else {
            Log.i(TAG, "setting up nextHuman");

            nextHuman = human; // Who do I talk to if this guy leaves?
        }
    }

    private void onEngaged() {
        Log.i(TAG, "onEngaged()");

        if (state == STATE_ENGAGING) {
            sayComing.async().run();
            setState(STATE_MOVING);
            // TODO : you need to run a goto to get to the human
        }
    }

    private void goToEngaged() {
        Log.i(TAG, "goToEngaged()");

        // Try again
        if ((state == STATE_MOVING) && (engagedHuman != null)) {

            // TODO build a goto based on the headFrame of the human that is engaged

            // TODO holder hold

            // TODO run the goto

            // TODO then the gotoDone
        }
    }

    private void goToDone(Future<Void> future)  {
        Log.i(TAG, "goToDone()");

        // TODO handle the cancellation, error and success of the future
        // TODO on error run the goTo again
        // make use of toasts


        // TODO holder release
    }


    @Override
    public void onRobotFocusLost() {
        Log.i(TAG, "onRobotFocusLost");

        // TODO remember that listeners are not removed when the focus is lost
    }

    public void setState(int newState) {
        Log.i(TAG, "setState "+ newState);

        // Stop old state
        if ((state == STATE_MOVING) && (this.goToFuture != null)) {
            this.goToFuture.requestCancellation();
        }
        // Set state
        state = newState;
        String stateName = "UNKNOWN " + newState;
        switch(state) {
            case STATE_ALONE:
                stateName = "Alone";
                break;
            case STATE_ARRIVED:
                stateName = "Arrived";
                break;
            case STATE_ENGAGING:
                stateName = "Engaging";
                break;
            case STATE_INITIALIZING:
                stateName = "Initializing";
                break;
            case STATE_MOVING:
                stateName = "Moving";
                break;
            case STATE_PAUSED:
                stateName = "Paused";
                break;
        }
        Log.i(TAG, "New state: " + stateName);
        String finalStateName = stateName;
        runOnUiThread(() -> {
            Button button = findViewById(R.id.sayhello);
            button.setText(finalStateName);
        });

    }

    @Override
    public void onRobotFocusRefused(String reason) {
        showToast("Focus refused");

    }
}
