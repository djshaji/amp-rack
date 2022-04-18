package com.shajikhan.ladspa.amprack;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "Amp Rack MainActivity";
    private static final String CHANNEL_ID = "default" ;
    static Context context;
    SwitchMaterial onOff;
    ToggleButton record ;
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
    Notification notification ;
    PurchasesResponseListener purchasesResponseListener ;
    public static boolean proVersion = false ;
    File dir ;

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
    String lastRecordedFileName ;
    NotificationManagerCompat notificationManager ;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
//        System.loadLibrary("opusenc");
    }

    private ActivityMainBinding binding;
    MediaPlayer mediaPlayer ;
    private BillingClient billingClient;
    private PurchasesUpdatedListener purchasesUpdatedListener ;
    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this ;
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        notificationManager = NotificationManagerCompat.from(this);
        acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
            }
        };

        purchasesResponseListener = new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                if (list.isEmpty()) {
                    Log.d(TAG, "onQueryPurchasesResponse: no purchases");
                    return ;
                }

                Purchase purchase = list.get(0);
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    Log.d(TAG, "onQueryPurchasesResponse: purchased");
                    proVersion = true;
                } else {
                    Log.d(TAG, "onQueryPurchasesResponse: not PRO version");
                }
            }
        };


        purchasesUpdatedListener = new PurchasesUpdatedListener() {

            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<com.android.billingclient.api.Purchase> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && list != null) {
                    for (com.android.billingclient.api.Purchase purchase : list) {
                        handlePurchase(purchase);
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }

            }
        };

        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(purchasesUpdatedListener)
                .build();

        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchasesResponseListener);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );


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
//        AudioEngine.showProgress(context);
        AudioEngine.create();
        // load included plugins
        loadPlugins();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();


        if (! proVersion) {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });

            AdView mAdView = findViewById(R.id.adViewBanner);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        int color = getDominantColor(BitmapFactory.decodeResource(getResources(), R.drawable.bg));
