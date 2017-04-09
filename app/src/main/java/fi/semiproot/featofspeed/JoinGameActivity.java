package fi.semiproot.featofspeed;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JoinGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        // Setup custom font:
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/unispace bold.ttf");
        Typeface font2 = Typeface.createFromAsset(getAssets(), "fonts/unispace.ttf");
        TextView titleJoinGame = (TextView)findViewById(R.id.title_joinGame);
        TextView textViewGameCode = (TextView)findViewById(R.id.textViewGameCode);
        final EditText editTextCode = (EditText)findViewById(R.id.editTextCode);
        titleJoinGame.setTypeface(font);
        textViewGameCode.setTypeface(font2);
        editTextCode.setTypeface(font);

        // Join Button
        Button joinButton = (Button) findViewById(R.id.joinButton);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JoinGameActivity.this, LoadActivity.class);
                String code = editTextCode.getText().toString();
                int max = 4 - code.length();
                for (int i = 0; i < max; i++) {
                    code = "0" + code;
                }
                intent.putExtra("code", code);
                startActivity(intent);
                JoinGameActivity.this.finish();
            }
        });


    }
}
