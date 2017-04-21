package fi.semiproot.featofspeed;

import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GameMapActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = GameMapActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 8888;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private static final String LOCATION_KEY = "location-key";

    LatLng lipasto = new LatLng(65.0593177,25.4662935);
    LatLng tokmanni = new LatLng(65.0585888, 25.4777468);
    LatLng merle = new LatLng(65.0590863, 25.4782688);
    LatLng kirjasto = new LatLng(65.061139, 25.4809759);


    private final List<LatLng> DUMMY_WAYPOINT_LOCS = Arrays.asList(lipasto, tokmanni, merle, kirjasto);

    private LatLng DUMMY_GAME_START_LATLNG = new LatLng(65.0613635, 25.4778139);
    private LatLng gameStartLatLng;
    private List<LatLng> waypointLocs = DUMMY_WAYPOINT_LOCS;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = false;

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

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Replace dummy data with location from intent
        gameStartLatLng = DUMMY_GAME_START_LATLNG;
        updateValuesFromBundle(savedInstanceState);

        // Make sure that location services provides the needed accuracy
        mLocationRequest = createLocationRequest(5000, 2000);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

    private void placeWaypointMarkers() {
        for (LatLng location : waypointLocs) {
            mMap.addCircle(new CircleOptions()
                .center(location)
                .radius(30)
                .strokeWidth(8)
                .strokeColor(Color.argb(255, 63, 81, 181))
                .fillColor(Color.argb(127, 255, 64, 129)));

        }

    }

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
}
