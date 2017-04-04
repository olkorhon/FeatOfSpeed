package fi.semiproot.featofspeed;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class LobbyActivity extends AppCompatActivity {

    private ListView playerListView;
    private PlayerAdapter pAdapter;
    protected List<String> players;

    private static final List<String> PLAYERS = Arrays.asList("Pekka Pekkala",
            "Heikki Heikkilä", "Jonne Jonnela", "Iiro Iirola", "Henna Hennala",
            "Taina Tainala", "Jukka Jukkola");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        players = PLAYERS;

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

            String player = (String)getItem(i);

            TextView playerName = (TextView)view.findViewById(R.id.playerName);
            playerName.setText(player);

            return view;
        }
    }
}
