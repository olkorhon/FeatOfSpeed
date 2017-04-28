package fi.semiproot.featofspeed;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StampWaypointFragment extends DialogFragment {
    private StampWaypointDialogListener listener;

    public static StampWaypointFragment getInstance(Waypoint waypoint) {
        StampWaypointFragment f = new StampWaypointFragment();

        Bundle args = new Bundle();
        args.putSerializable("waypoint", waypoint);
        f.setArguments(args);

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.fragment_stamp_waypoint, null);

        final Waypoint waypoint = (Waypoint)getArguments().getSerializable("waypoint");

        final TextView waypointNameTextView = (TextView)dialogView.findViewById(R.id.waypointName);
        String name = waypoint.getName();
        waypointNameTextView.setText(name);

        final Button punchButton = (Button)dialogView.findViewById(R.id.punchButton);
        punchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onWaypointStamped(waypoint);
                getDialog().dismiss();
            }
        });

        builder.setView(dialogView);

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (StampWaypointDialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement StampWaypointDialogListener.");
        }
    }

    public interface StampWaypointDialogListener {
        void onWaypointStamped(Waypoint waypoint);
    }
}
