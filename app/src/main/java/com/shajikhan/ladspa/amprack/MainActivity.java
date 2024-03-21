package com.shajikhan.ladspa.amprack;

import static java.lang.Math.abs;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shajikhan.ladspa.amprack.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, TextureView.SurfaceTextureListener {
    private static final String TAG = "Amp Rack MainActivity";

    private static final String CHANNEL_ID = "default";
    Surface surface_ = null;
    SurfaceTexture surfaceTexture;
    public boolean headphoneWarning = true;
    static Context context;
    static FileOutputStream fileOutputStream = null ;
    static DataOutputStream dataOutputStream = null ;
    static MainActivity mainActivity;
    boolean videoRecording = false ;
    Camera2 camera2 ;
    MediaPlayerDialog mediaPlayerDialog = null;
    static class AVBuffer {
        float [] bytes ;
        int size ;
    }
    public static LinkedList<AVBuffer> avBuffer = new LinkedList<>();
    static int avEncoderIndex = 0 ;
    public static long presentationTimeUs = 0;
    int totalBytesRead = 0;

    ExtendedFloatingActionButton fab ;
    Button hidePanel;
    SwitchMaterial onOff = null ;
    AudioEncoder audioEncoder ;
    String exportFormat ;
    TextView patchName, patchNo, patchDesc ;
    ToggleButton triggerRecordToggle ;
    int deviceWidth;
    int deviceHeight;
    long totalMemory = 0;
    static boolean lowMemoryMode = false;
    static boolean darkMode = false ;
    boolean safeMode = false;
    static boolean introShown = false ;
    public Camera camera ;
    ToggleButton record;
    boolean triggerRecord = false ;
    public Handler handler;
    public boolean tunerEnabled = false;

    enum RequestCode {
        TRACK_AUDIO_FILE (1001);

        RequestCode(int i) {
        }
    }  ;
    boolean triggerRecordedSomething = false ;
    boolean recording = false ;
    PopupMenu addPluginMenu;
    RecyclerView recyclerView;
    DataAdapter dataAdapter;
    AlertDialog pluginDialog;
    ImageView pluginDialogWallpaper;
    AudioManager audioManager;
    TextView tuner ;
    boolean running = false ;
    long bootFinish = 0 ;
    static boolean showIntro = false ;
    static public JSONObject pluginCategories;
    public Spinner pluginDialogCategorySpinner;
    AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput;
    int defaultInputDevice = 0;
    int defaultOutputDevice = 0;
    RecyclerView.LayoutManager layoutManager;
    ConstraintLayout linearLayoutPluginDialog;
    boolean lazyLoad = true;
    static String[] sharedLibraries;
    static String[] sharedLibrariesLV2;
    PluginDialogAdapter pluginDialogAdapter;
    SharedPreferences defaultSharedPreferences = null;
    Notification notification;
    JSONObject knobsLayout;
    PurchasesResponseListener purchasesResponseListener;
    public static boolean proVersion = false;
    File dir;
    static File localThemeDir ;
    HashCommands hashCommands;
    JSONObject rdf;
    JSONObject ampModels;
    Slider inputVolume, outputVolume;
    SeekBar seekBarIn, seekBarOut;
    RotarySeekbar rotarySeekbarIn, rotarySeekbarOut;
    TextView displayIn, displayOut;

    ToggleButton toggleMixer;
    static int totalPlugins = 0;
    static boolean useTheme = true;
    String theme = "TubeAmp";
    String customTheme = null ;
    static SkinEngine skinEngine = null;

    int primaryColor = com.google.android.material.R.color.design_default_color_primary;
    private static final int AUDIO_EFFECT_REQUEST = 0;
    private static final int READ_STORAGE_REQUEST = 1;
    private static final int WRITE_STORAGE_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 3;
    final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501; // Any value

    // Firebase
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    public Rack rack;
    public Tracks tracks, drums;
    public Presets presets;
//    public MyPresets quickPatch;
    public QuickPatch quickPatch ;
    PopupMenu optionsMenu;
    String lastRecordedFileName;
    NotificationManagerCompat notificationManager;
    static ProgressBar inputMeter;
    static ProgressBar outputMeter;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
//        System.loadLibrary("opusenc");
    }

    private ActivityMainBinding binding;
    MediaPlayer mediaPlayer;
    JSONObject availablePlugins, availablePluginsLV2;
    ArrayList<String> lv2Plugins = new ArrayList<String>();
    private BillingClient billingClient;
    private PurchasesUpdatedListener purchasesUpdatedListener;
    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;
    Bundle savedState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        savedInstanceState = null ;
        super.onCreate(null);

         */

        savedState = savedInstanceState;
        context = this;
        mainActivity = this;

        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        lowMemoryMode = memoryInfo.lowMemory ;

        handler = new Handler(Looper.getMainLooper()) ;

        if (savedInstanceState != null) {
            // to remove duplicate fragments
            List<Fragment> al = getSupportFragmentManager().getFragments();
            if (al == null) {
                // code that handles no existing fragments
                Log.d(TAG, "onCreate: no existing fragments");
//                return;
            } else {
                for (Fragment frag : al) {
                    // To save any of the fragments, add this check.
                    // A tag can be added as a third parameter to the fragment when you commit it
                    if (frag == null) {
                        continue;
                    }

                    switch (frag.getTag()) {
                        case "qp":
                            quickPatch = (QuickPatch) frag;
                            break;
                        case "rack":
                            rack = (Rack) frag;
                            break;
                        case "presets":
                            presets = (Presets) frag;
                            break;
                        case "tracks":
                            tracks = (Tracks) frag;
                            break;
                        case "drums":
                            drums = (Tracks) frag;
                            break;
                        default:
                    }
                    Log.d(TAG, "onCreate: " + frag);
//                getSupportFragmentManager().beginTransaction().remove(frag).commit();
                }
            }
        }

        Log.d(TAG, "onCreate: Welcome! " + getApplicationInfo().toString());
        hashCommands = new HashCommands(this);
        hashCommands.setMainActivity(this);
        hashCommands.add (this, "AudioRecordTest");
        hashCommands.add (this, "cameraPreview");
        hashCommands.add(this, "saveActivePreset");
        hashCommands.add(this, "printActivePreset");
        hashCommands.add(this, "proDialog");
        hashCommands.add(this, "testLV2");
        hashCommands.add(this, "printDebugLog");
        hashCommands.add(this, "printActiveChain");
        hashCommands.add(this, "printPluginsAll");
        hashCommands.add(this, "drummer");
        hashCommands.add(this, "featured");
        hashCommands.add(this, "resetOnboard");
        hashCommands.add(this, "setAudioDevice");

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        headphoneWarning = defaultSharedPreferences.getBoolean("headphone-warning", true);

        Log.d(TAG, "onCreate: " + String.format("" +
                "%d: %d", BuildConfig.VERSION_CODE, defaultSharedPreferences.getInt("currentVersion", 0)));
        if (BuildConfig.VERSION_CODE > defaultSharedPreferences.getInt("currentVersion", 0)) {
            Log.d(TAG, "onCreate: " + String.format(
                    "Version Code: %d\t\tcurrent version: %d",
                    BuildConfig.VERSION_CODE, defaultSharedPreferences.getInt("currentVersion", 0)
            ));
            showIntro = true;
        }

        Intent intentMain = getIntent();
        int showOn = intentMain.getIntExtra("onboard", 0);

        Log.d(TAG, "onCreate: showOn: " + showOn);

        theme = defaultSharedPreferences.getString("theme", "TubeAmp");
//        customTheme = defaultSharedPreferences.getString("custom_theme", null);
        String themeOnboarded = intentMain.getStringExtra("theme");
        if (themeOnboarded != null)
            theme = themeOnboarded;
        if (showIntro && !introShown && showOn == 0 && ! lowMemoryMode) {
            Intent intent = new Intent(this, Onboard.class);
            startActivity(intent);
        }

        Log.d(TAG, "onCreate: loading theme " + theme);
        if (theme.equals("Material") || lowMemoryMode) {
            useTheme = false;
        } else {
            skinEngine = new SkinEngine(this);
            /*
            if (customTheme != null) {
                theme = customTheme;
                skinEngine.custom = true ;
            }

             */

            skinEngine.setTheme(theme);

        }

        int nightModeFlags =
                getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                darkMode = true;
                break;
            default:
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                darkMode = false;
                break;
        }

        pluginCategories = MainActivity.loadJSONFromAsset("plugins.json");
        availablePlugins = ConnectGuitar.loadJSONFromAssetFile(this, "all_plugins.json");
        availablePluginsLV2 = ConnectGuitar.loadJSONFromAssetFile(this, "lv2_plugins.json");
        ampModels = ConnectGuitar.loadJSONFromAssetFile(this, "amps.json");
        knobsLayout = ConnectGuitar.loadJSONFromAssetFile(this, "knobs.json");

        Iterator<String> keys = availablePluginsLV2.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                String _p = availablePluginsLV2.getJSONObject(key).getString("name");
                Log.d(TAG, "onCreate: found LV2 plugin " + key + ": " + _p);
                lv2Plugins.add(_p);
            } catch (JSONException e) {
                Log.e(TAG, "onCreate: no name in plugin " + key, e);
            }
        }

        Log.d(TAG, "onCreate: [LV2 plugins]: " + availablePluginsLV2.toString());

        Log.d(TAG, "onCreate: bootstart: " + defaultSharedPreferences.getLong("bootStartz", 1L) + " bootFinish: " + defaultSharedPreferences.getLong("bootFinish", 1L));
        if (defaultSharedPreferences.getLong("bootFinish", 1L) < defaultSharedPreferences.getLong("bootStart", 0L)) {
            safeMode = true;
            Log.d(TAG, "onCreate: turned on safe mode");
        }

        defaultSharedPreferences.edit().putLong("bootStart", System.currentTimeMillis()).commit();

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        notificationManager = NotificationManagerCompat.from(this);
        lazyLoad = defaultSharedPreferences.getBoolean("lazyLoad", true);
        Log.d(TAG, "onCreate: lazyLoad set to " + lazyLoad);
        try {
            proVersion = defaultSharedPreferences.getBoolean("pro", false);
        } catch (ClassCastException e) {
            Log.e(TAG, "onCreate: incorrect preference found!", e);
            proVersion = false;
            defaultSharedPreferences.edit().putBoolean("pro", false).apply();
        }
        Log.d(TAG, "onCreate: purchased proVersion: " + proVersion);

        boolean forceAds = defaultSharedPreferences.getString("forceads", "off") != "off";
