package fi.semiproot.featofspeed;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class GameMapActivity extends FragmentActivity implements
        SensorEventListener, ConnectionCallbacks, OnConnectionFailedListener,
        OnMapReadyCallback, LocationListener, ResultCallback<Status>, StampWaypointFragment.StampWaypointDialogListener {

    private static final String TAG = GameMapActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 8888;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private static final String LOCATION_KEY = "location-key";
    private static final String WAYPOINTS_KEY = "all-waypoints";
    private static final String VISITED_WAYPOINTS_KEY = "visited-waypoints";
    private static final String VISITED_TIMESTAMPS_KEY = "visit-timestamps";
    private static final String GAME_START_TIMESTAMP = "date";

    // Map and location fields
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

    // Geofence fields
    private final int GEOFENCE_RADIUS = 100;            // Meters
    private final int GEOFENCE_EXPIRATION = 3600000;    // Hour in ms
    protected ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    // Broadcast receiver
    GeofenceEventReceiver mGeofenceEventReceiver;

    // Game related fields
    private String gameCode;
    private LatLng gameStartLatLng;
    //private ArrayList<Waypoint> mAllWaypoints = new ArrayList<>(DUMMY_WAYPOINTS);
    private ArrayList<Waypoint> mAllWaypoints;
    private ArrayList<Waypoint> mVisitedWaypoints;
    private ArrayList<Date> mVisitedTimestamps;
    private ArrayList<Circle> mWaypointCircles;
    private Date gameStartedTimestamp;

    // Dialog alert
    DialogFragment stampDialog;

    // Compass functionality
    CompassView compassWidget;

    private final int COMPASS_FILTER_SIZE = 8;
    Sensor accelometer;
    Sensor magnetometer;
    SensorManager sManager;

    float[] lastAcceleration;
    float[] lastMagnetometer;
    ArrayList<float[]> accFilterWindow;
    ArrayList<float[]> magFilterWindow;
    float[] rotationMatrix;
    float[] orientation;

    boolean rotationResult;

    Handler compassUpdateHandler;
    boolean compassUpdating;
    Runnable compassUpdaterRunnable = new Runnable() {
        @Override
        public void run() {
            compassWidget.refresh();

            if (compassUpdating)
                compassUpdateHandler.postDelayed(this, 60);
        }
    };

    // NOTE: you can minimize regions from the gutter
    // region Lifecycle callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_map);

        // Get an instance of GoogleAPIClient.
        getGoogleApiClient();

        // Fetch compass view instance from content
        compassWidget = (CompassView) findViewById(R.id.compass);

        // Get game data from LoadActivity's intent
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            gameCode = bundle.getString("code", "0000");
            gameStartLatLng = bundle.getParcelable("GAME_LAT_LNG");
            mAllWaypoints = (ArrayList<Waypoint>) bundle.getSerializable("waypoints");
            gameStartedTimestamp = (Date) bundle.getSerializable("date");

            compassWidget.setPos(gameStartLatLng);
            compassWidget.setWaypoints(mAllWaypoints);
        } else {
            Log.d(TAG, "Intent extras from LoadActivity are missing!!!");
        }

        if (gameStartLatLng == null) {
            Log.d(TAG, "Game start location missing!");
        }

        // Location request specifies how often the app gets a location from google services
        mLocationRequest = createLocationRequest(5000, 2000);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        // Calls onMapReady() when ready
        mapFragment.getMapAsync(this);

        mGeofenceList = new ArrayList<Geofence>();
        mVisitedWaypoints = new ArrayList<Waypoint>();
        mVisitedTimestamps = new ArrayList<Date>();
        mWaypointCircles = new ArrayList<Circle>();

        updateValuesFromBundle(savedInstanceState);

        populateGeofenceList(mAllWaypoints);

        // Fetch relevant sensor instances
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        accFilterWindow = new ArrayList<float[]>();
        magFilterWindow = new ArrayList<float[]>();
        rotationMatrix = new float[9];
        orientation = new float[3];

        compassUpdateHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() was called");
        if (mGoogleApiClient == null) {
            getGoogleApiClient();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() was called");
        Log.d(TAG, "," + mRequestingLocationUpdates);
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        // Create a broadcast receiver
        if (mGeofenceEventReceiver == null)
            mGeofenceEventReceiver = new GeofenceEventReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GeofenceTransitionsIntentService.ENTERED_GEOFENCE);
        intentFilter.addAction(GeofenceTransitionsIntentService.EXITED_GEOFENCE);
        registerReceiver(mGeofenceEventReceiver, intentFilter);

        // Start refreshing compass
        compassUpdating = true;
        compassUpdateHandler.post(compassUpdaterRunnable);

        if (accelometer == null) {
            Toast.makeText(this, "Accelometer not found!", Toast.LENGTH_SHORT).show();
        } else if (magnetometer == null) {
            Toast.makeText(this, "Magnetometer not found!", Toast.LENGTH_SHORT).show();
        } else {
            sManager.registerListener(this, accelometer, 40000);
            sManager.registerListener(this, magnetometer, 40000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGeofenceEventReceiver != null) {
            unregisterReceiver(mGeofenceEventReceiver);
            mGeofenceEventReceiver = null;
        }
        unregisterGeofences();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        // Stop updating compass
        compassUpdating = false;
        sManager.unregisterListener(this);
    }
    // endregion

    private synchronized void getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected LocationRequest createLocationRequest(int interval, int fastestInterval) {
        Log.d(TAG, "createLocationRequest() was called");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastestInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    // region GoogleApiClient callbacks
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected() was called");
        askLocationPermission();

        // Register geofences
        registerGeofences();

        // Start listening to location updates
        if (!mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() was called");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() was called");
    }
    // endregion

    // region Location callbacks
    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates() was called");
        mRequestingLocationUpdates = true;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates() was called");
        mRequestingLocationUpdates = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged() was called");
        mCurrentLocation = location;
        compassWidget.setPos(new LatLng(location.getLatitude(), location.getLongitude()));
        Log.d(TAG, "onLocationChanged: " + location.getLatitude() + "; " + location.getLongitude());
    }
    // endregion

    // region Map functions
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady() was called");
        mMap = googleMap;
        setMapStyle();
        setCameraBounds();
        placeWaypointMarkers();
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // For debugging only
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                centerCameraToPlayArea();
            }
        }, 1000);
    }

    private void setMapStyle() {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    private void centerCameraToPlayArea() {
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15.1f), 3000, null);
    }

    private void setCameraBounds() {
        // magic constants for bounds
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gameStartLatLng, 11.9f));
        float latDelta = 0.010f;
        float lngDelta = 0.03f;
        LatLng ne_corner = new LatLng(gameStartLatLng.latitude + latDelta, gameStartLatLng.longitude + lngDelta);
        LatLng sw_corner = new LatLng(gameStartLatLng.latitude - latDelta, gameStartLatLng.longitude - lngDelta);
        LatLngBounds gameAreaBounds = new LatLngBounds(sw_corner, ne_corner);
        mMap.setLatLngBoundsForCameraTarget(gameAreaBounds);
    }

    private void placeWaypointMarkers() {
        for (Waypoint waypoint : mAllWaypoints) {
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(waypoint.getLat(), waypoint.getLng()))
                    .radius(GEOFENCE_RADIUS)
                    .strokeWidth(8)
                    .strokeColor(Color.argb(255, 63, 81, 181))
                    .fillColor(Color.argb(127, 255, 64, 129)));
            circle.setTag(waypoint.getName());
            mWaypointCircles.add(circle);
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(waypoint.getLat(), waypoint.getLng()))
                    .title(waypoint.getName())
                    .alpha(0)
            );
        }
    }
    // endregion

    // region Geofence functions
    private void populateGeofenceList(List<Waypoint> waypointLocs) {
        for (Waypoint waypoint : waypointLocs) {
            String reqId = "FOS_" + gameCode + "_" + waypoint.getId();
            mGeofenceList.add(new Geofence.Builder()
                .setRequestId(reqId)
                .setCircularRegion(waypoint.getLat(), waypoint.getLng(), GEOFENCE_RADIUS)
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
            Log.d(TAG, "Geofence set for waypoint " + reqId);
        }
    }

    private void registerGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected to google play, cannot register new geofences", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void unregisterGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected to google play, cannot remove geofences", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    // Handle the result from adding geofences to the list of actually checked geofences
    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            //Toast.makeText(this, "Geofences succesfully modified", Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    // Helper method to log a security exception raised by a service that needs ACCESS_FINE_LOCATION
    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // Should send an enter notification if user is already in a geofence
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add list of geofences to the request
        builder.addGeofences(mGeofenceList);

        // Return the constructed request
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }
    // endregion


    private class GeofenceEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeofenceTransitionsIntentService.ENTERED_GEOFENCE)) {
                Log.d(TAG, "Activity received Entered broadcast");

                // Fetch and parse Waypoints from intent
                Bundle extras = intent.getExtras();
                ArrayList<String> ids = extras.getStringArrayList("ids");
                ArrayList<Waypoint> triggered_waypoints = new ArrayList<Waypoint>();
                for (int i = 0; i < ids.size(); i++) {
                    triggered_waypoints.add(getWaypointWithReguestId(ids.get(i)));
                }

                // Start dialog
                stampDialog = StampWaypointFragment.getInstance(triggered_waypoints.get(0));
                stampDialog.show(getSupportFragmentManager(), "stamp");

            } else if (intent.getAction().equals(GeofenceTransitionsIntentService.EXITED_GEOFENCE)) {
                Log.d(TAG, "Activity received Exited broadcast");

                // Fetch and parse Waypoints from intent
                Bundle extras = intent.getExtras();
                ArrayList<String> ids = extras.getStringArrayList("ids");
                ArrayList<Waypoint> triggered_waypoints = new ArrayList<Waypoint>();
                for (int i = 0; i < ids.size(); i++) {
                    triggered_waypoints.add(getWaypointWithReguestId(ids.get(i)));
                }

                // Close stamp dialog if it visible and not null
                if (stampDialog != null && stampDialog.getDialog() != null && stampDialog.getDialog().isShowing()) {
                    stampDialog.getDialog().dismiss();
                }

            } else {
                Log.e(TAG, "Activity received irrelevant broadcast, intent filter needs tuning");
            }
        }

        @Nullable
        private Waypoint getWaypointWithReguestId(String request_id) {
            String[] request_parts = request_id.split("_");
            int id = Integer.parseInt(request_parts[request_parts.length - 1]);

            for (Waypoint waypoint : mAllWaypoints) {
                if (waypoint.getId() == id) {
                    return waypoint;
                }
            }

            return null;
        }
    }

    // Called when button in StampWaypointFragment is clicked
    @Override
    public void onWaypointStamped(Waypoint waypoint) {
        mVisitedWaypoints.add(waypoint);
        mVisitedTimestamps.add(new Date());
        // Change waypoint's style on map
        markWaypointAsVisitedOnMap(waypoint);
        // Remove geofence from waypoint
        for (int i=0; i < mGeofenceList.size(); i++) {
            if (mGeofenceList.get(i).getRequestId().equals("FOS_"+gameCode+"_"+waypoint.getId())) {
                mGeofenceList.remove(i);    // don't do this at home kids
                break;
            }
        }
        unregisterGeofences();
        if (mGeofenceList.size() > 0) {
            registerGeofences();
        } else {
            // Game ended, go to results.
            Intent intent = new Intent(GameMapActivity.this, ResultsActivity.class);

            Bundle bundle = new Bundle();
            bundle.putSerializable("game_start_date", this.gameStartedTimestamp);
            bundle.putSerializable("waypoints", this.mAllWaypoints);
            bundle.putSerializable("visited_waypoints", this.mVisitedWaypoints);
            bundle.putSerializable("visited_timestamps", this.mVisitedTimestamps);

            intent.putExtras(bundle);
            startActivity(intent);
            GameMapActivity.this.finish();
        }
    }

    private void markWaypointAsVisitedOnMap(Waypoint waypoint) {
        // Change circle style
        for (Circle circle : mWaypointCircles) {
            if (circle.getTag().equals(waypoint.getName())) {
                circle.setRadius(GEOFENCE_RADIUS/4f);
                circle.setFillColor(Color.LTGRAY);
            }
        }
    }

    // region Compass related functions
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged() was called");
    }

    // React to acceleration and magnetometer readings
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelometer) {
            lastAcceleration = event.values;
            //Log.d("Acceleration", event.values.toString());
        }
        if (event.sensor == magnetometer) {
            lastMagnetometer = event.values;
            //Log.d("Magnetometer", event.values.toString());
        }
        // Escape if one of the sensors has yet to receive a value
        if (lastMagnetometer == null || lastAcceleration == null)
            return;
        accFilterWindow.add(lastAcceleration);
        magFilterWindow.add(lastMagnetometer);
        if (accFilterWindow.size() == COMPASS_FILTER_SIZE) {
            rotationResult = SensorManager.getRotationMatrix(rotationMatrix, null,
                    getFilteredReading(accFilterWindow), getFilteredReading(magFilterWindow));
            // rotation matrix can fail, no orientation when that happens
            if (rotationResult) {
                sManager.getOrientation(rotationMatrix, orientation);
                if (compassWidget != null) {
                    float angle = orientation[0] * (180.0f / (float) Math.PI);
                    //Log.d("Orientation", Float.toString(angle) + ":" + Float.toString(orientation[0]));
                    compassWidget.setActualRotation(angle);
                }
            }
            accFilterWindow.clear();
            magFilterWindow.clear();
        }
    }

    private float[] getFilteredReading(ArrayList<float[]> filterWindow) {
        float[] x_values = new float[COMPASS_FILTER_SIZE];
        float[] y_values = new float[COMPASS_FILTER_SIZE];
        float[] z_values = new float[COMPASS_FILTER_SIZE];
        float[] median = new float[3];

        int i = 0;
        for (float[] values : filterWindow) {
            x_values[i] = values[0];
            y_values[i] = values[1];
            z_values[i] = values[2];
            i++;
        }
        median[0] = calcMedian(x_values);
        median[1] = calcMedian(y_values);
        median[2] = calcMedian(z_values);
        return median;
    }

    private float calcMedian(float[] numArray) {
        Arrays.sort(numArray);
        float median;
        if (numArray.length % 2 == 0)
            median = ((float)numArray[numArray.length/2] + (float)numArray[numArray.length/2 - 1])/2;
        else
            median = (float) numArray[numArray.length/2];
        return median;
    }
    // endregion

    // region Save and restore app state
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putSerializable(WAYPOINTS_KEY, mAllWaypoints);
        savedInstanceState.putSerializable(VISITED_WAYPOINTS_KEY, mVisitedWaypoints);
        savedInstanceState.putSerializable(VISITED_TIMESTAMPS_KEY, mVisitedTimestamps);
        savedInstanceState.putSerializable(GAME_START_TIMESTAMP, gameStartedTimestamp);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Use this instead of onRestoreInstanceState() to get finer control of execution order
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(WAYPOINTS_KEY)) {
                mAllWaypoints = (ArrayList<Waypoint>) savedInstanceState.getSerializable(WAYPOINTS_KEY);
            }
            if (savedInstanceState.keySet().contains(VISITED_WAYPOINTS_KEY)) {
                mVisitedWaypoints = (ArrayList<Waypoint>) savedInstanceState.getSerializable(VISITED_WAYPOINTS_KEY);
            }
            if (savedInstanceState.keySet().contains(VISITED_TIMESTAMPS_KEY)) {
                mVisitedTimestamps = (ArrayList<Date>) savedInstanceState.getSerializable(VISITED_TIMESTAMPS_KEY);
            }
            if (savedInstanceState.keySet().contains(GAME_START_TIMESTAMP)) {
                gameStartedTimestamp = (Date) savedInstanceState.getSerializable(GAME_START_TIMESTAMP);
            }
        }
    }
    // endregion
}
