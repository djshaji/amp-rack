package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.preference.PreferenceManager;

public class HashCommands extends AlertDialog {
    protected HashCommands(Context context) {
        super(context);
        LayoutInflater inflater = getLayoutInflater();
        View iView = inflater.inflate(R.layout.hash_commands, null) ;
        setView(iView);

        EditText editText = iView.findViewById(R.id.hash_command_text);

        Button button = iView.findViewById(R.id.hash_run);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                run (editText.getText().toString());
                dismiss();
            }
        });
    }

    void run (String command) {
        switch (command) {
            default:
                break ;
            case "gopro":
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                sharedPreferences.edit().putBoolean("pro", true).apply();
                MainActivity.proVersion = true ;
                MainActivity.toast("You have been upgraded to the full version. Enjoy!");
                break;
        }
    }
}
