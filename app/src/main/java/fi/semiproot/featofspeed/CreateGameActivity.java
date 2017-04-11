package fi.semiproot.featofspeed;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class CreateGameActivity extends AppCompatActivity {
    RadioButton smallButton, mediumButton, largeButton;
    TextView description;

    static final String[] GAME_DESCRIPTIONS = {
            "Maximum of 5 waypoints in 250m radius.",
            "Maximum of 6 waypoints in 1km radius.",
            "Maximum of 6 waypoints in 2,5km radius."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        // Setup create game button:
        Button createButton = (Button)findViewById(R.id.createButton);
        createButton.setOnClickListener(new CreateButtonOnClickListener());

        // Setup custom font:
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/unispace bold.ttf");
        Typeface font2 = Typeface.createFromAsset(getAssets(), "fonts/unispace.ttf");
        TextView titleCreateGame = (TextView)findViewById(R.id.title_createGame);
        TextView textViewGameSize = (TextView)findViewById(R.id.textViewGameSize);
        TextView descriptionTitle = (TextView)findViewById(R.id.descriptionTitle);
        titleCreateGame.setTypeface(font);
        textViewGameSize.setTypeface(font2);
        descriptionTitle.setTypeface(font2);

        // Setup radio buttons:
        smallButton = (RadioButton)findViewById(R.id.radioButtonSmall);
        mediumButton = (RadioButton)findViewById(R.id.radioButtonMedium);
        largeButton = (RadioButton)findViewById(R.id.radioButtonLarge);

        RadioButtonOnClickListener rbListener = new RadioButtonOnClickListener();
        smallButton.setOnClickListener(rbListener);
        mediumButton.setOnClickListener(rbListener);
        largeButton.setOnClickListener(rbListener);

        // Setup game description text:
        description = (TextView)findViewById(R.id.descriptionText);
        description.setText(GAME_DESCRIPTIONS[1]);
    }

    private class CreateButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(CreateGameActivity.this, LoadActivity.class);
            intent.putExtra("host", true);
            String tempNum = String.valueOf((int)(Math.random() * 9999));
            if (tempNum.length() < 4) {
                tempNum = "0" + tempNum;
            }
            intent.putExtra("code", tempNum);
            startActivity(intent);
            CreateGameActivity.this.finish();
        }
    }

    private class RadioButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            boolean checked = ((RadioButton)v).isChecked();

            switch (v.getId()) {
                case R.id.radioButtonSmall:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[0]);
                    }
                    break;
                case R.id.radioButtonMedium:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[1]);
                    }
                    break;
                case R.id.radioButtonLarge:
                    if (checked) {
                        description.setText(GAME_DESCRIPTIONS[2]);
                    }
                    break;
            }
        }
    }
}
