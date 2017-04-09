package fi.semiproot.featofspeed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class GameMapActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap mMap;


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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Fetch relevant sensor instances
        sManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelometer = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Fetch compass view instance from content
        compassWidget = (CompassView)findViewById(R.id.compass);

        rotationMatrix = new float[9];
        orientation = new float[3];

        compassUpdateHandler = new Handler();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Start refreshing compass
        compassUpdating = true;
        compassUpdateHandler.post(compassUpdaterRunnable);

        if (accelometer == null)
        {
            Toast.makeText(this, "Accelometer not found!", Toast.LENGTH_SHORT).show();
        }
        else if (magnetometer == null)
        {
            Toast.makeText(this, "Magnetometer not found!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            sManager.registerListener(this, accelometer, 60000);
            sManager.registerListener(this, magnetometer, 60000);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // Stop updating compass
        compassUpdating = false;
        sManager.unregisterListener(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        float zoomLevel = 10.0f; //This goes up to 21
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoomLevel));
    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
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

                float angle = orientation[0] * (180.0f / (float)Math.PI);
                //Log.d("Orientation", Float.toString(angle) + ":" + Float.toString(orientation[0]));
                compassWidget.setActualRotation(angle);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
