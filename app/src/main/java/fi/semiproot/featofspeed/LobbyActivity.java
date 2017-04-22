package fi.semiproot.featofspeed;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class LobbyActivity extends AppCompatActivity {

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
