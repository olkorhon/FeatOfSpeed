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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Initiialize firebase connection
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Get extras:
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            isHost = bundle.getBoolean("host", false);
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
                Intent intent = new Intent(LobbyActivity.this, GameMapActivity.class);
                startActivity(intent);
                LobbyActivity.this.finish();
            }
        });
        if (isHost) {
        } else {
            button.setVisibility(View.GONE);
        }

        playerListView = (ListView)findViewById(R.id.playerListView);

        pAdapter = new PlayerAdapter();
        playerListView.setAdapter(pAdapter);


        if (code != null) {
            // Link to database updates that happen to this game
            DatabaseReference playerRef = database.getReference("games/" + code + "/players");
            DatabaseReference stateRef = database.getReference("games/" + code + "/current_state");

            playerRef.addValueEventListener(new ValueEventListener() { // Read from the database
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again whenever data at this location is updated.
                    String value = dataSnapshot.toString();
                    Log.d(TAG, "Value is: " + value);

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


            // Listen to gamestate change

        }




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
