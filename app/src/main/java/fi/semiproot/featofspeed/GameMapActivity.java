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
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GameMapActivity extends FragmentActivity implements
        SensorEventListener, ConnectionCallbacks, OnConnectionFailedListener, OnMapReadyCallback, LocationListener, ResultCallback<Status> {

    private static final String TAG = GameMapActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 8888;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private static final String LOCATION_KEY = "location-key";

    // Dummy data for testing
    LatLng lipasto = new LatLng(65.0593177,25.4662935);
    LatLng tokmanni = new LatLng(65.0585888, 25.4777468);
    LatLng merle = new LatLng(65.0590863, 25.4782688);
    LatLng kirjasto = new LatLng(65.061139, 25.4809759);
    LatLng mumina = new LatLng(65.061129, 25.48029);

    private final List<LatLng> DUMMY_WAYPOINT_LOCS = Arrays.asList(lipasto, tokmanni, merle, kirjasto, mumina);
    private LatLng DUMMY_GAME_START_LATLNG = new LatLng(65.0613635, 25.4778139);
    // END dummy data

    // Map and location fields
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

    // Geofence fields
    private final int GEOFENCE_RADIUS = 100;             // Meters
    private final int GEOFENCE_EXPIRATION = 3600000;    // Hour in ms
    protected ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    // Broadcast receiver
    GeofenceEventReceiver mGeofenceEventReceiver;

    // Game related fields
    private LatLng gameStartLatLng;
    private List<LatLng> waypointLocs = DUMMY_WAYPOINT_LOCS;

    // Compass functionality
    CompassView compassWidget;

    Sensor accelometer;
    Sensor magnetometer;
    SensorManager sManager;

    float[] lastAcceleration;
    float[] lastMagnetometer;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_map);

        // Get an instance of GoogleAPIClient.
        mGoogleApiClient = getGoogleApiClient();

        // Replace dummy data with location from intent
        gameStartLatLng = DUMMY_GAME_START_LATLNG;
        updateValuesFromBundle(savedInstanceState);

        // Location request specifies how often the app gets a location from google services
        mLocationRequest = createLocationRequest(5000, 2000);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGeofenceList = new ArrayList<Geofence>();
        populateGeofenceList(waypointLocs);

        // Fetch relevant sensor instances
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Fetch compass view instance from content
        compassWidget = (CompassView) findViewById(R.id.compass);

        rotationMatrix = new float[9];
        orientation = new float[3];

        compassUpdateHandler = new Handler();
    }

    private void populateGeofenceList(List<LatLng> waypointLocs) {
        for (LatLng location : waypointLocs) {
            String latStr = String.valueOf(location.latitude);
            String lngStr = String.valueOf(location.longitude);
            String reqId = latStr.substring(latStr.length() - 4) + lngStr.substring(lngStr.length() - 4);
            Log.d(TAG, "Request id for location " + location.toString() + "is " + reqId);
            mGeofenceList.add(new Geofence.Builder()
                .setRequestId(reqId)
                .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
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

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    private synchronized GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            return new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        } else return mGoogleApiClient;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart() was called");
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        super.onResume();
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
            sManager.registerListener(this, accelometer, 60000);
            sManager.registerListener(this, magnetometer, 60000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Release and unregister listener
        if (mGeofenceEventReceiver != null) {
            unregisterReceiver(mGeofenceEventReceiver);
            mGeofenceEventReceiver = null;
        }

        // Remove geofences
        unregisterGeofences();

        stopLocationUpdates();


    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        // Stop updating compass
        compassUpdating = false;
        sManager.unregisterListener(this);
    }

    // Triggered when the map is ready to be used
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady() was called");
        mMap = googleMap;
        centerCameraToPlayArea();
        setMapStyle();
        setCameraBounds();
        placeWaypointMarkers();
    }

    private void setMapStyle() {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    private void centerCameraToPlayArea() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gameStartLatLng, 13));
    }

    private void setCameraBounds() {
        // magic constants for bounds
        float latDelta = 0.010f;
        float lngDelta = 0.03f;
        LatLng ne_corner = new LatLng(gameStartLatLng.latitude + latDelta, gameStartLatLng.longitude + lngDelta);
        LatLng sw_corner = new LatLng(gameStartLatLng.latitude - latDelta, gameStartLatLng.longitude - lngDelta);
        LatLngBounds gameAreaBounds = new LatLngBounds(sw_corner, ne_corner);
        mMap.setLatLngBoundsForCameraTarget(gameAreaBounds);
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
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void placeWaypointMarkers() {
        for (LatLng location : waypointLocs) {
            mMap.addCircle(new CircleOptions()
                .center(location)
                .radius(GEOFENCE_RADIUS)
                .strokeWidth(8)
                .strokeColor(Color.argb(255, 63, 81, 181))
                .fillColor(Color.argb(127, 255, 64, 129)));
        }
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

        rotationResult = sManager.getRotationMatrix(rotationMatrix, null, lastAcceleration, lastMagnetometer);

        // rotation matrix can fail, no orientation when that happens
        if (rotationResult) {
            sManager.getOrientation(rotationMatrix, orientation);

            if (compassWidget != null) {

                float angle = orientation[0] * (180.0f / (float) Math.PI);
                //Log.d("Orientation", Float.toString(angle) + ":" + Float.toString(orientation[0]));
                compassWidget.setActualRotation(angle);
            }
        }
    }

    // GoogleApiClient.ConnectionCallbacks
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged() was called");
    }

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
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() was called");
    }
    // END GoogleApiClient.ConnectionCallbacks

    // GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() was called");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged() was called");
        mCurrentLocation = location;
        // UpdateUI();
        Log.d(TAG, "onLocationChanged: " + location.getLatitude() + "; " + location.getLongitude());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Handle the result from adding geofences to the list of actually checked geofences
    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Toast.makeText(this, "Geofences succesfully modified", Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    private class GeofenceEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeofenceTransitionsIntentService.ENTERED_GEOFENCE)) {
                Log.d(TAG, "Activity received Entered broadcast");
            } else if (intent.getAction().equals(GeofenceTransitionsIntentService.EXITED_GEOFENCE)) {
                Log.d(TAG, "Activity received Exited broadcast");
            } else {
                Log.e(TAG, "Activity received irrelevant broadcast, intent filter needs tuning");
            }
        }
    }
}
