package fi.semiproot.featofspeed;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;


public class GeofenceTransitionsIntentService extends IntentService {
    /*
    Has snippets from https://github.com/googlesamples/android-play-location/tree/master/Geofencing
     */
    protected static final String TAG = "GeofenceTransitionsIS";

    protected static final String ENTERED_GEOFENCE = "FOS_GEOFENCE_ENTERED";
    protected static final String EXITED_GEOFENCE = "FOS_GEOFENCE_EXITED";

    /**
     * Default constructor for this service
     */
    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionsIntentService(String name) {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            return;
        }
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofenceErrorCode: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get transition event type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "Entered geofence");
            Intent broadcast_intent = new Intent(GeofenceTransitionsIntentService.ENTERED_GEOFENCE);

            // Fetch ids of triggered geofences
            ArrayList<String> triggered_ids = new ArrayList<>();
            for (Geofence fence : geofencingEvent.getTriggeringGeofences()) {
                triggered_ids.add(fence.getRequestId());
            }

            // send intent to show stamp alert
            Bundle extras = new Bundle();
            extras.putStringArrayList("ids", triggered_ids);
            broadcast_intent.putExtras(extras);
            sendBroadcast(broadcast_intent);

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Exited geofence");
            Intent broadcast_intent = new Intent(GeofenceTransitionsIntentService.EXITED_GEOFENCE);

            // Fetch ids of triggered geofences
            ArrayList<String> triggered_ids = new ArrayList<>();
            for (Geofence fence : geofencingEvent.getTriggeringGeofences()) {
                triggered_ids.add(fence.getRequestId());
            }

            // send intent to show stamp alert
            Bundle extras = new Bundle();
            extras.putStringArrayList("ids", triggered_ids);
            broadcast_intent.putExtras(extras);
            sendBroadcast(broadcast_intent);
        }
        else {
            Log.d(TAG, "Unhandled transition type: " + geofenceTransition);
        }
    }
}
