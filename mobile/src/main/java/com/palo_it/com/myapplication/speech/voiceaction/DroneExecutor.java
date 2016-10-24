package com.palo_it.com.myapplication.speech.voiceaction;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.palo_it.com.myapplication.R;
import com.palo_it.com.myapplication.activity.SpeechRecognizingActivity;
import com.palo_it.com.myapplication.drone.DroneReadyListener;
import com.palo_it.com.myapplication.drone.DroneService;
import com.palo_it.com.myapplication.drone.JSDrone;
import com.palo_it.com.myapplication.drone.JSDroneListenerBase;
import com.palo_it.com.myapplication.drone.JSDroneStatusListener;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;

public class DroneExecutor extends VoiceActionExecutor implements DroneReadyListener {


    private final DroneService droneController = DroneService.getInstance();
    private final Activity activity;
    private ProgressDialog progressDialog;
    private JSDrone drone;
    private TextToSpeech tts;

    public DroneExecutor(SpeechRecognizingActivity speech) {
        super(speech);
        this.activity = speech;
        if (drone == null) {
            progressDialog = new ProgressDialog(speech, R.style.AppCompatAlertDialogStyle);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Waiting for drone to come online...");
            progressDialog.show();
        }
        droneController.initDiscoveryService(speech, this);
    }


    @Override
    public void setTts(TextToSpeech tts) {
        this.tts = tts;
        super.setTts(tts);
    }

    @Override
    public void speak(String toSay) {
        super.speak(toSay);
        while (tts.isSpeaking()) ;
    }


    public void doAction(Pair<String, String> action, String name) {

        /*
        * TODO: Create a new VoiceAction for this
        * =================================================================
         */
        String apiAction = action.first;
        String outputMessage = action.second;
        if (action.first != null) {
            JSDrone.ACTIONS actionEnum = Enum.valueOf(JSDrone.ACTIONS.class, apiAction.toUpperCase());
            switch (actionEnum) {
                case MYNAMEIS:
                    outputMessage = outputMessage.concat(" ").concat(name);
                    break;
                case WHATSMYNAME:
                    outputMessage = name.isEmpty() ? "Je ne sais pas" : outputMessage.concat(" ").concat(name);
                    break;
            }
        /*
        * =================================================================
         */

            if (!outputMessage.isEmpty()) {
                speak(outputMessage);
            }
            Toast.makeText(activity, apiAction, Toast.LENGTH_LONG).show();
            if (actionEnum.equals(JSDrone.ACTIONS.STOP)) {
                activity.finish();
            } else {
                new AsyncTask<JSDrone.ACTIONS, Void, Void>() {
                    @Override
                    protected Void doInBackground(JSDrone.ACTIONS... params) {
                        if (drone != null) {
                            for (JSDrone.ACTIONS action : params) {
                                if (action != null) {
                                    drone.doSomething(action);
                                }
                            }
                        }
                        return null;
                    }
                }.execute(actionEnum);
            }
        }
    }

    @Override
    public void onDroneReady(JSDrone drone) {
        this.drone = drone;
        this.drone.addListener(new JSDroneListenerBase() {
        });
        final Handler handler = new Handler(activity.getMainLooper());
        drone.setAsyncListener(new JSDroneStatusListener() {
            @Override
            public void asyncReceiver(Runnable task) {
                handler.post(task);
            }
        });
        if (doConnectDrone()) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public boolean doConnectDrone() {
        return this.drone.connect();
    }

    @Override
    public boolean isDroneConnected() {
        return drone.getConnectionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING);
    }

    private <T extends Enum<T>> T getOrNull(Class<T> enumClass, String enumValue) {
        try {
            return Enum.valueOf(enumClass, enumValue);
        } catch (IllegalArgumentException e) {
            //not found
            Log.d(TAG, "Drone command not found: " + enumValue);
            return null;
        }
    }
}
