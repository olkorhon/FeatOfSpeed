package fi.semiproot.featofspeed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ResultsActivity extends AppCompatActivity {

    private static final String TAG = ResultsActivity.class.getSimpleName();

    private ArrayList<Waypoint> mAllWaypoints;
    private ArrayList<Waypoint> mVisitedWaypoints;
    private ArrayList<Date> mVisitedTimestamps;
    private Date gameStartDate;

    private ResultsButtonListener resultsButtonListener;

    private ListView waypointsListView;
    private WaypointsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            this.gameStartDate = (Date) bundle.getSerializable("game_start_date");
            this.mAllWaypoints = (ArrayList<Waypoint>) bundle.getSerializable("waypoints");
            this.mVisitedWaypoints = (ArrayList<Waypoint>) bundle.getSerializable("visited_waypoints");
            this.mVisitedTimestamps = (ArrayList<Date>) bundle.getSerializable("visited_timestamps");
        }

        resultsButtonListener = new ResultsButtonListener();

        Button resultsButton = (Button) findViewById(R.id.resultsButton);
        resultsButton.setOnClickListener(resultsButtonListener);

        TextView waypointsTextView = (TextView) findViewById(R.id.textViewWaypoints);
        String result = String.valueOf(this.mVisitedWaypoints.size()) + "/" +
                String.valueOf(this.mAllWaypoints.size());
        waypointsTextView.setText(result);

        // Get game duration:
        Date lastTimestamp = this.mVisitedTimestamps.get(this.mVisitedTimestamps.size() - 1);
        long duration = lastTimestamp.getTime() - this.gameStartDate.getTime();
        long diffinMillisecs = (duration % 1000) / 100;
        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);

        TextView gameTimestampTextView = (TextView) findViewById(R.id.gameTimestampTextView);
        gameTimestampTextView.setText(  String.format("%02d", diffInHours) + ":" +
                                        String.format("%02d", diffInMinutes) + ":" +
                                        String.format("%02d", diffInSeconds));

        TextView gameTimestampMSTextView = (TextView) findViewById(R.id.gameTimestampMSTextView);
        gameTimestampMSTextView.setText("," + String.format("%02d", diffinMillisecs));

        waypointsListView = (ListView) findViewById(R.id.waypointsListView);
        mAdapter = new WaypointsAdapter();
        waypointsListView.setAdapter(mAdapter);
    }

    private class ResultsButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            ResultsActivity.this.finish();
        }
    }

    private class WaypointsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ResultsActivity.this.mAllWaypoints.size();
        }

        @Override
        public Object getItem(int position) {
            return ResultsActivity.this.mAllWaypoints.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ResultsActivity.this)
                        .inflate(R.layout.waypoints_listview_item, parent, false);

            }

            Waypoint waypoint = ((Waypoint) getItem(position));
            String name = waypoint.getName();

            if (name.length() > 15) {
                name = name.substring(0, 15);
            }

            TextView waypointNameTextView = (TextView) convertView.findViewById(R.id.waypointNameTextView);
            waypointNameTextView.setText(name);

            TextView waypointTimestampWhole = (TextView) convertView.findViewById(R.id.waypointTimestampWhole);
            TextView waypointTimestamp = (TextView) convertView.findViewById(R.id.waypointTimestampTextView);

            if (position <= ResultsActivity.this.mVisitedTimestamps.size() - 1) {
                Date timestamp = ResultsActivity.this.mVisitedTimestamps.get(position);
                long duration = timestamp.getTime() - ResultsActivity.this.gameStartDate.getTime();
                long diffinMillisecs = (duration % 1000) / 100;
                long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);


                waypointTimestampWhole.setText(
                        String.format("%02d", diffInHours) + ":" +
                        String.format("%02d", diffInMinutes) + ":" +
                        String.format("%02d", diffInSeconds) + "," +
                        String.format("%02d", diffinMillisecs));

                if (position > 0) {
                    Date lastTimestamp = ResultsActivity.this.mVisitedTimestamps.get(position - 1);
                    duration =  timestamp.getTime() - lastTimestamp.getTime();
                }

                diffinMillisecs = (duration % 1000) / 100;
                diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
                diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                diffInHours = TimeUnit.MILLISECONDS.toHours(duration);

                waypointTimestamp.setText(
                        String.format("%02d", diffInHours) + ":" +
                        String.format("%02d", diffInMinutes) + ":" +
                        String.format("%02d", diffInSeconds) + "," +
                        String.format("%02d", diffinMillisecs));

            } else {
                waypointTimestampWhole.setText("");
                waypointTimestamp.setText("");
            }

            return convertView;
        }
    }
}
