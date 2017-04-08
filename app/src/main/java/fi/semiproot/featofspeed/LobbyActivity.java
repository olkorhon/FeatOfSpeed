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

import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;

public class LobbyActivity extends AppCompatActivity {

    private boolean isHost;
    private String code;
    private ListView playerListView;
    private PlayerAdapter pAdapter;
    Button button;
    TextView readyCountTextView;
    List<String> players;

    private static final List<String> PLAYERS = Arrays.asList("Pekka Pekkala",
            "Heikki Heikkilä", "Jonne Jonnela", "Iiro Iirola", "Henna Hennala",
            "Taina Tainala", "Jukka Jukkola");

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
        }

        // Setup custom font:
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/unispace bold.ttf");
        TextView textViewGameId = (TextView)findViewById(R.id.textViewGameId);
        TextView textViewPlayers = (TextView)findViewById(R.id.textViewPlayers);
        TextView textViewReady = (TextView)findViewById(R.id.textViewReady);
        textViewGameId.setTypeface(font);
        textViewGameId.setText(code);
        textViewPlayers.setTypeface(font);
        textViewReady.setTypeface(font);

        // Button
        button = (Button)findViewById(R.id.lobbyButton);
        if (isHost) {
            button.setText("Start");
            button.setEnabled(false); // can the host start the game without everyone ready?
        } else {
            button.setText("Ready!");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button.setEnabled(false);
                    Intent intent = new Intent(LobbyActivity.this, GameMapActivity.class);
                    startActivity(intent);
                    LobbyActivity.this.finish();
                }
            });
        }

        players = PLAYERS;

        readyCountTextView = (TextView)findViewById(R.id.textViewReadyCount);
        readyCountTextView.setText("0/" + String.valueOf(players.size()));
        playerListView = (ListView)findViewById(R.id.playerListView);

        pAdapter = new PlayerAdapter();
        playerListView.setAdapter(pAdapter);
    }

    private class PlayerAdapter extends BaseAdapter {
        private int checkedCount;

        public PlayerAdapter() {
            super();
            checkedCount = 0;
        }

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

            String player = (String)getItem(i);

            TextView playerName = (TextView)view.findViewById(R.id.playerName);
            playerName.setText(player);

            CheckBox readyBox = (CheckBox)view.findViewById(R.id.checkBoxReady);
            readyBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        checkedCount++;
                    } else {
                        checkedCount--;
                    }
                    String text = String.valueOf(checkedCount) + "/" +
                            String.valueOf(LobbyActivity.this.players.size());
                    readyCountTextView.setText(text);

                    if (isHost && checkedCount == LobbyActivity.this.players.size()) {
                        button.setEnabled(true);
                    }
                }
            });

            return view;
        }
    }
}