//        Log.d(TAG, "onCreate: forceads is " + defaultSharedPreferences.getString("forceads", "off"));
        if (!proVersion) {
            acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                    Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
                }
            };

            purchasesResponseListener = new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                    Log.d(TAG, "onQueryPurchasesResponse: " + billingResult.getDebugMessage());
                    if (list.isEmpty()) {
                        Log.d(TAG, "onQueryPurchasesResponse: no purchases");
                        Log.d(TAG, "onQueryPurchasesResponse: not PRO version");

                        /*
                        MobileAds.initialize(context, new OnInitializationCompleteListener() {
                            @Override
                            public void onInitializationComplete(InitializationStatus initializationStatus) {
//                                AdView mAdView = findViewById(R.id.adViewBanner);
//                                mAdView.setVisibility(View.VISIBLE);

                            //RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("BFB9B3B3E530352EEB4F664CA9D5E692"));
                            //RequestConfiguration requestConfiguration = requestConfigurationBuilder.build() ;



//                                AdRequest adRequest = new AdRequest.Builder().build();
                                boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
                                if (!isDebuggable) {
                                    Log.d(TAG, "onQueryPurchasesResponse: is not debuggable");
//                                    mAdView.setAdUnitId("ca-app-pub-2182672984086800~2348124251");
                                }

//                                mAdView.loadAd(adRequest);
                            }
                        });

                         */

                        defaultSharedPreferences.edit().putBoolean("pro", false).apply();
                        return;
                    }

                    Purchase purchase = list.get(0);
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !forceAds) {
                        Log.d(TAG, "onQueryPurchasesResponse: purchased");
                        proVersion = true;
                        AdView mAdView = findViewById(R.id.adViewBanner);
                        mAdView.setVisibility(View.GONE);
                        defaultSharedPreferences.edit().putBoolean("pro", true).apply();
                    } else {
                        Log.d(TAG, "onQueryPurchasesResponse: not PRO version");
                        /*
                        MobileAds.initialize(context, new OnInitializationCompleteListener() {
                            @Override
                            public void onInitializationComplete(InitializationStatus initializationStatus) {
                                AdView mAdView = findViewById(R.id.adViewBanner);
                                mAdView.setVisibility(View.VISIBLE);
                                AdRequest adRequest = new AdRequest.Builder().build();
                                boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
                                if (!isDebuggable) {
                                    Log.d(TAG, "onQueryPurchasesResponse: is not debuggable");
//                                    mAdView.setAdUnitId("ca-app-pub-2182672984086800~2348124251");
                                }
                                mAdView.loadAd(adRequest);
                            }

                        });
                         */

                        defaultSharedPreferences.edit().putBoolean("pro", false).apply();
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
                        Log.d(TAG, "onPurchasesUpdated: user cancelled purchase");
                    } else {
                        // Handle any other error codes.
                        Log.d(TAG, "onPurchasesUpdated: got purchase response " + billingResult.getDebugMessage());
                    }

                }
            };

            billingClient = BillingClient.newBuilder(context)
                    .enablePendingPurchases()
                    .setListener(purchasesUpdatedListener)
                    .build();
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

//        AudioEngine.showProgress(null);
//        AudioEngine.showProgress(context);
        AudioEngine.create();
        AudioEngine.setLibraryPath(getApplicationInfo().nativeLibraryDir);
        AudioEngine.setLazyLoad(lazyLoad);
        // load included plugins
        loadPlugins();

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: savedInstanceState is not null");

            rack = new Rack();
            tracks = new Tracks(this);
            drums = new Tracks(this, true);
            drums.isDrums = true ;
            presets = new Presets();
//            quickPatch = new MyPresets(false, true);
            quickPatch = new QuickPatch();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, quickPatch, "qp")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, rack, "rack")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, presets, "presets")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, tracks, "tracks")
                    .commit();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, drums, "drums")
                    .commit();
        }

//        LoadFragment(presets);
//        LoadFragment(rack);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        /* we do this async after getting billing client result
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

         */

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioDevicesInput = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        totalMemory = memInfo.totalMem;
        Log.d(TAG, "onCreate: total memory available: " + totalMemory);

        int color = 0;

        if (! lowMemoryMode) {
            color = getDominantColor(BitmapFactory.decodeResource(getResources(), R.drawable.bg));
        } else {
            color = getResources().getColor(androidx.navigation.ui.R.color.design_default_color_primary);
        }
//        getWindow().setStatusBarColor(color);
        color = adjustAlpha(color, .5f);
        primaryColor = color;

        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
                = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                Class<Rack> fragmentClass = null;
                switch (item.getItemId()) {
                    case R.id.page_quick:
                        fragment = rack;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(quickPatch)
                                .hide(rack)
                                .hide(presets)
                                .hide(tracks)
                                .hide(drums)
                                .commit();
                        return true;
                    case R.id.page_rack:
                        fragment = rack;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(rack)
                                .hide(quickPatch)
                                .hide(presets)
                                .hide(tracks)
                                .hide(drums)
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
                                .hide(tracks)
                                .hide(quickPatch)
                                .hide(drums)
                                .commit();
                        return true;

                    case R.id.page_tracks:
                        fragment = tracks;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(tracks)
                                .hide(rack)
                                .hide(quickPatch)
                                .hide(presets)
                                .hide(drums)
                                .commit();

                        return true;
                    case R.id.page_drums:
                        fragment = drums;
                        getSupportFragmentManager()
                                .beginTransaction()
                                .show(drums)
                                .hide(rack)
                                .hide(quickPatch)
                                .hide(presets)
                                .hide(tracks)
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
                .hide(tracks)
                .hide(drums)
                .commit();

        bottomNavigationView.setSelectedItemId(R.id.page_rack);

        // notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    CHANNEL_ID,
                    getResources().getString(R.string.app_name),
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
        localThemeDir = context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS);
        if (dir == null || !dir.mkdirs()) {
            Log.e(TAG, "Directory not created: " + dir.toString());
        } else {
            Log.d(TAG, "onResume: default directory set as " + dir.toString());
        }

        tracks.load(dir);
//        tracks.load(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES));
        Set<String> _tracksCustom = defaultSharedPreferences.getStringSet("tracks", null) ;
        if (_tracksCustom != null) {
            for (String _d:
                 _tracksCustom) {
                tracks.load(Uri.parse(_d));
            }
        }

        try {
            drums.load(getAssets().list("drums"));
        } catch (IOException e) {
            MainActivity.toast("Cannot load drum loops: " + e.getMessage());
            e.printStackTrace();
        }

        Set<String> _drumsCustom = defaultSharedPreferences.getStringSet("drums", null) ;
        if (_drumsCustom != null) {
            for (String _d:
                    _drumsCustom) {
                drums.load(Uri.parse(_d));
            }
        }

        applyWallpaper(context, getWindow(), getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
        if (!proVersion) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {

                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    Log.d(TAG, "onBillingSetupFinished: " + billingResult.getDebugMessage());
                    billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, purchasesResponseListener);

                }
            });
            ;
        }

        deviceWidth = getWindowManager().getDefaultDisplay().getWidth();
        deviceHeight = getWindowManager().getDefaultDisplay().getHeight();

        Log.d(TAG, "onCreate: Loading JSON");
        rdf = loadJSONFromAsset("plugins_info.json");

        bootFinish = System.currentTimeMillis();
        defaultSharedPreferences.edit().putLong("bootFinish", bootFinish).commit();
        if (safeMode) {
            toast("Safe mode was enabled because the app did not load successfully. Some features may be disabled.");
        }

        Log.d(TAG, "onCreate: boot complete, we are now live bootFinish: [" + bootFinish + "]");
        if (showIntro && ! introShown && showOn == 0) {
            finishActivity(0);
        }

