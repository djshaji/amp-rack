package com.shajikhan.ladspa.amprack;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import io.grpc.internal.JsonParser;

public class ConnectGuitar extends AppCompatActivity {
    Context context ;
    String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_guitar);
        context = this ;
        JSONObject jsonObject = loadJSONFromAsset() ;
        getSupportActionBar().hide();
        MainActivity.applyWallpaper(context, getWindow(),getResources(), findViewById(R.id.bg), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
        if (jsonObject == null) {
            new AlertDialog.Builder(context)
                    .setTitle("Unable to parse config file")
                    .setMessage("There is a problem with the build. Please update or report to developer!")
                    .setNegativeButton(android.R.string.ok, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        LinearLayout layout = findViewById(R.id.layout);
        Iterator<String> keys = jsonObject.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                if (jsonObject.get(key) instanceof JSONObject) {
                    Log.d(TAG, "onCreate: key " + key);
                    JSONObject object = jsonObject.getJSONObject(key);
                    // do something with jsonObject here
                    String icon = object.getString("icon");
                    String desc = object.getString("description");
                    String title = object.getString("title");

                    LinearLayout layout1 = (LinearLayout) getLayoutInflater().inflate(R.layout.connect_tips, null);
                    layout.addView(layout1);

                    ImageView logo = (ImageView)layout1.findViewById(R.id.icon) ;
                    logo.setImageDrawable(getIcon(icon));
//                    logo.setImageDrawable(getResources().getDrawable(icon));

                    ((TextView) layout1.findViewById(R.id.name)).setText(title);
                    ((TextView) layout1.findViewById(R.id.desc)).setText(desc);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "onCreate: " + String.format(
                "otg: %d\tcable: %d\tinterface: %d\tearphones: %d",
                R.drawable.otg, R.drawable.guitar_cable, R.drawable.audio_interface,
                R.drawable.earphones
        ));
    }

    public JSONObject loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = context.getAssets().open("connect.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: unable to parse json", ex);
            return null;
        }

        JSONObject jsonObject = null ;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: cannot parse json", e);
        }

        return jsonObject;
    }

    Drawable getIcon (String filename) {
        switch (filename) {
            default:
            case "splitter":
                return getResources().getDrawable(R.drawable.splitter);
            case "otg":
                return getResources().getDrawable(R.drawable.otg);
            case "earphones":
                return getResources().getDrawable(R.drawable.earphones);
            case "interface":
                return getResources().getDrawable(R.drawable.audio_interface);
            case "trs":
                return getResources().getDrawable(R.drawable.trs);
            case "cable":
                return getResources().getDrawable(R.drawable.guitar_cable);

        }
    }
}