//        getWindow().setStatusBarColor(color);
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

        // notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
        }
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Effects Processor is running")
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MIN);


        notification = builder.build();
        dir = context.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC);
        if (dir == null || !dir.mkdirs()) {
            Log.e(TAG, "Directory not created: " + dir.toString());
        } else {
            Log.d(TAG, "onResume: default directory set as " + dir.toString());
        }

        applyWallpaper(context, getWindow(),getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
    }

    void showMediaPlayerDialog () {
        if (lastRecordedFileName == null)
            return;
        Log.d(TAG, "showMediaPlayerDialog: " + lastRecordedFileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.media_player_dialog, null);
        ToggleButton toggleButton = constraintLayout.findViewById(R.id.media_play);
        TextView textView = constraintLayout.findViewById(R.id.media_filename);
        File file = new File(lastRecordedFileName);
        textView.setText(file.getName());
        toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Uri uri = Uri.parse(lastRecordedFileName);
                    try {
                        mediaPlayer.setDataSource(getApplicationContext(), uri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("Cannot load media file: " + e.getMessage());
                        return ;
                    }
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_pause_24);
                    mediaPlayer.start();
                } else {
                    mediaPlayer.pause();
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                }
            }
        });

        SeekBar seekBar = constraintLayout.findViewById(R.id.media_seekbar);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                seekBar.setProgress(0);
            }
        });

        Button share = constraintLayout.findViewById(R.id.share_file);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                // this is pretty awesome!
                MediaScannerConnection.scanFile(context,
                        new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                                intentShareFile.setType("audio/*");
                                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);

                                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                                        "Sharing Audio File...");
                                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing Audio File...");

                                intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));
                            }
                        });
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar _seekBar) {
                mediaPlayer.seekTo(mediaPlayer.getDuration() * (_seekBar.getProgress() / 100));

            }
        });

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    seekBar.setProgress(100* mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                    Log.d(TAG, "run: " + mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                }
            }
        },0,1000);

        builder.setView(constraintLayout)
                .setPositiveButton("Close", null);

        AlertDialog dialog = builder.create();

        Button deleteFile = constraintLayout.findViewById(R.id.delete_file);
        deleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure you want to delete this file?")
                        .setTitle("Delete " + lastRecordedFileName)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface _dialog, int id) {
                                file.delete();
                                if (file.exists()){
                                    toast("File could not be deleted");
                                } else {
                                    toast ("File deleted");
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null);
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mediaPlayer.stop();
                timer.cancel();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mediaPlayer.stop();
                timer.cancel();
            }
        });
        dialog.show();

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

        record = findViewById(R.id.record_button);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "onCheckedChanged: record pressed");
                if (onOff.isChecked()) {
                    Log.d(TAG, "onCheckedChanged: onOff is checked");
                    toast("Cannot start or stop recording while playing");
                    record.setChecked(!b);
                    return;
                }

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
        applyWallpaper(context, getWindow(),getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
        Log.d(TAG, "lifecycle: resumed");
//        AudioEngine.create(); // originally was here
//        loadPlugins();

        // our app got rejected because we use MANAGE STORAGE permission.
        // so we switch to the new API
//        File dir = Environment.getExternalStorageDirectory();
        dir = context.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC);

        if (dir == null || !dir.mkdirs()) {
            Log.e(TAG, "Directory not created: " + dir.toString());
        } else {
            Log.d(TAG, "onResume: default directory set as " + dir.toString());
        }

        String path = dir.getAbsolutePath();

        AudioEngine.setExternalStoragePath(path);
        File defaultDir = dir;
        if (!defaultDir.exists()) {
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
        super.onPause();
        Log.d(TAG, "lifecycle: paused");
        saveActivePreset();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "lifecycle: stopped");
        saveActivePreset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "lifecycle: destroyed");
        stopEffect();
        saveActivePreset();
        AudioEngine.delete();
    }

    public void toggleEffect(boolean isPlaying) {
        if (isPlaying) {
            if (record.isChecked()) {
                lastRecordedFileName = AudioEngine.getRecordingFileName();
                showMediaPlayerDialog();
            }

            stopEffect();
            notificationManager.cancelAll();
        } else {
            // apply settings
            applyPreferencesDevices();
            applyPreferencesExport();
            startEffect();
            notificationManager.notify(0, notification);
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
        return true ; /*

                (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) ;/* &&
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_GRANTED) ; */

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
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_STORAGE_REQUEST);

        /*
        Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
        startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE);

         */

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
            } else {
                if (dir == null || !dir.mkdirs()) {
                    Log.e(TAG, "Directory not created: " + dir.toString());
                }

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
        if (AudioEngine.getTotalPlugins() != 0)
            return;
//        String[] tapPlugins = context.getResources().getStringArray(R.array.tap_plugins);
        String[] tapPlugins = context.getResources().getStringArray(R.array.ladspa_plugins);
        for (String s: tapPlugins) {
            AudioEngine.loadLibrary(/*"lib" + */s);
        }



        /*
        File dir = new File (getApplicationInfo().nativeLibraryDir) ;
        File[] soFiles = dir.listFiles();
        Log.d(TAG, "loadPlugins: found "+ soFiles.length + " plugins in directory "+ getApplicationInfo().nativeLibraryDir);

        for (File s: soFiles) {
            Log.d(TAG, "loadPlugins: loading " + s.toString());
            AudioEngine.loadLibrary(s.toString().split(".")[0]);
        }

         */

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
        // forgot to add this. that this was forgotten was very difficult to guess
        AudioEngine.clearActiveQueue();

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
                // forgot this too
                AudioEngine.setPluginControl(plugin, i, Float.parseFloat(control [i]));
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
        String input = defaultSharedPreferences.getString("input", "-1");
        String output = defaultSharedPreferences.getString("output", "-1");
        Log.d(TAG, "applyPreferences: [devices] " + String.format("input: %s, output: %s", input, output));

        AudioEngine.setRecordingDeviceId(new Integer(input));
        AudioEngine.setPlaybackDeviceId(new Integer(output));

        AudioEngine.setLowLatency(defaultSharedPreferences.getBoolean("latency", true));
        int sampleRate = 48000 ;
        try {
            sampleRate = defaultSharedPreferences.getInt("sample_rate", 48000) ;
        } catch (ClassCastException e) {
            Log.e(TAG, "applyPreferencesDevices: cannot get default sample rate from preference", e);
        }
        AudioEngine.setSampleRate(sampleRate);
    }

    void applyPreferencesExport () {
        String format = defaultSharedPreferences.getString("export_format", "1");
        AudioEngine.setExportFormat(Integer.parseInt(format));
        Integer bitRate = Integer.valueOf(defaultSharedPreferences.getString("opus_bitrate", "64"));
        Log.d(TAG, "applyPreferencesExport: setting bitrate " + bitRate * 1000);
        AudioEngine.setOpusBitRate(bitRate * 1000);
        if (proVersion == false) {
            AudioEngine.setExportFormat(0);
        }
    }

    void printDebugLog () {
        AudioEngine.debugInfo();

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void handlePurchase(com.android.billingclient.api.Purchase purchase) {
        if (purchase.getPurchaseState() == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Thank you for supporting the app!")
                        .setTitle("Purchase Successful")
                        .setIcon(R.drawable.logo)
                        .setPositiveButton("You're Welcome!", null);

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Log.d(TAG, "handlePurchase: purchase already acknowledged");
            }
        }

    }

    public static Bitmap scaleBackground (Bitmap originalImage, int width, int height) {
        Bitmap background = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);

        float originalWidth = originalImage.getWidth();
        float originalHeight = originalImage.getHeight();

        Canvas canvas = new Canvas(background);

//        float scale = width / originalWidth;
        float scale = height / originalHeight ;

//        float xTranslation = 0.0f;
//        float yTranslation = (height - originalHeight * scale) / 2.0f;
        float xTranslation = (width - originalWidth * scale) / 2.0f;
        float yTranslation = 0.0f ;

        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);
        return background;

    }
    public static void applyWallpaper (Context _context, Window window, Resources resources, ImageView imageView, int width, int height) {
        String resIdString = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", "Space");
        Bitmap bitmap = null ;
        switch (resIdString) {
            default:
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(_context.getContentResolver(), Uri.parse(resIdString));
                } catch (Exception e) {
                    e.printStackTrace();
                    break ;
                }

                bitmap = scaleBackground(bitmap, width, height);
                break ;
            case "Space":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg) ;
                break ;
            case "Water":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.water) ;
                break ;
            case "Fire":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.fire) ;
                break ;
            case "Sky":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.sky) ;
                break ;
            case "Earth":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_earth) ;
                break ;
        }

        if (bitmap == null) {
            Log.e(TAG, "applyWallpaper: No suitable bg from settings", null);
            return;
        }

        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);

        int color = getDominantColor(bitmap);
        window.setStatusBarColor(color);

    }
}