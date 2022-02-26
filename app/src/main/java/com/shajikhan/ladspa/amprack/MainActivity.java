package com.shajikhan.ladspa.amprack;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.shajikhan.ladspa.amprack.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "Amp Rack MainActivity";
    Context context;
    SwitchMaterial onOff;
    MaterialButton record ;
    PopupMenu addPluginMenu ;
    RecyclerView recyclerView ;
    DataAdapter dataAdapter ;
    RecyclerView.LayoutManager layoutManager ;
    int primaryColor = com.google.android.material.R.color.design_default_color_primary ;
    private static final int AUDIO_EFFECT_REQUEST = 0;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this ;

        AudioEngine.create();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        int color = getDominantColor(BitmapFactory.decodeResource(getResources(), R.drawable.bg));
        getWindow().setStatusBarColor(color);
        color = adjustAlpha(color, .5f);
        primaryColor = color ;
        onOff = findViewById(R.id.onoff);
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toggleEffect(!b);
            }
        });

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        addPluginMenu = new PopupMenu(context, fab);
        int libraries = AudioEngine.getSharedLibraries();
        for (int i = 0 ; i < libraries ; i ++) {
            SubMenu subMenu = addPluginMenu.getMenu().addSubMenu(AudioEngine.getLibraryName(i));
            for (int plugin = 0 ; plugin < AudioEngine.getPlugins(i) ; plugin ++) {
                // library * 100 + plugin i.e. first plugin from first library = 0
                subMenu.add(AudioEngine.getPluginName(i, plugin));
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPluginMenu.show();
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setBackgroundColor(color);
        /*
        layoutManager = new RecyclerView.LayoutManager() {
            @Override
            public RecyclerView.LayoutParams generateDefaultLayoutParams() {
                return null;
            }
        } ;
        recyclerView.setLayoutManager(layoutManager);
         */
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataAdapter = new DataAdapter();
        recyclerView.setAdapter(dataAdapter);

        // add sample item to recylcer view here
        dataAdapter.setColor(primaryColor);
        dataAdapter.addItem(0, 1);
        dataAdapter.addItem(1, 2);
        dataAdapter.notifyDataSetChanged();
        AudioEngine.setDefaultStreamValues(context);
    }

    /**
     * A native method that is implemented by the 'amprack' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected void onStart() {
        super.onStart();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        AudioEngine.create(); // originally was here
    }
    @Override
    protected void onPause() {
        stopEffect();
        AudioEngine.delete();
        super.onPause();
    }

    public void toggleEffect(boolean isPlaying) {
        if (isPlaying) {
            stopEffect();
        } else {
            startEffect();
        }
    }

    private void startEffect() {
        Log.d(TAG, "Attempting to start");

        if (!isRecordPermissionGranted()){
            requestRecordPermission();
            return;
        }

        boolean success = AudioEngine.setEffectOn(true);
    }

    private void stopEffect() {
        Log.d(TAG, "Playing, attempting to stop");
        AudioEngine.setEffectOn(false);
    }
    private boolean isRecordPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED);
    }

    private void requestRecordPermission(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (AUDIO_EFFECT_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 1 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            // User denied the permission, without this we cannot record audio
            // Show a toast and update the status accordingly
            Toast.makeText(getApplicationContext(),
                    "Permission for audio record denied.",
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            // Permission was granted, start live effect
            toggleEffect(false);
        }
    }

    public static int getDominantColor(Bitmap bitmap) {
        // haha!
        return bitmap.getPixel(0, 0) ;
    }

    @ColorInt
    public static int adjustAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

}