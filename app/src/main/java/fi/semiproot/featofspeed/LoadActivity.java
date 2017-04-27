package fi.semiproot.featofspeed;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
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
import java.util.ArrayList;

public class LoadActivity extends AppCompatActivity {

    private static final String TAG = LoadActivity.class.getSimpleName();

    // Game data:
    private boolean isHost;
    private String code;
    private LatLng gameLatLng;
    private String gameSize;
    private ArrayList<Player> playersList;

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
            isHost = bundle.getBoolean("host", false);
            gameLatLng = bundle.getParcelable("GAME_LAT_LNG");
            gameSize = bundle.getString("gameSize", "small");
            code = bundle.getString("code", "");
        }

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("FeetOfSpeed", Context.MODE_PRIVATE);

        playersList = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();

        String url;
        JSONObject reqObject = new JSONObject();

        if (isHost) {
            url = "https://us-central1-featofspeed.cloudfunctions.net/createGame";

            try {
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
            }
        } else {
            url = "https://us-central1-featofspeed.cloudfunctions.net/joinGame?game_id=" + code;

            try {
                JSONObject player = new JSONObject();
                player.put("user_id", mAuth.getCurrentUser().getUid());
                player.put("nickname", prefs.getString("nickname", "Anonymous"));

                reqObject.put("player", player);
            } catch (JSONException ex) {
            }
        }

        Log.d(TAG, "URL: " + url);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, reqObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());

                try {
                    JSONArray errors = response.getJSONArray("errors");
                    if (errors != null && errors.length() > 0) {
                        Toast.makeText(LoadActivity.this, errors.getString(0), Toast.LENGTH_SHORT).show();
                        LoadActivity.this.finish();
                    } else {
                        JSONObject game = response.getJSONObject("game");
                        code = String.valueOf(game.getInt("game_id"));
                        int max = 4 - code.length();
                        for (int i = 0; i < max; i++) {
                            code = "0" + code;
                        }
                        Log.d(TAG, "GameCode: " + code);

                        JSONArray players = game.getJSONArray("players");
                        if (players != null) {
                            for (int i = 0; i < players.length(); i++) {
                                //JSONObject player = players.getJSONObject(i);
                                playersList.add(new Player(
                                        players.getString(i), "Name"));//.getString("user_id"),
                                        //player.getString("nickname")));
                            }
                        }


                        goLobby();
                    }
                } catch (Exception ex) {
                    LoadActivity.this.finish();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse res = error.networkResponse;
                if (res != null) {
                    Log.d(TAG, "Error status: " + res.statusCode);
                    try {
                        Log.d(TAG, "Error: " + new String(res.data, "UTF-8"));
                    } catch (UnsupportedEncodingException ex) {

                    }
                }
                LoadActivity.this.finish();
            }
        });

        HttpService.getInstance(this).addToRequestQueue(req);
    }

    private void goLobby() {
        Log.d(TAG, "Going to lobby");
        Intent intent = new Intent(LoadActivity.this, LobbyActivity.class);

        Bundle bundle = new Bundle();
        bundle.putBoolean("host", isHost);
        bundle.putString("code", code);
        bundle.putSerializable("players", playersList);
        intent.putExtras(bundle);

        startActivity(intent);
        LoadActivity.this.finish();
    }
}
