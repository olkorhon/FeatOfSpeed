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
import android.widget.EditText;
import android.widget.Toast;

public class ChangeNicknameFragment extends DialogFragment {
    private ChangeNickNameDialogListener listener;

    public static ChangeNicknameFragment getInstance(String nickname) {
        ChangeNicknameFragment f = new ChangeNicknameFragment();

        Bundle args = new Bundle();
        args.putString("nickname", nickname);
        f.setArguments(args);

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.fragment_change_nickname, null);
        final EditText editTextNickname = (EditText)dialogView.findViewById(R.id.editTextNickname);
        Log.d("FOS", "ChangeNicknameFragment: " + getArguments().getString("nickname", ""));
        editTextNickname.setText(getArguments().getString("nickname", ""));

        builder.setView(dialogView)
                .setTitle(R.string.change_nickname)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newNickname = editTextNickname.getText().toString();
                        if (newNickname.length() < 4) {
                            Toast.makeText(getActivity(), "Nickname too short!", Toast.LENGTH_SHORT).show();
                        } else {
                            listener.onFinishChangeNickname(newNickname);
                            getDialog().dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (ChangeNickNameDialogListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                + " must implement ChangeNickNameDialogListener.");
        }
    }

    public interface ChangeNickNameDialogListener {
        void onFinishChangeNickname(String nickname);
    }
}
