package com.shajikhan.ladspa.amprack;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;

public class HashCommands extends AlertDialog {
    public MainActivity mainActivity ;
    AutoCompleteTextView autoCompleteTextView;
    String TAG = getClass().getSimpleName();

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    protected HashCommands(Context context) {
        super(context);
        LayoutInflater inflater = getLayoutInflater();
        View iView = inflater.inflate(R.layout.hash_commands, null) ;
        setView(iView);

        AutoCompleteTextView editText = iView.findViewById(R.id.hash_command_text);
        String[] hashCommands = context.getResources().getStringArray(R.array.hash_commands);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,hashCommands);
        editText.setAdapter(adapter);
        editText.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View paramView, MotionEvent paramMotionEvent) {
                // TODO Auto-generated method stub
                editText.showDropDown();
                editText.requestFocus();
                return false;
            }
        });

        autoCompleteTextView = editText;

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
        String [] vector = command.split(":", 2);
        String cmd = vector [0] ;
        String args = "";
        if (vector.length > 1)
            args = vector [1];

        switch (cmd.toLowerCase().replaceAll(" ", "")) {
            default:
                break ;
            case "gopro":
                mainActivity.defaultSharedPreferences.edit().putBoolean("pro", true).apply();
                MainActivity.proVersion = true ;
                MainActivity.alert("Pro Version Activated", "You have been upgraded to the full version. Enjoy!");
                break;
            case "ver":
            case "version":
                String version = String.format(
                        "%s %s build %s",
                        getContext().getResources().getString(R.string.app_name),
                        getContext().getResources().getString(R.string.app_version),
                        getContext().getResources().getString(R.string.build_id)
                ) ;

                MainActivity.alert("Version", version);
                break ;
            case "logout":
                FirebaseAuth.getInstance().signOut();
                MainActivity.alert("Logged out", "You have been signed out");
                break;
            case "lastrecordedaudio":
                mainActivity.showMediaPlayerDialog();
                break ;
            case "alert":
                MainActivity.alert("Alert", args);
                break;
            case "set":
                String [] s = args.split(";");
                String what = s [0].replaceAll(" ", "");
                String to = s [1].replaceAll(" ", "");
                if (to != "false" && to != "true")
                    mainActivity.defaultSharedPreferences.edit().putString(what, to).apply();
                else
                    mainActivity.defaultSharedPreferences.edit().putBoolean(what, Boolean.parseBoolean(s[1])).apply();
                Log.d(TAG, String.format ("run: set %s to %s", what, to));
                MainActivity.alert("Preference Updated", String.format ("run: set %s to %s", what, to));
                break ;
        }
    }
}