//        Log.d(TAG, "onCreate: mixer state: " + );
    }

    void showMediaPlayerDialog() {
        if (lastRecordedFileName == null) {
            Log.e(TAG, "showMediaPlayerDialog: no last recorded audio");
            return;
        }

//        if (mediaPlayerDialog == null)
//            mediaPlayerDialog = new MediaPlayerDialog(this, mediaPlayer);
//        mediaPlayerDialog.dialog.show();
//        if (mediaPlayerDialog.dialog != null)
//            return;
        Log.d(TAG, "showMediaPlayerDialog: " + lastRecordedFileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.media_player_dialog, null);
        SurfaceView surface = constraintLayout.findViewById(R.id.video_player_dialog);

        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mediaPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mediaPlayer.setDisplay(null);
                mediaPlayer.stop();
            }
        });

        ToggleButton toggleButton = constraintLayout.findViewById(R.id.media_play);
        surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButton.setChecked(false);
            }
        });
        Button openFolder = constraintLayout.findViewById(R.id.open_folder);
        openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(dir.toString());
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(uri, "*/*");
                startActivity(intent);

            }
        });
        TextView textView = constraintLayout.findViewById(R.id.media_filename);
        File file = new File(lastRecordedFileName);
        tracks.tracksAdapter.add(lastRecordedFileName);
        textView.setText(file.getName());
        toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Uri uri = Uri.parse(lastRecordedFileName);
                    try {
                        surface.setVisibility(View.VISIBLE);
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(getApplicationContext(), uri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("Cannot load media file: " + e.getMessage());
                        return;
                    }

                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_pause_24);
                    mediaPlayer.start();
                } else {
                    mediaPlayer.pause();
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                    surface.setVisibility(View.GONE);
                }
            }
        });

        SeekBar seekBar = constraintLayout.findViewById(R.id.media_seekbar);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                seekBar.setProgress(0);
                surface.setVisibility(View.GONE);
            }
        });

        Button share = constraintLayout.findViewById(R.id.share_file);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareFile(file);
                // this is pretty awesome!
                // update 24-6-2022 doesnt work :(
                /*
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

                 */
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

        // Disabling this because this caused a crash when stopping recording ... I think
        // very hard to debug ... VERY !!!
        /*
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    seekBar.setProgress(100 * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                    Log.d(TAG, "run: " + mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                }
            }
        }, 0, 1000);

         */

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
                                if (file.exists()) {
                                    toast("File could not be deleted");
                                } else {
                                    toast("File deleted");
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
//                timer.cancel();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mediaPlayer.stop();
//                timer.cancel();
            }
        });
        dialog.show();

    }

    /**
     * A native method that is implemented by the 'amprack' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    void setupRack() {
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
        if (useTheme)
            skinEngine.view(linearLayoutPluginDialog, "wallpaper", "bg", SkinEngine.Resize.Width, 1);
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

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(this, "assets/blacklist.json");
        Log.d(TAG, String.format ("blacklist: %s", blacklist.toString()));

        int libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");
        for (int i = 0; i < libraries; i++) {
            SubMenu subMenu = addPluginMenu.getMenu().addSubMenu(AudioEngine.getLibraryName(i));
            for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                // library * 100 + plugin i.e. first plugin from first library = 0
                String name = AudioEngine.getPluginName(i, plugin);
                MenuItem menuItem = subMenu.add(name);
                int finalI = i;
                int finalPlugin = plugin;
                int pluginID = finalI * 100 + finalPlugin ;
                if (blacklist.has(String.valueOf(pluginID)))
                    continue;

                pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name);

                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int ret = AudioEngine.addPlugin(finalI, finalPlugin);
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
                LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.audio_devices_selector, null);
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

                int i = 0;
//                HashMap<CharSequence, Integer> inputs = new HashMap<>();
//                HashMap <CharSequence, Integer> outputs = new HashMap<>();
                ArrayList<String> input_s = new ArrayList<>();
                ArrayList<String> output_s = new ArrayList<>();

                for (i = 0; i < audioDevicesInput.length; i++) {
                    String name = typeToString(audioDevicesInput[i].getType());
//                    inputs.put(name, audioDevicesInput [i].getId()) ;
                    input_s.add(name);
                }

                for (i = 0; i < audioDevicesOutput.length; i++) {
                    String name = typeToString(audioDevicesOutput[i].getType());
//                    outputs.put(name, audioDevicesOutput [i].getId()) ;
                    output_s.add(name);
                }

                ArrayAdapter input_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, input_s);
                input_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                ArrayAdapter output_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, output_s);
                output_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                Spinner in = (Spinner) linearLayout.getChildAt(1);
                Spinner out = (Spinner) linearLayout.getChildAt(3);
                in.setAdapter(input_a);
                out.setAdapter(output_a);

                in.setSelection(defaultInputDevice);
                out.setSelection(defaultOutputDevice);

                in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setRecordingDeviceId(audioDevicesInput[i].getId());
                        defaultInputDevice = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                out.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        AudioEngine.setPlaybackDeviceId(audioDevicesOutput[i].getId());
                        defaultInputDevice = i;
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
        dataAdapter.mainActivity = this;
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
        Log.d(TAG, "onStart: " + String.format(
                "bootFinish: %d", bootFinish
        ));

    }

    @Override
    protected void onResume() {
        super.onResume();
//        SkinEngine.setColorScheme(this, defaultSharedPreferences.getString("color_scheme", "AmpRack"));
        applyWallpaper(context, getWindow(), getResources(), findViewById(R.id.wallpaper), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally
//        recreate();
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
                    Log.wtf(TAG, "Unable to create directory!");
            } catch (Exception e) {
                Log.w(TAG, "UNable to create directory: " + e.getMessage());
            }
        }

//        loadActivePreset();
        // when savedInstanceState is not null
        // the problem with this is that when we start at first,
        // this will be empty since firestore returns data async
        /*
        if (quickPatch.myPresetsAdapter.allPresets.isEmpty() && savedState != null) {
            Log.d(TAG, "onViewCreated: loading quick patch manually: " + savedState);
            quickPatch.load();
        }

         */
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
            /*
            if (record.isChecked()) {
                lastRecordedFileName = AudioEngine.getRecordingFileName();
                showMediaPlayerDialog();
            }

             */

            if (record.isChecked())
                record.setChecked(false);
            if (rack.toggleVideo.isChecked())
                rack.toggleVideo.setChecked(false);

            stopEffect();
            notificationManager.cancelAll();
            running = false ;
        } else {
            if (! isHeadphonesPlugged() && headphoneWarning) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("No headphones or audio interface detected: you may hear feedback if you run the app on device speakers. Do you wish to continue?")
                        .setPositiveButton("Start audio", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                headphoneWarning = false;
                                toggleEffect(isPlaying);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onOff.setChecked(false);
                            }
                        })
                        .setNeutralButton("Do not show again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position of the selected item.
                                defaultSharedPreferences.edit().putBoolean("headphone-warning", false).apply();
                                headphoneWarning = false;
                                toggleEffect(isPlaying);
                            }
                        })
                        .setTitle("Feedback Noise Warning") ;
                builder.create().show();
                return;
            }
            // apply settings
            applyPreferencesDevices();
            applyPreferencesExport();
            startEffect();
            notificationManager.notify(0, notification);
        }
    }

    private void startEffect() {
        Log.d(TAG, "Attempting to start");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!isRecordPermissionGranted()) {
            requestRecordPermission();
            return;
        }

        running = AudioEngine.setEffectOn(true);
    }

    private void stopEffect() {
        if (! running) return;
        Log.d(TAG, "Playing, attempting to stop, state: " + running);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AudioEngine.setEffectOn(false);
        if (bootFinish > 0 && !AudioEngine.wasLowLatency() && defaultSharedPreferences.getBoolean("warnLowLatency", true)) {
            Log.d(TAG, "stopEffect() called: Low Latency Warning");
            toast(getResources().getString(R.string.lowLatencyWarning));
        }

        inputMeter.setProgress(0);
        outputMeter.setProgress(0);
        if(triggerRecordedSomething) {
            showMediaPlayerDialog();
            triggerRecordToggle.setChecked(false);
            triggerRecordedSomething = false;
//            outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        }

        if (record.isChecked())
            record.setChecked(false);
    }

    private boolean isRecordPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    boolean isStoragePermissionGranted() {
        return true; /*

                (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) ;/* &&
                        (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_GRANTED) ; */

    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                READ_STORAGE_REQUEST);
    }

    void requestWriteStoragePermission() {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
        if (resultCode == RESULT_OK && requestCode == APP_STORAGE_ACCESS_REQUEST_CODE) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted. Now resume your workflow.
            } else {
                Toast.makeText(getApplicationContext(),
                                "Storage permission denied. Recording and playing features won't work",
                                Toast.LENGTH_LONG)
                        .show();

            }

        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (resultCode == RESULT_OK && requestCode == 100) {
            Uri selectedImage = data.getData();
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
            Log.d(TAG, "onActivityResult: " + selectedImage.getPath());

            String contents = null ;
            try
            {
                InputStream in =  mainActivity.getContentResolver().openInputStream(selectedImage) ;
                contents = new BufferedReader(new InputStreamReader(in))
                        .lines().collect(Collectors.joining("\n"));

//                contents = IOUtils.toString(in, StandardCharsets.UTF_8);
                System.out.println(contents);
            } catch (IOException e) {
                toast(e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
                return;
            }

            JSONObject jsonObject = null ;
            try {
                jsonObject = new JSONObject(contents);
            } catch (JSONException e) {
                alert("Cannot load file", e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
            }

            if (jsonObject != null) {
                Log.d(TAG, "onActivityResult: adding collection json " + contents);
                String name = selectedImage.getLastPathSegment();
                if (name.contains(":"))
                    name = name.substring(name.lastIndexOf(":") + 1);
                try {
                    sharedPreferences.edit().putString(name, jsonObject.toString(4)).commit();
                } catch (JSONException e) {
                    MainActivity.alert("Cannot load collection", e.getMessage());
                    Log.e(TAG, "onActivityResult: ", e);
                }
                Set<String> _vals = sharedPreferences.getStringSet("collections", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(name);
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                sharedPreferences.edit().putStringSet("collections", vals).commit();
                /*
                MainActivity.alert("Loaded collection", String.format(
                        "%s was loaded successfully. Restart the app and select the collection from the quick presets drop down menu."
                ));

                 */

                List<String> labels = new ArrayList<String>();
                labels.add("Factory Presets");
                if (vals != null) {
                    Log.d(TAG, "onViewCreated: adding collections " + vals);
                    for (String v: vals) {
                        labels.add(v);
                    }

                    labels.add ("Load from file") ;
                    labels.add ("More presets online") ;
                }

                ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, labels);
                quickPatch.quickSpinner.setAdapter(categoriesDataAdapter);
                MainActivity.alert("Preset Collection Imported", "You can load presets from the collection from Quick Presets -> Collections");
            }

            Log.d(TAG, "onActivityResult: setting wallpaper: " + selectedImage.toString());
        }

        if (resultCode == RESULT_OK && (requestCode == 1001 || requestCode == 1002)) {
            Uri selectedImage = data.getData();
            Log.d(TAG, "onActivityResult: trying to add " + selectedImage.toString());
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;

            if (requestCode == 1001) {
                Set<String> _vals = sharedPreferences.getStringSet("tracks", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(selectedImage.toString());
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                tracks.load(selectedImage);
                sharedPreferences.edit().putStringSet("tracks", vals).commit();
            }
            else {
                Set<String> _vals = sharedPreferences.getStringSet("drums", null) ;
                Set<String> vals = new ArraySet<>(_vals);
                if (vals == null) {
                    vals = new ArraySet<>();
                }
                vals.add(selectedImage.toString());
                Log.d(TAG, "onActivityResult: adding collection to list: " + vals.toString());
                drums.load(selectedImage);
                sharedPreferences.edit().putStringSet("drums", vals).commit();
            }
        }

        if (resultCode == RESULT_OK && requestCode > 4999 && requestCode < 5020) {
            int plugin = requestCode - 5000 ;
            AudioDecoder audioDecoder = new AudioDecoder(this);
            MediaCodecList supported = new MediaCodecList(MediaCodecList.ALL_CODECS);
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

//                if (!codecInfo.isEncoder()) {
//                    continue;
//                }

                String[] types = codecInfo.getSupportedTypes();
                String feature = "decoder" ;
                if (codecInfo.isEncoder())
                    feature = "encoder" ;
                if (codecInfo.isHardwareAccelerated())
                    feature += " hwaccel";
                String typ = "" ;
                for (String s: types)
                    typ += s + " ";
                Log.d(TAG, String.format ("found supported codec: %s [%s]", typ, feature));
            }

            try {
                int samplerate = AudioEngine.getSampleRate() ;
                if (samplerate < 44100 /*aaaaaaaarghhh*/)
                    samplerate = 48000 ;
                float [] samples = audioDecoder.decode(data.getData(), null, samplerate);
                AudioEngine.setPluginBuffer(samples, plugin);
            } catch (IOException e) {
                toast(e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (AUDIO_EFFECT_REQUEST != requestCode && requestCode != READ_STORAGE_REQUEST && requestCode != WRITE_STORAGE_REQUEST &&
                PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
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

        if (AUDIO_EFFECT_REQUEST == requestCode) {
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

        if (PERMISSION_REQUEST_CODE_CAMERA == requestCode &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Thread initCamera = new Thread(new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            createTextureView();
                        }
                    });
                }
            });
            initCamera.start();
        }
    }

    public static int getDominantColor(Bitmap bitmap) {
        // haha!
        return bitmap.getPixel(0, 0);
    }

    @ColorInt
    public static int adjustAlpha(@ColorInt int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    static String typeToString(int type) {
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

    public void proDialog() {
        Intent intent = new Intent(this, com.shajikhan.ladspa.amprack.Purchase.class);
        startActivity(intent);
    }

    AlertDialog createPluginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        linearLayoutPluginDialog = (ConstraintLayout) inflater.inflate(R.layout.load_plugin_dialog, null);
        /*
        if (useTheme) {
            Log.d(TAG, "createPluginDialog: skinning background");
            skinEngine.view(linearLayoutPluginDialog, "wallpaper", "bg", SkinEngine.Resize.Width, 1);
        }

         */

        EditText editText = (EditText) linearLayoutPluginDialog.findViewById(R.id.pl_search);
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

        ToggleButton toggleButton = (ToggleButton) linearLayoutPluginDialog.findViewById(R.id.pl_favs);
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

        Iterator<String> keys = MainActivity.pluginCategories.keys();
        List<String> categories = new ArrayList<String>();

        while (keys.hasNext()) {
            String key = keys.next();
            Log.d(TAG, "pluginCategory: key " + key);
            categories.add(key);
        }

        ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        categoriesDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pluginDialogCategorySpinner = (Spinner) linearLayoutPluginDialog.findViewById(R.id.plugin_types);
        // attaching data adapter to spinner
        pluginDialogCategorySpinner.setAdapter(categoriesDataAdapter);
        pluginDialogCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String category = ((TextView) view).getText().toString();
                Log.d(TAG, "onItemSelected: selected category " + category);
                pluginDialogAdapter.filterByCategory(category);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pluginDialogWallpaper = linearLayoutPluginDialog.findViewById(R.id.pl_wallpaper);

        builder.setView(linearLayoutPluginDialog);
        AlertDialog pluginDialog = builder.create();
        Button closeButton = linearLayoutPluginDialog.findViewById(R.id.pl_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pluginDialog.hide();
            }
        });
        // Add action buttons
        /*
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                    }
                }) ;
            */
        return pluginDialog;
    }

    public void addPluginToRack(int pluginID) {
        int library = pluginID / 100;
        int plug = pluginID - (library * 100);
        Log.d(TAG, "addPluginToRack: loading from " + sharedLibraries.length + " LADSPA and " + sharedLibrariesLV2.length + " LV2 plugins");
        Log.d(TAG, "Adding plugin: " + library + ": " + plug);
        int ret = -1;
        if (lazyLoad == false)
            ret = AudioEngine.addPlugin(library, plug);
        else {
            if (library > 149 /* because we have 149 lADSPA libraries */) {
                ret = AudioEngine.addPluginLazyLV2(sharedLibrariesLV2[library - sharedLibraries.length], plug);
            } else
                ret = AudioEngine.addPluginLazy(sharedLibraries[library], plug);
        }

        dataAdapter.addItem(pluginID, ret);

        Toast.makeText(context, "Added plugin to rack", Toast.LENGTH_LONG).show();

//        Snackbar.make(recyclerView, "Added plugin to rack", Snackbar.LENGTH_LONG)
//                .show();
    }

    void loadPlugins() {
        if (AudioEngine.getTotalPlugins() != 0)
            return;
//        String[] tapPlugins = context.getResources().getStringArray(R.array.tap_plugins);
        sharedLibraries = context.getResources().getStringArray(R.array.ladspa_plugins);
        sharedLibrariesLV2 = context.getResources().getStringArray(R.array.lv2_plugins);

//        sharedLibraries.add (sharedLibrariesLV2);

        if (lazyLoad == false) {
            for (String s : sharedLibraries) {
                AudioEngine.loadLibrary(/*"lib" + */s);
            }

            AudioEngine.loadPlugins();
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
    }

    String presetToString() {
        int totalPresets = AudioEngine.getActivePlugins();
        JSONObject preset = new JSONObject();

        for (int i = 0; i < totalPresets; i++) {
            JSONObject jo = new JSONObject();
            String vals = "";

            float[] values = AudioEngine.getActivePluginValues(i);
            for (int k = 0; k < values.length; k++) {
                vals += values[k];
                if (k < values.length - 1) {
                    vals += ";";
                }
            }

            try {
                jo.put("name", AudioEngine.getActivePluginName(i));
                jo.put("controls", vals);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                preset.put(String.valueOf(i), jo.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return preset.toString();
    }

    Map presetToMap() {
        int totalPresets = AudioEngine.getActivePlugins();
        Map<String, Map> preset = new HashMap<>();

        for (int i = 0; i < totalPresets; i++) {
            Map<String, String> jo = new HashMap<>();
            String vals = "";

            float[] values = AudioEngine.getActivePluginValues(i);
            for (int k = 0; k < values.length; k++) {
                vals += values[k];
                if (k < values.length - 1) {
                    vals += ";";
                }
            }

            jo.put("name", AudioEngine.getActivePluginName(i));
            jo.put("controls", vals);

            preset.put(String.valueOf(i), jo);
        }

        return preset;
    }


    /*  The problem with this is that we can only get viewholders which are visible on screen.
        See #19 https://github.com/djshaji/amp-rack/issues/19
     */
    String _presetToString() throws JSONException {
        JSONObject preset = new JSONObject();
        if (dataAdapter == null)
            return null;

        for (int i = 0; i < dataAdapter.getItemCount(); i++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i + " of " + dataAdapter.getItemCount(), null);
                continue;
            }

            JSONObject jo = new JSONObject();
            String vals = "";

            for (int k = 0; k < holder.sliders.size(); k++) {
                vals += holder.sliders.get(k).getValue();
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

        return preset.toString();
    }

    Map _presetToMap() throws JSONException {
        Map<String, Map> preset = new HashMap<>();
        if (dataAdapter == null)
            return null;

        for (int i = 0; i < dataAdapter.getItemCount(); i++) {
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                Log.e(TAG, "presetToString: holder is null for " + i, null);
                continue;
            }

            Map<String, String> jo = new HashMap<>();
            String vals = "";

            for (int k = 0; k < holder.sliders.size(); k++) {
                vals += holder.sliders.get(k).getValue();
                if (k < holder.sliders.size() - 1) {
                    vals += ";";
                }
            }

            jo.put("name", (String) holder.pluginName.getText());
            jo.put("controls", vals);

            preset.put(String.valueOf(i), jo);
        }

        return preset;
    }


    public void printActivePreset() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset;
        preset = presetToString();

        if (preset == null)
            return;
        Log.d(TAG, "printActivePreset: " + preset.toString());
    }

    public void saveActivePreset() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset;
        preset = presetToString();
        if (preset == null)
            Log.d(TAG, "saveActivePreset: preset is null");
        else {
            sharedPreferences.edit().putString("activePreset", preset).apply();
            Log.d(TAG, "saveActivePreset: saved " + preset);
        }

        defaultSharedPreferences.edit().putFloat("inputVolume", inputVolume.getValue()).apply();
        defaultSharedPreferences.edit().putFloat("outputVolume", outputVolume.getValue()).apply();
        defaultSharedPreferences.edit().putBoolean("toggleMixer", toggleMixer.isChecked()).apply();

        Log.d(TAG, "saveActivePreset: toggle mixer: " + !toggleMixer.isChecked());
        Log.d(TAG, "saveActivePreset: Saved preset: " + preset);
    }

    void loadActivePreset() {
        if (safeMode) {
            Log.d(TAG, "loadActivePreset: skipping loading preset");
            return;
        }

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String preset = sharedPreferences.getString("activePreset", null);
        AudioEngine.toggleMixer(sharedPreferences.getBoolean("toggleMixer", true));
        Log.i(TAG, "loadActivePreset: loading preset:\n" + preset);

        if (preset != null) {
            Log.d(TAG, "loadActivePreset: " + preset);
            loadPreset(preset);
        } else
            Log.d(TAG, "loadActivePreset: active preset is null or empty");
    }

    void loadPreset(Map map) {
        JSONObject jsonObject = new JSONObject(map);
        String controls;
        try {
            controls = (String) jsonObject.get("controls").toString();
            loadPreset(controls.toString());
        } catch (JSONException e) {
            MainActivity.toast("Cannot load preset: " + e.getMessage());
            e.printStackTrace();
        }


    }

    void loadPreset(String preset) {
        // forgot to add this. that this was forgotten was very difficult to guess
        AudioEngine.bypass(true);
        AudioEngine.clearActiveQueue();

        Log.d(TAG, "loadPreset: " + preset);
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(preset);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadPreset: Unable to load preset\n" + preset, e);
            AudioEngine.bypass(false);
            return;
        }

        int items = dataAdapter.totalItems;
        if (items > 0) {
            Log.d(TAG, "loadPreset: already loaded something, deleting ...");
//            dataAdapter.deleteAll();
            dataAdapter.reset();
        }

        int plugin = 0;
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONObject jo;
            try {
                Log.d(TAG, "loadPreset: trying preset " + key + ": " + jsonObject.getString(key));
                jo = new JSONObject(jsonObject.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse key: " + key, e);
                continue;
            }

            String name, controls;
            try {
                name = jo.getString("name");
                controls = jo.getString("controls");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "loadPreset: unable to parse name or controls for key: " + key, e);
                continue;
            }

            int ret = -1;
            if (!lazyLoad)
                AudioEngine.addPluginByName(name);
            else
                addPluginByName(name);
            Log.d(TAG, "loadPreset: Loaded plugin: " + name);
            String[] control = controls.split(";");


            DataAdapter.ViewHolder holder = null;
            if (dataAdapter.holders.size() == 0)
                Log.e(TAG, "loadPreset: data adapter holders is zero", null);
            else
                dataAdapter.holders.get(plugin);
            if (holder == null) {
                Log.e(TAG, "loadPreset: cannot find holder for " + (ret - 1), null);
            }


            Log.d(TAG, "loadPreset: loading " + control.length + " controls from " + controls);
            for (int i = 0; i < control.length; i++) {
                Log.d(TAG, "loadPreset: " + i + ": " + control[i]);
                //                holder.sliders.get(i).setValue(Integer.parseInt(control [i]));
                float savedValue = -6906;// aaaaargh
                try {
                    savedValue = Float.parseFloat(control[i]);
                } catch (Exception e) {
                    Log.e(TAG, "loadPreset: cannot load saved float value for " + control[i], e);
                    continue;
                }

                AudioEngine.setPresetValue(plugin, i, savedValue);
                // forgot this too
                AudioEngine.setPluginControl(plugin, i, savedValue);
            }

            dataAdapter.addItem(ret, ret);
            plugin++;
        }

        AudioEngine.bypass(false);
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

    public static void toast(String text) {
        Toast.makeText(context,
                        text,
                        Toast.LENGTH_LONG)
                .show();

    }

    public static void alert(String title, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text)
                .setTitle(title)
                .setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void heartPlugin(String name) {
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

    public void unheartPlugin(String name) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return;
        }

        set.remove(name);
        sharedPreferences.edit().putStringSet("favoritePresets", set).apply();
    }

    boolean isPluginHearted(String plugin) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);
        if (set == null) {
            // no hearted presets
            return false;
        }

        return set.contains(plugin);
    }

    Set getHeartedPlugins() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        Set<String> set = sharedPreferences.getStringSet("favoritePresets", null);

        return set;
    }

    void applyPreferencesDevices() {
        // Audio Devices
        String input = defaultSharedPreferences.getString("input", "-1");
        String output = defaultSharedPreferences.getString("output", "-1");
        Log.d(TAG, "applyPreferences: [devices] " + String.format("input: %s, output: %s", input, output));

        Log.d(TAG, "applyPreferencesDevices: " + String.format(
                "[preferences] playback device: %s, recording device: %s",
                output, input
        ));
        AudioEngine.setRecordingDeviceId(new Integer(input));
        AudioEngine.setPlaybackDeviceId(new Integer(output));

        AudioEngine.setLowLatency(defaultSharedPreferences.getBoolean("latency", true));
        int sampleRate = 48000;
        try {
            sampleRate = Integer.valueOf(defaultSharedPreferences.getString("sample_rate", "48000"));
        } catch (ClassCastException e) {
            Log.e(TAG, "applyPreferencesDevices: cannot get default sample rate from preference: " + defaultSharedPreferences.getString("sample_rate", null), e);
        }
        AudioEngine.setSampleRate(sampleRate);
    }

    void applyPreferencesExport() {
        exportFormat = defaultSharedPreferences.getString("export_format", "1");
        AudioEngine.setExportFormat(Integer.parseInt(exportFormat));
        Integer bitRate = Integer.valueOf(defaultSharedPreferences.getString("opus_bitrate", "64"));
        Log.d(TAG, "applyPreferencesExport: setting bitrate " + bitRate * 1000);
        AudioEngine.setOpusBitRate(bitRate * 1000);
        if (proVersion == false) {
            AudioEngine.setExportFormat(0);
        }
    }

    public static void printDebugLog() {
        AudioEngine.debugInfo();

    }

    public static void printActiveChain () {
        AudioEngine.printActiveChain();
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
                Log.d(TAG, "handlePurchase: purchase already acknowledged. Turning on pro features");
                proVersion = true;
            }
        } else {
            Log.d(TAG, "handlePurchase: not purchased (" + purchase.getPurchaseState() + ')');
        }

    }

    public static Bitmap scaleBackground(Bitmap originalImage, int width, int height) {
        Bitmap background = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

        float originalWidth = originalImage.getWidth();
        float originalHeight = originalImage.getHeight();

        Canvas canvas = new Canvas(background);

//        float scale = width / originalWidth;
        float scale = height / originalHeight;

//        float xTranslation = 0.0f;
//        float yTranslation = (height - originalHeight * scale) / 2.0f;
        float xTranslation = (width - originalWidth * scale) / 2.0f;
        float yTranslation = 0.0f;

        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(originalImage, transformation, paint);
        return background;

    }

    public static void applyWallpaper(Context _context, Window window, Resources resources, ImageView imageView, int width, int height) {
        String mTheme = PreferenceManager.getDefaultSharedPreferences(context).getString("color_scheme", "Theme.AmpRack") ;
        Log.d(TAG, "applyWallpaper: setting native theme " + mTheme);
        SkinEngine.setColorScheme((MainActivity) context, mTheme);

        if (lowMemoryMode) {
            imageView.setBackgroundColor(_context.getResources().getColor(R.color.black));
            return;
        }

        String wallpaper = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", null);
        Log.d(TAG, "applyWallpaper: wallpaper: " + wallpaper);

        if (useTheme && skinEngine != null) {
            skinEngine.setNativeTheme();
            skinEngine.wallpaper(imageView);
            return ;
        }

        Bitmap bitmap = null;
        if (wallpaper != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(_context.getContentResolver(), Uri.parse(wallpaper));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);
    }

    public static void applyWallpaper1(Context _context, Window window, Resources resources, ImageView imageView, int width, int height) {
        if (useTheme) {
            skinEngine.setNativeTheme();
            skinEngine.wallpaper(imageView);
            return ;
        }

        String resIdString = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", "700032");
//        String resIdString = PreferenceManager.getDefaultSharedPreferences(_context).getString("background", "a1");
        Bitmap bitmap = null;
        switch (resIdString) {
            default:
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(_context.getContentResolver(), Uri.parse(resIdString));
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

                bitmap = scaleBackground(bitmap, width, height);
                context.setTheme(R.style.Theme_Bright);
                break;
            case "Space":
                if (lowMemoryMode)
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg1);
                else
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                break;
            case "Water":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.water);
                break;
            case "Fire":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.fire);
                break;
            case "Sky":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.sky);
                break;
            case "Earth":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_earth);
                break;
            case "a1":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a1);
                context.setTheme(R.style.Theme_1);
                break;
            case "700032":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                context.setTheme(R.style.Theme_AmpRack);
                break;
            case "a2":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a2);
                context.setTheme(R.style.Theme_2);
                break;
            case "a3":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a3);
                context.setTheme(R.style.Theme_3);
                break;
            case "a4":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.a4);
                context.setTheme(R.style.Theme_4);
                break;
            case "2.9 Beta":
                bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg);
                context.setTheme(R.style.Theme_29beta);
                break;
        }

        if (bitmap == null) {
            Log.e(TAG, "applyWallpaper: No suitable bg from settings", null);
            return;
        }

        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);
        Paint paint = new Paint();
        PorterDuffColorFilter filter = new PorterDuffColorFilter(0xffaaaaaa, PorterDuff.Mode.MULTIPLY);
        imageView.getDrawable().setColorFilter(filter);
        paint.setColorFilter(filter);
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 10, 10);

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(croppedBitmap, 0, 0, paint);
        int color = (int) (getDominantColor(croppedBitmap));

        Log.d(TAG, "applyWallpaper: dominant color " + color + ": " + Color.parseColor("#6c6f6e"));
        window.setStatusBarColor(color);
    }

    static public JSONObject loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
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

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: cannot parse json", e);
        }

        return jsonObject;
    }

    public static void shareFile(File file) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri contentUri = null;
        try {
            contentUri = FileProvider.getUriForFile(context, "com.shajikhan.ladspa.amprack.fileprovider", file);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, "shareFile: ", illegalArgumentException);
            return;
        }

        intentShareFile.setType("audio/*");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, contentUri);

        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                "Sharing Audio File...");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.app_name) + " recorded audio ...");

        intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));

    }

    public void runCommand(String command) {
        switch (command) {
            case "saveactivepreset":
                saveActivePreset();
                break;
            default:
                alert(this.getClass().getSimpleName(), "Command not supported: " + command);
                break;
        }
    }

    public void addPluginByName(String pluginName) {
//        Log.d(TAG, "addPluginByName: " + pluginName);
//        Log.d(TAG, "addPluginByName: " + availablePlugins.toString());
        JSONObject plugins = availablePlugins;
        Iterator<String> keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (plugins.get(key) instanceof JSONObject) {
//                    Log.d(TAG, "onCreate: key " + key);
                    JSONObject object = plugins.getJSONObject(key);
                    // do something with jsonObject here
                    String name = object.getString("name");
                    String id = object.getString("id");
                    int plugin = object.getInt("plugin");
                    String lib = object.getString("library");

//                    Log.d(TAG, String.format ("addPluginByName: comparing %s and %s", pluginName, name));

                    if (pluginName.equals(name)) {
                        Log.d(TAG, "addPluginByName: found plugin " + name);
                        AudioEngine.addPluginLazy(lib, plugin);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.e(TAG, "addPluginByName: unable to find plugin name " + pluginName + " ... trying LV2 plugins ...");
        addPluginByNameLV2(pluginName);
    }

    public void addPluginByNameLV2(String pluginName) {
//        Log.d(TAG, "addPluginByName: " + pluginName);
        Log.d(TAG, "addPluginByName: " + availablePluginsLV2.toString());
        JSONObject plugins = availablePluginsLV2;
        Iterator<String> keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (plugins.get(key) instanceof JSONObject) {
//                    Log.d(TAG, "onCreate: key " + key);
                    JSONObject object = plugins.getJSONObject(key);
                    // do something with jsonObject here
                    String name = object.getString("name");
                    String id = object.getString("id");
                    int plugin = object.getInt("index");
                    String lib = object.getString("library");

//                    Log.d(TAG, String.format ("addPluginByName: comparing %s and %s", pluginName, name));

                    if (pluginName.equals(name)) {
                        Log.d(TAG, "addPluginByName: found plugin " + name);
                        AudioEngine.addPluginLazyLV2(lib, plugin);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.e(TAG, "[LV2] addPluginByName: unable to find plugin name " + pluginName);
    }

    static public String readAssetFile(Context context, String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
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

        return json ;
    }

    public void testLV2 () {
        Log.d(TAG, "testLV2: " +
                String.format("%d\t%d",
                        getDisplayRotation(),getCameraSensorOrientation(camera2.cameraCharacteristicsHashMap.get(camera2.cameraId))));
//        Log.d(TAG, "testLV2: " + getLV2Info("eql"));
//        AudioEngine.testLV2();
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        audioDevicesInput = audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        for (AudioDeviceInfo audioDeviceInfo: audioDevicesInput)
            Log.d(TAG, String.format ("[%s] %s: %s", audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
        for (AudioDeviceInfo audioDeviceInfo: audioDevicesOutput)
            Log.d(TAG, String.format ("[%s] %s: %s", audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
//        Log.d(TAG, "testLV2: " + getLV2Info("mda-Limiter.so", "http://drobilla.net/plugins/mda/Limiter"));
    }

    public static String getLV2Info (String libraryName, String plugin) {
        String pluginName ;
        if (plugin.indexOf("#") != -1)
            pluginName = plugin.split("#")[1];
        else {
            String [] p = plugin.split("/");
            pluginName = p [p.length -1];
        }

        Log.d(TAG, "getLV2Info: lv2/" + libraryName + "/" + pluginName + ".json");
        JSONObject jsonObject = ConnectGuitar.loadJSONFromAssetFile(context, "lv2/" + libraryName + "/" + pluginName + ".json");
        return jsonObject.toString();
    }

    public static boolean isLibraryLV2 (String libraryName) {
        return Arrays.asList(sharedLibrariesLV2).contains(libraryName) ;
    }

    public boolean isPluginLV2 (String pluginName) {
//        Log.d(TAG, "isPluginLV2: " + pluginName + " in " + lv2Plugins.toString() + " = " + lv2Plugins.contains(pluginName));
        return lv2Plugins.contains(pluginName);
    }

    static ArrayList <Float> tunerBuffer = new ArrayList<>();
    static Pitch pitch = new Pitch(AudioEngine.getSampleRate());
    static void setMixerMeter (float inputValue, float outputValue) {
        inputMeter.setProgress((int) (inputValue * 100));
        outputMeter.setProgress((int) (outputValue * 100));
    }

    static int audioIn = 0, audioOut = 0 ;
    static void setTuner (float [] data, int size) {
        if (! mainActivity.tunerEnabled)
            return ;
        double freq = pitch.computePitchFrequency(data);
        String note = " - " ;
        double cents = 0 ;
        double diff = 0 ;
        float targetFrequency = 0;
        for (int i = 0 ; i < pitch.notes.length ; i ++) {
            targetFrequency = Float.valueOf(pitch.notes [i][1]) ;
            cents = 1200 * Math.log(freq / targetFrequency) / Math.log(2);
            if (abs (cents) < 50) {
                note = pitch.notes [i][0];
                diff = freq - targetFrequency ;
                break ;
            }
        }

//        Log.d(TAG, String.format(
//                "%f %s %f",
//                data [0], note, freq
//        )) ;
        String finalNote = note;
        double finalCents = cents;
        float finalTargetFrequency = targetFrequency;
        double finalDiff = diff;
        mainActivity.handler.post(() -> {
//            Log.d(TAG, "setTuner: " + String.format("%s %f [%f] %f", finalNote,freq, finalTargetFrequency, finalCents));
            // write your code here
            if (abs (finalDiff) < 1){
                mainActivity.tuner.setText("✔ " + finalNote);
            } else if (finalDiff < 0) {
                mainActivity.tuner.setText("⬆ " +  + Math.round(finalDiff) + "  " + finalNote );
            } else {
                mainActivity.tuner.setText("⬇ " + Math.round(finalDiff) + "  " + finalNote );
            }
        });

        tunerBuffer.clear();
    }

    static void setMixerMeterSwitch (float inputValue, boolean isInput) {
        if (inputValue < 0.001)
            return;
//        Log.d(TAG, "setMixerMeterSwitch() called with: inputValue = [" + inputValue + "], isInput = [" + isInput + "]");
        if (isInput) {
            inputMeter.setProgress((int) (inputValue * 100));
            /*
            if (tunerBuffer.size() < 32) {
                Log.d(TAG, tunerBuffer.size() + ": setMixerMeter: " + inputValue);
                tunerBuffer.add(inputValue);
            } else {
                double freq = pitch.computePitchFrequency(tunerBuffer);
                String note = " - " ;
                double diff = 0 ;
                for (int i = 0 ; i < pitch.notes.length ; i ++) {
                    double _diff = Float.valueOf(pitch.notes [i][1]) - freq ;
                    if (abs (_diff) < 31) {
                        diff = _diff;
                        note = pitch.notes [i][0];
                        break ;
                    }
                }

                Log.d(TAG, String.format(
                        "%s %f %f",
                        note, freq, diff
                        )) ;
                String finalNote = note;
                double finalDiff = diff;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // write your code here
                        mainActivity.tuner.setText(finalNote);
                        if (finalDiff < 0) {
                            mainActivity.tuner.setCompoundDrawables(mainActivity.getResources().getDrawable(R.drawable.ic_baseline_keyboard_arrow_up_24), null,null,null);
                        } else {
                            mainActivity.tuner.setCompoundDrawables(null,null,null, mainActivity.getResources().getDrawable(R.drawable.ic_baseline_keyboard_arrow_down_24));
                        }
                    }
                });

                tunerBuffer.clear();
            }
             */
        }
        else {
            outputMeter.setProgress((int) (inputValue * 100));
//            Log.d(TAG, "setMixerMeterSwitch: " + inputValue);
            if (mainActivity.triggerRecord) {
                if (mainActivity.recording) {
                    if (inputValue < 0.1) {
//                        mainActivity.record.setChecked(false);
                        AudioEngine.toggleRecording(false);
                        mainActivity.recording = false;
                        mainActivity.triggerRecord = false;
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                    }
                } else {
                    if (inputValue > 0.3) {
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
//                        mainActivity.record.setChecked(true);
                        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
                        Date date = new Date();
                        mainActivity.lastRecordedFileName = formatter.format(date);
                        mainActivity.lastRecordedFileName = mainActivity.dir.getAbsolutePath() + "/" + mainActivity.lastRecordedFileName ;
                        AudioEngine.setFileName(mainActivity.lastRecordedFileName);
                        switch (mainActivity.exportFormat) {
                            case "0":
                            default:
                                mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".wav" ;
                                break ;
                            case "1":
                                mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".ogg" ;
                                break ;
                            case "2":
                                mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".mp3" ;
                                break ;
                        }

                        mainActivity.triggerRecordedSomething = true ;
                        mainActivity.recording = true;
                        Log.d(TAG, "setMixerMeterSwitch: triggering recording");
                        AudioEngine.toggleRecording(true);
//                        mainActivity.triggerRecordToggle.setChecked(false);
                    }
                }
            }

        }
    }

    static void setMixerMeterInput (float inputValue) {
//        Log.d(TAG, "setMixerMeterInput() called with: inputValue = [" + inputValue + "]");
        inputMeter.setProgress((int) (inputValue * 100));
    }
    static void setMixerMeterOutput (float outputValue) {
//        Log.d(TAG, "setMixerMeterOutput() called with: outputValue = [" + outputValue + "]");
        outputMeter.setProgress((int) (outputValue * 100));
    }

    /* the following supposibly adds hardware keyboard shortcut support a.k.a volume keys

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (onOff != null) {
            if (onOff.isChecked() == false)
                return super.onKeyLongPress(keyCode, event);
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                rack.patchDown.performClick();
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                rack.patchUp.performClick();
        }

        return super.onKeyLongPress(keyCode, event);
    }
     */

    public static void drummer () {
        Intent intent = new Intent(context, DrumMachineActivity.class);
        mainActivity.startActivity(intent);
     }

     public static void resetOnboard () {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("currentVersion").apply();
     }

     public static void featured () {
         Intent intent = new Intent(context, FeaturedVideos.class);
         mainActivity.startActivity(intent);
     }

     /*
    public boolean hotkeys (int keyCode, KeyEvent event) {
        Log.d(TAG, "hotkeys() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        switch (keyCode) {
            case KeyEvent.KEYCODE_SPACE:
                onOff.setChecked(!onOff.isChecked());
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return hotkeys(keyCode, event);
    }

      */
    public static void printPluginsAll () {
        Log.d(TAG, "printPlugins: " + String.format(
                "Shared libraries: %d", sharedLibrariesLV2.length
        ));

        for (int i = 0 ; i < sharedLibrariesLV2.length ; i ++) {
            Log.d(TAG, String.format("printPluginsAll [%d]: %s", sharedLibraries.length + i, sharedLibrariesLV2 [i]));
        }
    }

    public void saveCollection () {
        ArrayList selectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        List<CharSequence> strings = new ArrayList<>();
        if (presets.fragmentStateAdapter.myPresets.myPresetsAdapter == null) {
            alert("Load presets first", "To save preset collection to file, switch to the Presets tab from the bottom navigation menu");
            return;
        }
        for (int i = 0 ; i < presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.size() ; i ++) {
            strings.add(presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.get(i).get("name").toString());
        }

        builder.setTitle("Select presets for collection")
                .setMultiChoiceItems((strings.toArray(new
                                CharSequence[strings.size()])), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    if (which < selectedItems.size())
                                        selectedItems.remove(which);
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the selectedItems results somewhere
                        // or return them to the component that opened the dialog
                        Log.d(TAG, "onClick: " + selectedItems.toString());
                        JSONObject collection = new JSONObject();
                        for (int i = 0 ; i < presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.size() ; i ++) {
                            if (selectedItems.contains(i)) {
                                try {
                                    JSONObject jsonObject = new JSONObject(presets.fragmentStateAdapter.myPresets.myPresetsAdapter.allPresets.get(i)) ;
                                    collection.put(String.valueOf(i), jsonObject);
                                } catch (JSONException e) {
                                    Log.e(TAG, "onClick: ", e);
                                }
                            }
                        }

                        AlertDialog.Builder _Builder1 = new AlertDialog.Builder(mainActivity);
                        _Builder1.setTitle("Enter file name for preset collection");
                        LinearLayout linearLayout = new LinearLayout(context);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        TextView textView = new TextView(context);
                        textView.setText("File will be saved to " + context.getExternalFilesDir(
                                Environment.DIRECTORY_DOWNLOADS));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                        layoutParams.setMargins(10,10,10,10);
                        linearLayout.setLayoutParams(layoutParams);
                        linearLayout.setPadding(50,50,50,50);
                        linearLayout.addView(textView);

                        EditText editText = new EditText(context);
                        linearLayout.addView(editText);
                        _Builder1.setView(linearLayout).
                                setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String filename = String.valueOf(editText.getText());

                                        if (filename != null) {
                                            File file = new File(context.getExternalFilesDir(
                                                    Environment.DIRECTORY_DOWNLOADS) + "/" + filename +".txt");
                                            FileOutputStream stream = null;
                                            try {
                                                stream = new FileOutputStream(file);
                                            } catch (FileNotFoundException e) {
                                                alert("Cannot write file", e.getMessage());
                                                throw new RuntimeException(e);
                                            }

                                            if (stream != null) {
                                                try {
                                                    String data = collection.toString(4);
                                                    stream.write(data.getBytes());
                                                    Log.d(TAG, "onClick: " + data);
                                                    stream.close();
                                                } catch (IOException e) {
                                                    alert ("Cannot write file", e.getMessage());
                                                    Log.e(TAG, "onClick: ", e);
                                                } catch (JSONException e) {
                                                    alert ("Cannot write file", e.getMessage());
                                                    Log.e(TAG, "onClick: ", e);
                                                }

                                                alert("Wrote file succesfully",
                                                        String.format(
                                                                "Collection saved to file:\n%s",
                                                                file.getName()
                                                        ));
                                            }

                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", null);
                        Log.d(TAG, "onClick: show dilog");
                        _Builder1.create().show();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        Dialog dialog = builder.create() ;
        dialog.show();
    }

    public void AudioRecordTest () {
        Log.d(TAG, "AudioRecordTest: ");
        int min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord aRecord = null;

        aRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 48000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, 1024*4);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Audio Record Test");
        builder.setPositiveButton("Close", null);

        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView textView = new TextView(getApplicationContext());
        textView.setText("Toggle record");
        ToggleButton toggleButton = new ToggleButton(getApplicationContext());
        linearLayout.addView(textView);
        linearLayout.addView(toggleButton);
        builder.setView(linearLayout);
        int maxJitter = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack track = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxJitter, AudioTrack.MODE_STREAM);

        Dialog dialog = builder.create();
        Pitch p = new Pitch(48000);
        AudioRecord finalARecord = aRecord;
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    finalARecord.startRecording();
                    float[] lin = new float[1024*4];
                    while (finalARecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int num = finalARecord.read(lin, 0, 1024 *4, AudioRecord.READ_NON_BLOCKING);
                        double pitchFrequency = p.computePitchFrequency (lin);
                        Log.d(TAG, "onCheckedChanged: " + lin [0] + " " + pitchFrequency);
//                        break ;

                    }
                } else {
                    finalARecord.stop();
                }
            }
        } ;

        toggleButton.setOnCheckedChangeListener(listener);
        dialog.show();
    }

    public void cameraPreview() {
        Intent intent = new Intent(context, Camera.class);
        startActivity(intent);

    }

    public int [] safeAudioDevices = {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_AUX_LINE,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_IP,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET
    } ;

    public boolean isHeadphonesPlugged(){
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for(AudioDeviceInfo deviceInfo : audioDevices){
            if(Arrays.stream(safeAudioDevices).anyMatch(x -> x == deviceInfo.getType())){
                return true;
            }
        }

        return false;
    }

    public static void setAudioDevice () {
        AudioDeviceInfo[] audioDevicesInput, audioDevicesOutput ;
        audioDevicesInput = mainActivity.audioManager.getDevices (AudioManager.GET_DEVICES_INPUTS) ;
        audioDevicesOutput = mainActivity.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ;

        String current_in = mainActivity.defaultSharedPreferences.getString("input", "");
        String current_out = mainActivity.defaultSharedPreferences.getString("output", "");

        if (current_out == "")
            current_out = "0" ;
        if (current_in == "")
            current_in = "0";

        Log.d(TAG, String.format ("%s: %s", current_in, current_out));
        int cin = 0, cout = 0, counter = 0 ; ;

        for (AudioDeviceInfo audioDeviceInfo: audioDevicesInput) {
            Log.d(TAG, String.format("<%s> [%s] %s: %s", current_in, audioDeviceInfo.getType(), audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
            if (Integer.parseInt(current_in) == audioDeviceInfo.getId())
                cin = counter + 1;
            counter ++ ;
        }

        counter = 0 ;
        for (AudioDeviceInfo audioDeviceInfo: audioDevicesOutput) {
            Log.d(TAG, String.format("%b <%s> [%s] %s: %s",
                    Integer.valueOf(current_out) == Integer.valueOf(audioDeviceInfo.getId()),
                    current_out, audioDeviceInfo.getType(),
                    audioDeviceInfo.getId(), audioDeviceInfo.getProductName()));
            if (Integer.parseInt(current_out) == audioDeviceInfo.getId())
                cout = counter + 1;
            counter ++ ;
        }
        Log.d(TAG, String.format ("[defaults] %d: %d", cin, cout));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Get the layout inflater
        LayoutInflater inflater = mainActivity.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.audio_devices_selector, null);
        builder.setView(linearLayout)
                // Add action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        /*
                        try {
                            String in = ((EditText) linearLayout.findViewById(R.id.custom_input_device)).getText().toString() ;
                            if (!in.isEmpty()) {
                                int dev = Integer.parseInt(in) ;
                                AudioEngine.setRecordingDeviceId(dev);
                                Log.d(TAG, String.format ("set record dev: %d", dev));
                                mainActivity.defaultSharedPreferences.edit().putString("input", String.valueOf(dev)).apply();
                            }
                            in = ((EditText) linearLayout.findViewById(R.id.custom_output_device)).getText().toString() ;
                            if (!in.isEmpty()) {
                                int dev = Integer.parseInt(in) ;
                                mainActivity.defaultSharedPreferences.edit().putString("output", String.valueOf(dev)).apply();
                                Log.d(TAG, String.format ("set playback dev: %d", dev));
                                AudioEngine.setPlaybackDeviceId(dev);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "onClick: ", e);
                        }

                         */
                    }
                });
                /*
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

                 */

        int i = 0;
//                HashMap<CharSequence, Integer> inputs = new HashMap<>();
//                HashMap <CharSequence, Integer> outputs = new HashMap<>();
        ArrayList<String> input_s = new ArrayList<>();
        ArrayList<String> output_s = new ArrayList<>();

        input_s.add ("Default");
        output_s.add ("Default");

        for (i = 0; i < audioDevicesInput.length; i++) {
            AudioDeviceInfo deviceInfo = audioDevicesInput[i] ;
            String name = String.format("[%d] %s (%s)", deviceInfo.getId(), deviceInfo.getProductName(), typeToString(deviceInfo.getType()));
//                    inputs.put(name, audioDevicesInput [i].getId()) ;
            input_s.add(name);
        }

        for (i = 0; i < audioDevicesOutput.length; i++) {
            AudioDeviceInfo deviceInfo = audioDevicesOutput[i] ;
            String name = String.format("[%d] %s (%s)", deviceInfo.getId(), deviceInfo.getProductName(), typeToString(deviceInfo.getType()));
//                    outputs.put(name, audioDevicesOutput [i].getId()) ;
            output_s.add(name);
        }

        ArrayAdapter input_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, input_s);
        input_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter output_a = new ArrayAdapter(context, android.R.layout.simple_spinner_item, output_s);
        output_a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner in = (Spinner) linearLayout.findViewById(R.id.input_devices);
        Spinner out = (Spinner) linearLayout.findViewById(R.id.output_devices);
        in.setAdapter(input_a);
        out.setAdapter(output_a);

        in.setSelection(cin);
        out.setSelection(cout);

        in.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    mainActivity.defaultSharedPreferences.edit().putString("input", "-1").apply();
                    return ;
                }

                i-- ;
                mainActivity.defaultSharedPreferences.edit().putString("input", String.valueOf(audioDevicesInput[i].getId())).apply();
//                AudioEngine.setRecordingDeviceId(audioDevicesInput[i].getId());
                mainActivity.defaultInputDevice = i;
                Log.d(TAG, String.format ("set recording device: %d", audioDevicesInput[i].getId()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        out.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    mainActivity.defaultSharedPreferences.edit().putString("output", "-1").apply();
                    return ;
                }

                i-- ;
                AudioEngine.setPlaybackDeviceId(audioDevicesOutput[i].getId());
                mainActivity.defaultSharedPreferences.edit().putString("output", String.valueOf(audioDevicesOutput[i].getId())).apply();
                mainActivity.defaultInputDevice = i;
                Log.d(TAG, String.format ("set playback device: %d", audioDevicesOutput[i].getId()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        builder.show();
    }

    public static void about () {
        Intent intent = new Intent(mainActivity, About.class);
        mainActivity.startActivity(intent);

    }

    public void createTextureView() {
        Log.d(TAG, "createTextureView: creating surface");
        rack.videoTexture = (TextureView) findViewById(R.id.video_texture);
        rack.videoTexture.setSurfaceTextureListener(this);
        if (rack.videoTexture.isAvailable()) {
            onSurfaceTextureAvailable(rack.videoTexture.getSurfaceTexture(),
                    rack.videoTexture.getWidth(), rack.videoTexture.getHeight());

        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height) {
//        createNativeCamera();

//        resizeTextureView(width, height);
//        surface.setDefaultBufferSize(cameraPreviewSize_.getWidth(),
//                cameraPreviewSize_.getHeight());
//        surface_ = new Surface(surface);
//        surfaceTexture = surface;
//        camera2.createCameraPreview();
//        camera2.updatePreview();

//        onPreviewSurfaceCreated(ndkCamera_, surface_);
    }
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        onPreviewSurfaceDestroyed(ndkCamera_, surface_);
//        deleteCamera(ndkCamera_, surface_);
//        ndkCamera_ = 0;
        surface_ = null;
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }


    public void RequestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE_CAMERA);
            return;
        }
        createTextureView();
    }

    void setTextureTransform(CameraCharacteristics characteristics) {
        Size previewSize = getPreviewSize(characteristics);
        int width = previewSize.getWidth();
        int height = previewSize.getHeight();
        int sensorOrientation = getCameraSensorOrientation(characteristics);
        // Indicate the size of the buffer the texture should expect
//        rack.videoTexture.getSurfaceTexture().setDefaultBufferSize(width, height);
        // Save the texture dimensions in a rectangle
        RectF viewRect = new RectF(0,0, rack.videoTexture.getWidth(), rack.videoTexture.getHeight());
        // Determine the rotation of the display
        float rotationDegrees = 0;
        try {
            rotationDegrees = (float)getDisplayRotation();
        } catch (Exception ignored) {
        }
        float w, h;
        if ((sensorOrientation - rotationDegrees) % 180 == 0) {
            w = width;
            h = height;
        } else {
            // Swap the width and height if the sensor orientation and display rotation don't match
            w = height;
            h = width;
        }
        float viewAspectRatio = viewRect.width()/viewRect.height();
        float imageAspectRatio = w/h;
        final PointF scale;
        // This will make the camera frame fill the texture view, if you'd like to fit it into the view swap the "<" sign for ">"
        if (viewAspectRatio < imageAspectRatio) {
            // If the view is "thinner" than the image constrain the height and calculate the scale for the texture width
            scale = new PointF((viewRect.height() / viewRect.width()) * ((float) height / (float) width), 1f);
        } else {
            scale = new PointF(1f, (viewRect.width() / viewRect.height()) * ((float) width / (float) height));
        }
        if (rotationDegrees % 180 != 0) {
            // If we need to rotate the texture 90º we need to adjust the scale
            float multiplier = viewAspectRatio < imageAspectRatio ? w/h : h/w;
            scale.x *= multiplier;
            scale.y *= multiplier;
        }

        Matrix matrix = new Matrix();
        // Set the scale
        matrix.setScale(scale.x, scale.y, viewRect.centerX(), viewRect.centerY());
        if (rotationDegrees != 0) {
            // Set rotation of the device isn't upright
            matrix.postRotate(0 - rotationDegrees, viewRect.centerX(), viewRect.centerY());
        }
        // Transform the texture
        rack.videoTexture.setTransform(matrix);
    }

    int getDisplayRotation() {
        switch (rack.videoTexture.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                return 0;
            case Surface.ROTATION_90:
                return  90;
            case Surface.ROTATION_180:
                return  180;
            case Surface.ROTATION_270:
                return 270;
        }
    }

    Size getPreviewSize(CameraCharacteristics characteristics) {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
        // TODO: decide on which size fits your view size the best
        return previewSizes[0];
    }

    int getCameraSensorOrientation(CameraCharacteristics characteristics) {
        Integer cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (360 - (cameraOrientation != null ? cameraOrientation : 0)) % 360;
    }

    public static void pushToVideo (float [] data, int nframes) {
//        Log.d(TAG, String.format ("%d: %f - %f", nframes, data [0], data [nframes - 1]));
        if (! mainActivity.videoRecording || ! mainActivity.camera2.mMuxerStarted)
            return;

        /*
        ByteBuffer buffer = ByteBuffer.allocate(nframes*2);
        for (int i = 0 ; i < nframes ; i ++)
            buffer.putChar(data [i]);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        bufferInfo.set(0, nframes, mainActivity.camera2.timestamp.get(), 0);
        mainActivity.camera2.mMuxer.writeSampleData(mainActivity.camera2.audioTrackIndex, buffer, bufferInfo);

         */

        AVBuffer buffer = new AVBuffer();
        buffer.size = nframes;
        buffer.bytes = data.clone();
        avBuffer.addLast(buffer);
    }

    private static long computePresentationTimeNsec(long frameIndex, int sampleRate) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / sampleRate;
    }

    void startDemoRecord () {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
        Date date = new Date();
        mainActivity.lastRecordedFileName = formatter.format(date);
        mainActivity.lastRecordedFileName = mainActivity.dir.getAbsolutePath() + "/" + mainActivity.lastRecordedFileName  + ".wav";

        try {
            fileOutputStream = new FileOutputStream(mainActivity.lastRecordedFileName);
        } catch (IOException e) {
            fileOutputStream = null ;
            dataOutputStream = null ;
            throw new RuntimeException(e);
        }

        dataOutputStream = new DataOutputStream(fileOutputStream);
    }

    void stopDemoRecord () {
        if (fileOutputStream == null)
            return;
        try {
            dataOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            fileOutputStream = null ;
            throw new RuntimeException(e);
        }

        fileOutputStream = null ;
        dataOutputStream = null ;
    }

    public static Bitmap getBitmapFromAsset(AssetManager mgr, String path) {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = mgr.open(path);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (final IOException e) {
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }


}