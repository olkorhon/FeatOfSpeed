package fi.semiproot.featofspeed;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LoadActivity extends AppCompatActivity {
    private static DateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private static final String TAG = LoadActivity.class.getSimpleName();

    private String from;
    // Game data:
    private String code;
    private int gameState;

    private LatLng gameLatLng;
    private String gameSize;
    private ArrayList<Player> playersList;
    private ArrayList<Waypoint> waypointList;
    private Date ISODate;

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        Log.d("FOS", "LoadActivity onCreate");

        // Get extras:
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            from = bundle.getString("from", "");
            gameLatLng = bundle.getParcelable("GAME_LAT_LNG");
            gameSize = bundle.getString("gameSize", "small");
            code = bundle.getString("code", "");
        }

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("FeetOfSpeed", Context.MODE_PRIVATE);

        playersList = new ArrayList<>();
        waypointList = new ArrayList<>();

        ISODate = null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        String url;
        JSONObject reqObject = new JSONObject();

        switch (from) {
            case "CreateGameActivity":
                url = "https://us-central1-featofspeed.cloudfunctions.net/createGame";
                try {
                    // Construct create game request
                    JSONObject location = new JSONObject();
                    location.put("latitude", gameLatLng.latitude);
                    location.put("longitude", gameLatLng.longitude);

                    JSONObject config = new JSONObject();
                    config.put("size", gameSize);
                    config.put("location", location);

                    JSONObject host = new JSONObject();
                    host.put("user_id", mAuth.getCurrentUser().getUid());
                    host.put("nickname", prefs.getString("nickname", "Anonymous"));

                    reqObject.put("config", config);
                    reqObject.put("host", host);

                } catch (JSONException ex) {
                    Log.e(TAG, "JSONException: " + ex.getMessage());
                }
                break;
            case "JoinGameActivity":
                url = "https://us-central1-featofspeed.cloudfunctions.net/joinGame?game_id=" + code;
                try {
                    // Construct Join activity request
                    JSONObject player = new JSONObject();
                    player.put("user_id", mAuth.getCurrentUser().getUid());
                    player.put("nickname", prefs.getString("nickname", "Anonymous"));


                    reqObject.put("player", player);
                } catch (JSONException ex) {
                    Log.e(TAG, "JSONException: " + ex.getMessage());
                }
                break;
            case "LobbyActivity":
                url = "https://us-central1-featofspeed.cloudfunctions.net/startGame?game_id=" + code;
                break;
            default:
                url = "";
                break;
        }

        // Construct a request that will be sent to backend functions
        Log.d(TAG, "Creating request to URL: " + url);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, reqObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Received: " + response.toString());

                try {
                    JSONArray errors = response.getJSONArray("errors");
                    if (errors != null && errors.length() > 0) {
                        Toast.makeText(LoadActivity.this, errors.getString(0), Toast.LENGTH_SHORT).show();
                        LoadActivity.this.finish();
                        return;
                    }

                    // Get game object from successful request
                    JSONObject game = response.getJSONObject("game");

                    // Fetch general information about game
                    code = game.getString("game_id");
                    gameState = game.getInt("current_state");

                    if (!game.isNull("start_time")) {
                        ISODate = ISO_FORMAT.parse(game.getString("start_time"));
                    }

                    // Fetch and list current players
                    playersList.clear();
                    JSONArray players = game.getJSONArray("players");
                    for (int i = 0; i < players.length(); i++) {
                        // Fetch player object from JSONArray
                        JSONObject player = players.getJSONObject(i);

                        // If this player is currently playing add it to the player list
                        if (player.getBoolean("currently_playing")) {
                            playersList.add(new Player(
                                    player.getString("user_id"),
                                    player.getString("nickname")));
                        }
                    }

                    // Process current gameState
                    switch (gameState) {
                        case 0:
                        case 1:
                            // Game is either just started and/or still in the lobby
                            goLobby();
                            break;
                        case 3:
                            // Game has already started, join directly
                            JSONArray waypoints = game.getJSONArray("waypoints");
                            for (int i = 0; i < waypoints.length(); i++) {
                                Waypoint waypoint = Waypoint.fromJSONObject(waypoints.getJSONObject(i));
                                if (waypoint != null)
                                    waypointList.add(waypoint);
                            }

                            Log.d(TAG, "Waypoints: " + waypointList.toString());
                            goGame();
                            break;
                        default:
                            Log.d(TAG, "GameState is not valid! Is: " + gameState);
                            break;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "An error happened while processing game response.");
                    Log.e(TAG, ex.getMessage());

                    Toast.makeText(LoadActivity.this, "Could not find any waypoints", Toast.LENGTH_LONG).show();

                    LoadActivity.this.finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Toast.makeText(getApplicationContext(),
                            "Connection timed out!",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof AuthFailureError) {
                    Log.d(TAG, "AuthFailureError");
                } else if (error instanceof ServerError || error instanceof NetworkError) {
                    NetworkResponse res = error.networkResponse;
                    if (res != null) {
                        Log.e(TAG, "Error status: " + res.statusCode);
                        try {
                            Log.e(TAG, "Error: " + new String(res.data, "UTF-8"));
                        } catch (UnsupportedEncodingException ex) {
                            Log.e(TAG, "Unsupported Encoding: " + ex.getMessage());
                        }
                    } else if (error instanceof ParseError) {
                        Log.e(TAG, "ParseError happened!");
                    }
                }
                LoadActivity.this.finish();
            }
        });

        // Add the created request to request queue
        HttpService.getInstance(this).addToRequestQueue(req);
    }

    private void goGame() {
        Intent intent = new Intent(LoadActivity.this, GameMapActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("from", from);
        bundle.putString("code", code);
        bundle.putParcelable("GAME_LAT_LNG", gameLatLng);
        bundle.putSerializable("waypoints", waypointList);
        bundle.putSerializable("date", ISODate);
        intent.putExtras(bundle);

        startActivity(intent);
        LoadActivity.this.finish();
    }

    private void goLobby() {
        Log.d(TAG, "Going to lobby");
        Intent intent = new Intent(LoadActivity.this, LobbyActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("from", from);
        bundle.putString("code", code);
        bundle.putParcelable("GAME_LAT_LNG", gameLatLng);
        bundle.putSerializable("players", playersList);
        intent.putExtras(bundle);

        startActivity(intent);
        LoadActivity.this.finish();
    }
}
