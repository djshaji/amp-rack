package com.shajikhan.ladspa.amprack;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shajikhan.ladspa.amprack.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "Amp Rack MainActivity";
    static Context context;
    SwitchMaterial onOff;
    MaterialButton record ;
    PopupMenu addPluginMenu ;
    RecyclerView recyclerView ;
    DataAdapter dataAdapter ;
    AlertDialog pluginDialog ;
    AudioManager audioManager ;
    AudioDeviceInfo [] audioDevicesInput, audioDevicesOutput ;
    int defaultInputDevice = 0 ;
    int defaultOutputDevice = 0 ;
    RecyclerView.LayoutManager layoutManager ;
    LinearLayout linearLayoutPluginDialog ;
    PluginDialogAdapter pluginDialogAdapter ;
    SharedPreferences defaultSharedPreferences = null ;

    int primaryColor = com.google.android.material.R.color.design_default_color_primary ;
    private static final int AUDIO_EFFECT_REQUEST = 0;
    private static final int READ_STORAGE_REQUEST = 1;
    private static final int WRITE_STORAGE_REQUEST = 2;
    final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501; // Any value

    // Firebase
    private FirebaseAuth mAuth;
    FirebaseUser currentUser ;
    private FirebaseAnalytics mFirebaseAnalytics;
    Rack rack ;
    Presets presets ;
    PopupMenu optionsMenu ;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
//        System.loadLibrary("opusenc");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this ;
        defaultSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this);

        rack = new Rack();
        presets = new Presets();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, rack, null)
                .commit();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, presets, null)
                .commit();

//        LoadFragment(presets);
//        LoadFragment(rack);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

//        AudioEngine.showProgress(null);
        AudioEngine.showProgress(context);
        AudioEngine.create();
        // load included plugins
        loadPlugins();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        int color = getDominantColor(BitmapFactory.decodeResource(getResources(), R.drawable.bg));
        getWindow().setStatusBarColor(color);
        color = adjustAlpha(color, .5f);
        primaryColor = color ;

        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
                = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment=null;
                Class<Rack> fragmentClass=null;
                switch (item.getItemId()) {
                    case R.id.page_rack:
                        fragment = rack ;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(rack)
                                .hide(presets)
                                .commit();
                        return true;
                         /*
                        getSupportFragmentManager()
                            .beginTransaction()
                            .hide (presets)
                            .show (rack)
                            .commit();
                        return true ;

                         */

                    case R.id.page_preset:
                        fragment = presets;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(presets)
                                .hide(rack)
                                .commit();
                        return true;
                        /*
                        if (presets.loginNotice == null)
                            return LoadFragment(fragment);

                        getSupportFragmentManager()
                                .beginTransaction()
                                .hide (rack)
                                .show (presets)
                                .commit();
                        return true ;

                         */

                }
//                return LoadFragment(fragment);
                return false;
            }
        };

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        getSupportFragmentManager()
                .beginTransaction()
                .show(rack)
                .hide(presets)
                .commit();
    }

    /**
     * A native method that is implemented by the 'amprack' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    void setupRack () {
        onOff = findViewById(R.id.onoff);
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toggleEffect(!b);
            }
        });

        pluginDialog = createPluginDialog();

        RecyclerView recyclerView1 = (RecyclerView) linearLayoutPluginDialog.getChildAt(2);
        recyclerView1.setLayoutManager(new LinearLayoutManager(this));
        pluginDialogAdapter = new PluginDialogAdapter();
        pluginDialogAdapter.setMainActivity(context, this);
        recyclerView1.setAdapter(pluginDialogAdapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        addPluginMenu = new PopupMenu(context, fab);

        ToggleButton record = findViewById(R.id.record_button);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (!isStoragePermissionGranted()) {
//                        requestReadStoragePermission();
                        requestWriteStoragePermission();

                        /*
                        if (!isStoragePermissionGranted()) {
                            Toast.makeText(getApplicationContext(),
                                    "Permission denied. Recording features are disabled.",
                                    Toast.LENGTH_LONG)
                                    .show();
                            return ;
                        }
                        */
                    } else {
                        AudioEngine.setRecordingActive(b);
                    }
                }
            }
        });


        int libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");
        for (int i = 0 ; i < libraries ; i ++) {
            SubMenu subMenu = addPluginMenu.getMenu().addSubMenu(AudioEngine.getLibraryName(i));
            for (int plugin = 0 ; plugin < AudioEngine.getPlugins(i) ; plugin ++) {
                // library * 100 + plugin i.e. first plugin from first library = 0
                String name = AudioEngine.getPluginName(i, plugin);
                MenuItem menuItem = subMenu.add(name);
                int finalI = i;
                int finalPlugin = plugin;
                pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name);
                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int ret = AudioEngine.addPlugin(finalI, finalPlugin) ;
                        dataAdapter.addItem(finalI * 100 + finalPlugin, ret);
//                        pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name);
                        return false;
                    }
                });
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                addPluginMenu.show();
                pluginDialog.show();
            }
        });

        MaterialButton settings = findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                // Get the layout inflater
                LayoutInflater inflater = getLayoutInflater();

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.audio_devices_selector, null) ;
                builder.setView(linearLayout)
                        // Add action buttons
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // sign in the user ...
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });

                int i = 0 ;
