package fi.semiproot.featofspeed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class CreateGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        Spinner gameSizeSpinner = (Spinner)findViewById(R.id.gameSizeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.game_sizes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSizeSpinner.setAdapter(adapter);

        Button createButton = (Button)findViewById(R.id.createButton);
        createButton.setOnClickListener(new CreateButtonOnClickListener());
    }

    private class CreateButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(CreateGameActivity.this, LobbyActivity.class);
            startActivity(intent);
            CreateGameActivity.this.finish();
        }
    }
}
