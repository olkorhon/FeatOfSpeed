package fi.semiproot.featofspeed;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class CreateGameActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = CreateGameActivity.class.getSimpleName();

    static final String[] GAME_DESCRIPTIONS = {
            "Maximum of 5 waypoints in 250m radius.",
            "Maximum of 6 waypoints in 1km radius.",
            "Maximum of 6 waypoints in 2,5km radius."
    };

    Button createButton;
    RadioButton smallButton, mediumButton, largeButton;
    TextView description;

    private GoogleApiClient mGoogleApiClient;
    private Location mLatestLocation;
    private LatLng mLatestLatLng;
    private String selectedGameSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Setup create game button:
        createButton = (Button)findViewById(R.id.createButton);
        createButton.setOnClickListener(new CreateButtonOnClickListener());
        createButton.setEnabled(false);

        // Setup custom font:
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/unispace bold.ttf");
        Typeface font2 = Typeface.createFromAsset(getAssets(), "fonts/unispace.ttf");
        TextView titleCreateGame = (TextView)findViewById(R.id.title_createGame);
        TextView textViewGameSize = (TextView)findViewById(R.id.textViewGameSize);
        TextView descriptionTitle = (TextView)findViewById(R.id.descriptionTitle);
        titleCreateGame.setTypeface(font);
        textViewGameSize.setTypeface(font2);
        descriptionTitle.setTypeface(font2);

        // Setup radio buttons:
        smallButton = (RadioButton)findViewById(R.id.radioButtonSmall);
        mediumButton = (RadioButton)findViewById(R.id.radioButtonMedium);
        largeButton = (RadioButton)findViewById(R.id.radioButtonLarge);

        RadioButtonOnClickListener rbListener = new RadioButtonOnClickListener();
        smallButton.setOnClickListener(rbListener);
        mediumButton.setOnClickListener(rbListener);
        largeButton.setOnClickListener(rbListener);

        // Setup game description text:
        description = (TextView)findViewById(R.id.descriptionText);
        description.setText(GAME_DESCRIPTIONS[1]);
        selectedGameSize = "small";
    }

    private void updateLatestLocation() {
        if (mGoogleApiClient != null) {
            try {
                mLatestLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                mLatestLatLng = new LatLng(mLatestLocation.getLatitude(), mLatestLocation.getLongitude());
                Log.d(TAG, "Location: " + mLatestLatLng.toString());
                createButton.setEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
            }
            catch (NullPointerException e) {
                Toast.makeText(this, "Could not fetch initial location, cannot start", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
            }
        }
    }

    private boolean locationPermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private class CreateButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(CreateGameActivity.this, LoadActivity.class);
            intent.putExtra("host", true);
            intent.putExtra("GAME_LAT_LNG", mLatestLatLng);
            intent.putExtra("gameSize", selectedGameSize);
            startActivity(intent);
            CreateGameActivity.this.finish();
        }
    }

    private class RadioButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            boolean checked = ((RadioButton)v).isChecked();

            switch (v.getId()) {
                case R.id.radioButtonSmall:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[0]);
                        selectedGameSize = "small";
                    }
                    break;
                case R.id.radioButtonMedium:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[1]);
                        selectedGameSize = "medium";
                    }
                    break;
                case R.id.radioButtonLarge:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[2]);
                        selectedGameSize = "large";
                    }
                    break;
            }
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        updateLatestLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