//                HashMap<CharSequence, Integer> inputs = new HashMap<>();
//                HashMap <CharSequence, Integer> outputs = new HashMap<>();
                ArrayList <String> input_s = new ArrayList<>();
                ArrayList <String> output_s = new ArrayList<>();

                for (i = 0 ; i < audioDevicesInput.length ; i ++) {
                    String name = typeToString(audioDevicesInput[i].getType());
//                    inputs.put(name, audioDevicesInput [i].getId()) ;
                    input_s.add(name);
                }

                for (i = 0 ; i < audioDevicesOutput.length ; i ++) {
                    String name = typeToString(audioDevicesOutput[i].getType());
//                    outputs.put(name, audioDevicesOutput [i].getId()) ;
                    output_s.add(name);
                }

                ArrayAdapter input_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item,input_s);
                input_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                ArrayAdapter output_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item,output_s);
                output_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                Spinner in = (Spinner) linearLayout.getChildAt(1) ;
                Spinner out = (Spinner) linearLayout.getChildAt(3) ;
                in.setAdapter(input_a);
                out.setAdapter(output_a);

                in.setSelection(defaultInputDevice);
                out.setSelection(defaultOutputDevice);

                in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setRecordingDeviceId(audioDevicesInput[i].getId());
                        defaultInputDevice = i ;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                out.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setPlaybackDeviceId(audioDevicesOutput[i].getId());
                        defaultInputDevice = i ;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                builder.show();
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
        dataAdapter.mainActivity = this ;
        recyclerView.setAdapter(dataAdapter);

        // add sample item to recylcer view here
        dataAdapter.setColor(primaryColor);
        /*
        dataAdapter.addItem(0, 1);
        dataAdapter.addItem(1, 2);
        dataAdapter.notifyDataSetChanged();

         */

        /*
        FloatingActionButton debugButton = findViewById(R.id.debug);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioEngine.debugInfo();
            }
        });

         */


    }

    @Override
    protected void onStart() {
        super.onStart();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioEngine.create(); // originally was here
        loadPlugins();
        File dir = Environment.getExternalStorageDirectory();
        String path = dir.getAbsolutePath();

        AudioEngine.setExternalStoragePath(path);
        File defaultDir = new File (path + "/AmpRack/") ;
        if (!defaultDir.exists()) {
            Log.d(TAG, "making directory " + path + "/AmpRack/");
            try {
                if (!defaultDir.mkdir())
                    Log.wtf (TAG, "Unable to create directory!");
            }  catch (Exception e) {
                Log.w(TAG, "UNable to create directory: " + e.getMessage());
            }
        }

//        loadActivePreset();
    }
    @Override
    protected void onPause() {
        stopEffect();
        saveActivePreset();
        AudioEngine.delete();
        super.onPause();
    }

    public void toggleEffect(boolean isPlaying) {
        if (isPlaying) {
            stopEffect();
        } else {
            // apply settings
            applyPreferencesDevices();
            applyPreferencesExport();
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

    @RequiresApi(api = Build.VERSION_CODES.R)
    boolean isStoragePermissionGranted() {
        return Environment.isExternalStorageManager() ;
                /*
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) ; &&
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_GRANTED) ;*/
    }

    private void requestRecordPermission(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }

    private void requestReadStoragePermission(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                READ_STORAGE_REQUEST);
    }

    void requestWriteStoragePermission(){
        /*
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                WRITE_STORAGE_REQUEST);
         */
        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == APP_STORAGE_ACCESS_REQUEST_CODE)
        {
            if (Environment.isExternalStorageManager())
            {
                // Permission granted. Now resume your workflow.
            } else {
                Toast.makeText(getApplicationContext(),
                        "Storage permission denied. Recording and playing features won't work",
                        Toast.LENGTH_LONG)
                        .show();

            }

        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (AUDIO_EFFECT_REQUEST != requestCode && requestCode !=READ_STORAGE_REQUEST && requestCode != WRITE_STORAGE_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (requestCode == READ_STORAGE_REQUEST || requestCode == WRITE_STORAGE_REQUEST) {
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Storage permission denied. Recording and playing features won't work",
                        Toast.LENGTH_LONG)
                        .show();
            }
        }

        if ( AUDIO_EFFECT_REQUEST == requestCode) {
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                // User denied the permission, without this we cannot record audio
                // Show a toast and update the status accordingly
                Toast.makeText(getApplicationContext(),
                        "Permission denied.",
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                // Permission was granted, start live effect
                toggleEffect(false);
            }
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

    static String typeToString(int type){
        switch (type) {
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "auxiliary line-level connectors";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth device supporting the A2DP profile";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth device typically used for telephony";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "built-in earphone speaker";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "built-in microphone";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "built-in speaker";
            case AudioDeviceInfo.TYPE_BUS:
                return "BUS";
            case AudioDeviceInfo.TYPE_DOCK:
                return "DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "FM";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "FM tuner";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "HDMI audio return channel";
            case AudioDeviceInfo.TYPE_IP:
                return "IP";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "line analog";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "line digital";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "telephony";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TV tuner";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB accessory";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB device";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wired headphones";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "wired headset";
            default:
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "unknown";
        }
    }

    AlertDialog createPluginDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        linearLayoutPluginDialog = (LinearLayout) inflater.inflate(R.layout.load_plugin_dialog, null) ;
        EditText editText = (EditText)((LinearLayout) linearLayoutPluginDialog.getChildAt(1)).getChildAt(0);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                pluginDialogAdapter.search(editable.toString());
            }
        });

        ToggleButton toggleButton = (ToggleButton) ((LinearLayout) linearLayoutPluginDialog.getChildAt(1)).getChildAt(1);
        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                pluginDialogAdapter.showOnlyFavorites(b);
                if (b)
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
                else
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            }
        });

        builder.setView(linearLayoutPluginDialog)
                // Add action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                }) ;

        return builder.create() ;
    }

    public void addPluginToRack (int pluginID) {
        int library = pluginID / 100 ;
        int plug = pluginID - (library * 100) ;
        Log.d(TAG, "Adding plugin: " + library + ": " + plug);
        int ret = AudioEngine.addPlugin(library, plug) ;
        dataAdapter.addItem(pluginID, ret);

        Toast.makeText(context, "Added plugin to rack", Toast.LENGTH_LONG).show();

//        Snackbar.make(recyclerView, "Added plugin to rack", Snackbar.LENGTH_LONG)
//                .show();
    }

    void loadPlugins () {
        String[] tapPlugins = context.getResources().getStringArray(R.array.tap_plugins);
        for (String s: tapPlugins) {
            AudioEngine.loadLibrary("lib" + s);
        }

        AudioEngine.loadPlugins();

    }

    String presetToString () throws JSONException {
        JSONObject preset = new JSONObject();
        if (dataAdapter == null)
            return null ;

        for (int i = 0 ; i < dataAdapter.getItemCount() ; i ++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i, null);
                continue ;
            }

            JSONObject jo = new JSONObject();
            String vals = "";

            for (int k = 0 ; k < holder.sliders.size() ; k ++) {
                vals += holder.sliders.get(k).getValue() ;
                if (k < holder.sliders.size() - 1) {
                    vals += ";";
                }
            }

            try {
                jo.put("name", holder.pluginName.getText());
                jo.put("controls", vals);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            preset.put(String.valueOf(i), jo.toString());
        }

        return preset.toString() ;
    }

    Map presetToMap () throws JSONException {
        Map <String, Map> preset = new HashMap<>();
        if (dataAdapter == null)
            return null ;

        for (int i = 0 ; i < dataAdapter.getItemCount() ; i ++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i, null);
                continue ;
            }

            Map <String, String> jo = new HashMap<>();
            String vals  = "";

            for (int k = 0 ; k < holder.sliders.size() ; k ++) {
                vals += holder.sliders.get(k).getValue() ;
                if (k < holder.sliders.size() - 1) {
                    vals += ";";
                }
            }

            jo.put("name", (String) holder.pluginName.getText());
            jo.put("controls", vals);

            preset.put(String.valueOf(i), jo);
        }

        return preset ;
    }


    void saveActivePreset () {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset ;
        try {
            preset = presetToString() ;
        } catch (JSONException e) {
            e.printStackTrace();
            return ;
        }

        if (preset == null)
            return ;
        sharedPreferences.edit().putString("activePreset", preset).apply();
        Log.d(TAG, "saveActivePreset: Saved preset: " + preset);
    }

    void loadActivePreset () {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset = sharedPreferences.getString("activePreset", null);
        if (preset != null) {
            loadPreset(preset);
        }
    }

    void loadPreset (Map map) {
        JSONObject jsonObject = new JSONObject(map);
        String controls ;
        try {
            controls = (String) jsonObject.get("controls").toString();
            loadPreset(controls.toString());
        } catch (JSONException e) {
            MainActivity.toast("Cannot load preset: "+e.getMessage());
            e.printStackTrace();
        }

    }

    void loadPreset (String preset) {
        Log.d(TAG, "loadPreset: " + preset);
        JSONObject jsonObject ;
        try {
            jsonObject = new JSONObject(preset);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadPreset: Unable to load preset\n"+preset, e);
            return ;
        }

        int items = dataAdapter.totalItems ;
        if (items > 0) {
            Log.d(TAG, "loadPreset: already loaded something, deleting ...");
//            dataAdapter.deleteAll();
            dataAdapter.reset();
        }

        int plugin = 0 ;
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONObject jo ;
            try {
                Log.d(TAG, "loadPreset: trying preset " + key + ": " + jsonObject.getString(key));
                jo = new JSONObject (jsonObject.getString(key)) ;
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse key: " + key, e);
                continue;
            }

            String name, controls ;
            try {
                name = jo.getString("name");
                controls = jo.getString("controls");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse name or controls for key: " + key, e);
                continue ;
            }

            int ret = AudioEngine.addPluginByName(name);
            Log.d(TAG, "loadPreset: Loaded plugin: " + name);
            String [] control = controls.split(";");

            DataAdapter.ViewHolder holder = null ;
            if (dataAdapter.holders.size() == 0)
                Log.e(TAG, "loadPreset: data adapter holders is zero", null);
            else
                dataAdapter.holders.get(plugin);
            if (holder == null) {
                Log.e(TAG, "loadPreset: cannot find holder for " + (ret -1), null);
            }


            Log.d(TAG, "loadPreset: loading "+control.length+ " controls from "+controls);
            for (int i = 0 ; i < control.length ; i ++) {
                Log.d(TAG, "loadPreset: " + i + ": " + control[i]);
                //                holder.sliders.get(i).setValue(Integer.parseInt(control [i]));
                AudioEngine.setPresetValue(plugin, i, Float.parseFloat(control [i]));
            }

            dataAdapter.addItem(ret, ret);
            plugin ++ ;
        }

    }

    private boolean LoadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    public static void toast (String text) {
        Toast.makeText(context,
                text,
                Toast.LENGTH_LONG)
                .show();

    }

    public void heartPlugin (String name) {
        Log.d(TAG, "heartPlugin: " + name);
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            set = new HashSet<>();
        }

        set.add(name);
        sharedPreferences.edit().putStringSet("favoritePresets", set).apply();
    }

    public void unheartPlugin (String name) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return ;
        }

        set.remove(name);
        sharedPreferences.edit().putStringSet("favoritePresets", set).apply();
    }

    boolean isPluginHearted (String plugin) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return false;
        }

        return set.contains(plugin);
    }

    Set getHeartedPlugins () {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);

        return set ;
    }

    void applyPreferencesDevices () {
        // Audio Devices
        String input = defaultSharedPreferences.getString("input", "Default");
        String output = defaultSharedPreferences.getString("output", "Default");
        Log.d(TAG, "applyPreferences: [devices] " + String.format("input: %s, output: %s", input, output));

        AudioEngine.setRecordingDeviceId(new Integer(input));
        AudioEngine.setPlaybackDeviceId(new Integer(output));
    }

    void applyPreferencesExport () {
        String format = defaultSharedPreferences.getString("export_format", "OPUS (Recommended)");
        AudioEngine.setExportFormat(Integer.parseInt(format));
    }
}