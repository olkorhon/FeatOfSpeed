package fi.semiproot.featofspeed;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class LobbyActivity extends AppCompatActivity {
    private static String TAG = "FOS_LobbyActivity";

    private boolean isHost;
    private String code;
    private ArrayList<Player> players;
    private ListView playerListView;
    private PlayerAdapter pAdapter;
    Button button;
    TextView textViewPlayerCount;

    private ArrayList<Waypoint> waypoints;

    FirebaseDatabase database;
    DatabaseReference playerReference;
    DatabaseReference waypointReference;
    DatabaseReference stateReference;
    private ValueEventListener playerListener;
    private ValueEventListener waypointListener;
    private ValueEventListener stateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        database = FirebaseDatabase.getInstance();

        // Get extras:
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Log.d(TAG, "EXTRA:" + bundle.getString("from", ""));
            isHost = (bundle.getString("from", "").equals("CreateGameActivity"));
            code = bundle.getString("code", "0000");
            players = (ArrayList<Player>) bundle.getSerializable("players");
        }

        // Setup custom font:
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/unispace bold.ttf");
        TextView textViewGameId = (TextView)findViewById(R.id.textViewGameId);
        TextView textViewGameIdTitle = (TextView)findViewById(R.id.textViewGameIdTitle);
        TextView textViewPlayers = (TextView)findViewById(R.id.textViewPlayers);
        textViewPlayerCount = (TextView)findViewById(R.id.textViewPlayerCount);
        textViewGameId.setTypeface(font);
        textViewGameId.setText(code);
        textViewGameIdTitle.setTypeface(font);
        textViewPlayers.setTypeface(font);
        textViewPlayerCount.setTypeface(font);
        textViewPlayerCount.setText(String.valueOf(players.size()));

        // Button
        button = (Button)findViewById(R.id.lobbyButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setEnabled(false);
                Intent intent = new Intent(LobbyActivity.this, LoadActivity.class);
                intent.putExtra("code", code);
                intent.putExtra("from", "LobbyActivity");
                startActivity(intent);
                LobbyActivity.this.finish();
            }
        });

        // Hide start until waypoints are found
        button.setVisibility(View.GONE);

        playerListView = (ListView)findViewById(R.id.playerListView);

        pAdapter = new PlayerAdapter();
        playerListView.setAdapter(pAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (code != null) {
            // Link to database updates that happen to this game
            playerReference = database.getReference("games/" + code + "/players");
            waypointReference = database.getReference("games/" + code + "/waypoints");
            stateReference = database.getReference("games/" + code + "/current_state");

            playerListener = playerReference.addValueEventListener(new ValueEventListener() { // Read from the database
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again whenever data at this location is updated.
                    String value = dataSnapshot.toString();
                    Log.d(TAG, "Players is now: " + value);

                    // Fetch players
                    List<Object> players_snapshot = (List<Object>) (dataSnapshot.getValue());

                    // Clear old players and add new ones
                    players.clear();
                    for (int i = 0; i < players_snapshot.size(); i++) {
                        // We know what is in the list so we can cast the player_history_list element to a Map<String, Object>
                        Map<String, Object> playerDataMap = (Map<String, Object>) players_snapshot.get(i);

                        // Try to find a matching id from the list of current players
                        if (playerDataMap.get("currently_playing").equals(true)) {
                            players.add(new Player((String)playerDataMap.get("user_id"), (String)playerDataMap.get("nickname")));
                        }
                    }

                    pAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });

            waypointListener = waypointReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again whenever data at this location is updated.
                    String value = dataSnapshot.toString();
                    Log.d(TAG, "Waypoints changed to: " + value);

                    // Fetch new waypoints
                    List<Object> waypoints_snapshot = (List<Object>) dataSnapshot.getValue();

                    if (waypoints_snapshot != null)
                    {
                        if (waypoints_snapshot.size() > 0) {
                            waypoints = new ArrayList<>();

                            // Fetch waypoints
                            for (Object obj : waypoints_snapshot) {
                                Map<String, Object> waypoint = (Map<String, Object>) obj;
                                waypoints.add(Waypoint.fromMap(waypoint));
                            }

                            // Show start button
                            if (isHost) {
                                button.setVisibility(View.VISIBLE);
                            }
                        }
                        else {

                        }
                    } else {
                        Log.d(TAG, "S");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            // Listen to gamestate change
            stateListener = stateReference.addValueEventListener(new ValueEventListener() { // Read from the database
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again whenever data at this location is updated.
                    String value = dataSnapshot.toString();
                    Log.d(TAG, "Current state is now: " + value);

                    // Fetch players
                    int state = ((Long)dataSnapshot.getValue()).intValue();

                    if (state == 3) {
                        if (waypoints != null) {
                            Intent intent = new Intent(LobbyActivity.this, GameMapActivity.class);

                            Bundle bundle = new Bundle();
                            bundle.putString("code", "LobbyActivity");
                            bundle.putSerializable("waypoints", waypoints);
                            intent.putExtras(bundle);

                            startActivity(intent);
                            LobbyActivity.this.finish();
                        }
                        else {
                            Log.d(TAG, "No waypoints! There is no way to start the game");
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister listeners
        playerReference.removeEventListener(playerListener);
        waypointReference.removeEventListener(waypointListener);
        stateReference.removeEventListener(stateListener);
    }

    private class PlayerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return LobbyActivity.this.players.size();
        }

        @Override
        public Object getItem(int i) {
            return LobbyActivity.this.players.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(LobbyActivity.this)
                        .inflate(R.layout.player_listview_item, viewGroup, false);
            }

            String player = ((Player)getItem(i)).getNickName();

            TextView playerName = (TextView)view.findViewById(R.id.playerName);
            playerName.setText(player);

            textViewPlayerCount.setText(String.valueOf(LobbyActivity.this.players.size()));

            return view;
        }
    }
}
