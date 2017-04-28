package fi.semiproot.featofspeed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements ChangeNicknameFragment.ChangeNickNameDialogListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CHECK_SETTINGS = 9999;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 8888;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLatestLocation;

    private SharedPreferences prefs;

    private TextView textViewNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button createGameButton = (Button)findViewById(R.id.createGameButton);
        createGameButton.setOnClickListener(new CreateGameButtonListener());

        Button joinGameButton = (Button)findViewById(R.id.joinGameButton);
        joinGameButton.setOnClickListener(new JoinGameButtonListener());

        Button buttonChangeNickname = (Button)findViewById(R.id.buttonChangeNickname);
        buttonChangeNickname.setOnClickListener(new ChangeNicknameButtonListener());

        prefs = getSharedPreferences("FeetOfSpeed", Context.MODE_PRIVATE);

        final TextView loginStatusLabel = (TextView) findViewById(R.id.loginStatusLabel);
        textViewNickname = (TextView)findViewById(R.id.textViewNickname);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    String displayName = prefs.getString("nickname", user.getDisplayName());
                    if (displayName == null) {
                        displayName = "Guest_" + user.getUid().substring(0, 5);
                    }

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("nickname", displayName);
                    editor.apply();

                    textViewNickname.setText(prefs.getString("nickname", ""));

                    String userUid = user.getUid();
                    Log.d(TAG, "onAuthStateChanged:signed_in: " + userUid);
                    loginStatusLabel.setText(getString(R.string.signed_in_name_label));
                    Log.d(TAG, "onAuthStateChanged:signed_in displayName" + displayName);
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    loginStatusLabel.setText(getString(R.string.signed_out_name_label));
                }
            }
        };

        mAuth.signInAnonymously()
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    Log.d(TAG, "signInAnonymously:onComplete: " + task.isSuccessful());
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "signInAnonymously", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication successful.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        mLocationRequest = createLocationRequest(10000, 5000);
        mGoogleApiClient = buildGoogleApiClient();
    }

    private class CreateGameButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, CreateGameActivity.class);
            startActivity(intent);
        }
    }

    private class JoinGameButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, JoinGameActivity.class);
            startActivity(intent);
        }
    }

    private class ChangeNicknameButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            DialogFragment newFragment = ChangeNicknameFragment.getInstance(prefs.getString("nickname", ""));
            newFragment.show(getSupportFragmentManager(), "nickname");
        }
    }

    protected synchronized GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected LocationRequest createLocationRequest(int interval, int fastestInterval) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastestInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void checkLocationSettings() {
        LocationSettingsRequest locationSettings = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest).build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, locationSettings);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissions();
        }
    }

    private void askForLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permissions granted");
            } else {
                Toast.makeText(MainActivity.this, "Game needs location to work :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onFinishChangeNickname(String nickname) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nickname", nickname);
        editor.apply();
        textViewNickname.setText(nickname);
    }

    // Activity lifecycle callbacks
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }
    // END Activity lifecycle callbacks

    // GoogleApiClient callbacks
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Make sure that settings provides the needed accuracy
        checkLocationSettings();
        checkLocationPermission();
        Log.d(TAG, "GoogleApiClient connected!");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed.");
    }
    // END GoogleApiClient callbacks
}
