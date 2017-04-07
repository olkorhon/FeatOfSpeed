package fi.semiproot.featofspeed;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LoadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(LoadActivity.this, LobbyActivity.class);
        startActivity(intent);
        LoadActivity.this.finish();
    }
}
