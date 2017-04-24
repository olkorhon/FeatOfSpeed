package fi.semiproot.featofspeed;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;


public class GeofenceTransitionsIntentService extends IntentService {
    /*
    Has snippets from https://github.com/googlesamples/android-play-location/tree/master/Geofencing
     */
    protected static final String TAG = "GeofenceTransitionsIS";

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
            // send intent to show stamp overlay
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Exited geofence");
            // send intent to hide stamp overlay
        }
    }
}
