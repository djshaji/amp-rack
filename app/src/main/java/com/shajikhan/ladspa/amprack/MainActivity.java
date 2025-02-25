package com.shajikhan.ladspa.amprack;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
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
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
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
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.net.MacAddress;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, TextureView.SurfaceTextureListener {
    MidiOutputPort midiOutputPort;
    MyReceiver midiReciever;
    private CompanionDeviceManager deviceManager;

    class MyReceiver extends MidiReceiver {
        MainActivity mainActivity ;
        MidiFramer midiFramer ;
        MyReceiver (MainActivity m) {
            mainActivity = m ;
            midiFramer = new MidiFramer(this);
        }

        public void logMidiMessage(byte[] data, int offset, int count) {
            String text = "Received: ";
            for (int i = 0; i < count; i++) {
                text += String.format("0x%02X, ", data[offset + i]);
            }
//            Log.i(TAG, text);
        }

        public void onSend(byte[] data, int offset,
                           int count, long timestamp) throws IOException {
            ShortMessage shortMessage = new ShortMessage(data);
            byte command = (byte) (data[offset] & MidiConstants.STATUS_COMMAND_MASK);
            int channel = (byte) (data[offset] & MidiConstants.STATUS_CHANNEL_MASK);
//            Log.i(TAG, "onSend: recieved message on channel " + channel);
            switch (command) {
                case MidiConstants.STATUS_NOTE_OFF:
//                    noteOff(channel, data[1], data[2]);
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi note off] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    break;
                case MidiConstants.STATUS_NOTE_ON:
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi note on] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
//                    noteOn(channel, data[1], data[2]);
                    break;
                case MidiConstants.STATUS_PITCH_BEND:
                    int bend = (data[2] << 7) + data[1];
//                    pitchBend(channel, bend);
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
//                    Log.d(TAG, String.format ("[midi pitch bend] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    break;
                case MidiConstants.STATUS_PROGRAM_CHANGE:
//                    mProgram = data[1];
//                    mFreeVoices.clear();
//                    Log.d(TAG, "onSend: program change");
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
                    logMidiMessage(data, offset, count);
                    break;
                case MidiConstants.STATUS_CONTROL_CHANGE:
//                    Log.d(TAG, "onSend: control change " );
//                    Log.d(TAG, String.format ("[midi control change] %s [%d] %d: %d",
//                            shortMessage.getCommand(), shortMessage.getChannel(),
//                            // data 1
//                            data [offset+1] & 0xFF,
//                            //data 2
//                            data [offset+2] & 0xFF));
                    mainActivity.processMIDIMessage(channel, data [offset+1] & 0xFF, data [offset+2] & 0xFF);
                    logMidiMessage(data, offset, count);
                    break;
                default:
//                    Log.i(TAG, "onSend: command not understood, instructions unclear ...!");
                    logMidiMessage(data, offset, count);
                    break;
            }
        }
    }

    private static final String TAG = "Amp Rack MainActivity";

    private static final String CHANNEL_ID = "default";
    Surface surface_ = null;
    Spinner pluginDialogSortBy ;
    public static TextView sampleRateLabel ;
    public static ImageView latencyWarnLogo ;
    public static LinearLayout srLayout; ;
    SurfaceTexture surfaceTexture;
    public boolean headphoneWarning = true;
    public boolean experimentalBuild = false;
    static Context context;
    ArrayList <MIDIControl> midiControls ;
    MidiManager midiManager ;
    MidiDevice midiDevice ;
    String midiLastConnectedDevice = null ;
    int midiLastConnectedPort = -1 ;
    static FileOutputStream fileOutputStream = null ;
    static DataOutputStream dataOutputStream = null ;
    static MainActivity mainActivity;
    BluetoothDevice deviceToPair  ;
    boolean videoRecording = false ;
    static boolean tabletMode = false ;
    Camera2 camera2 ;
    Dialog midiAddDialog = null ;
    ToggleButton triggerMidiButton = null ;
    ConstraintLayout midiLayout = null ;
    EditText channelEdit = null, controlEdit = null ;

    public static String price = "$2";
    MediaPlayerDialog mediaPlayerDialog = null;
    private OrientationEventListener orientationEventListener;
    public String favPresetsDir = null ;

    static class AVBuffer {
        float [] bytes ;
        int size ;
    }
    public static LinkedBlockingQueue <AVBuffer> avBuffer = new LinkedBlockingQueue<>();
    public static LinkedBlockingQueue <OnEngineStartListener> engineStartListeners = new LinkedBlockingQueue<>();

    static int avEncoderIndex = 0 ;
    public static long presentationTimeUs = 0;
    int totalBytesRead = 0;

    String lastPresetLoadedPath = null ;
    String lastPresetLoadedUID = null ;

    ExtendedFloatingActionButton fab ;
    Button hidePanel;
    TextView midiDisplay ;
    SwitchMaterial onOff = null ;
    AudioEncoder audioEncoder ;
    String exportFormat = "1";
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
    static public JSONObject pluginCategories, pluginCreators;
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
    private static final int PERMISSION_REQUEST_CODE_BLUETOOTH = 3;
    final static int APP_STORAGE_ACCESS_REQUEST_CODE = 501; // Any value

    // Firebase
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    public Rack rack;
    public Tracks tracks, drums;
    public Presets presets;
    int nag = 0 ;
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
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        midiLastConnectedDevice = defaultSharedPreferences.getString("last_midi", null);
        midiLastConnectedPort = defaultSharedPreferences.getInt("last_midi_port", -1);

        midiManager = (MidiManager)context.getSystemService(Context.MIDI_SERVICE);
        String mac = defaultSharedPreferences.getString("last_bt", null);
        if (mac != null) {
            Log.i(TAG, "onCreate: trying to connect to previously connected bluetooth device " + mac);
            deviceToPair = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
            if (deviceToPair != null) {
                Log.i(TAG, "onCreate: bluetooth device connected !");
                midiManager.openBluetoothDevice(deviceToPair, new MidiManager.OnDeviceOpenedListener() {
                    @Override
                    public void onDeviceOpened(MidiDevice device) {
                        Log.i(TAG, "onDeviceOpened: bluetooth device " + mac);
                    }
                }, null);
            }
        }

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

                            if (tracks.mainActivity == null)
                                tracks.setMainActivity(this);
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

        favPresetsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + "favoritePresets";
        File f = new File(favPresetsDir) ;
        if (! f.exists())
            f.mkdirs();

        hashCommands = new HashCommands(this);
        hashCommands.setMainActivity(this);
        hashCommands.add (this, "AudioRecordTest");
        hashCommands.add (this, "pluginsCrashTest");
        hashCommands.add (this, "resetMIDI");
        hashCommands.add (this, "printMidi");
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
        hashCommands.add (this, "cameraTest");
        hashCommands.add (this, "getLatency");

        headphoneWarning = defaultSharedPreferences.getBoolean("headphone-warning", true);

        midiControls = new ArrayList<>();
        midiReciever = new MyReceiver(this);
        MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
        Log.i(TAG, "onCreate: [midi] last connected device " + midiLastConnectedDevice);
        Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));
        boolean found = false ;
        for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
            Log.d(TAG, String.format ("[midi device] %s: %s",
                    midiDeviceInfo.getId(), midiDeviceInfo.toString()));
            Log.d(TAG, String.format ("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
            int outputPort = -1 ;
            for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfo.getPorts()) {
                Log.d(TAG, String.format ("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                    outputPort = portInfo.getPortNumber() ;
                    Log.d(TAG, String.format ("[midi port] output port: %d", outputPort));
                    if (midiLastConnectedDevice == null && midiLastConnectedPort == -1) {
//                        found = true;
                        break;
                    }

                    String mName = midiDeviceInfo.getProperties().getString("name", midiDeviceInfo.toString()) ;
                    Log.i(TAG, "onCreate: [midi] compare: " + String.format("%s -> %s [%b]", midiLastConnectedDevice, mName, midiLastConnectedDevice.equals(mName)));
                    if (midiLastConnectedDevice != null && midiLastConnectedDevice.equals(mName)) {
                        if (midiLastConnectedPort == -1)
                            midiLastConnectedPort = 0;

                        outputPort = midiLastConnectedPort ;
                        found = true;
                        Log.i(TAG, "onCreate: [midi] found last connected device " + midiLastConnectedDevice);
                        midiConnect(midiDeviceInfo, outputPort);
                        break ;
                    }
                }

                if (found) {
                    Log.i(TAG, "onCreate: [midi] found: break");
                    break;
                }
            }

//            if (!found && midiDevice == null && midiDeviceInfo != null) {
            if (midiLastConnectedDevice == null) {
                midiConnect(midiDeviceInfo, outputPort);
            }

        }

        Log.d(TAG, "onCreate: " + String.format("" +
                "%d: %d", BuildConfig.VERSION_CODE, defaultSharedPreferences.getInt("currentVersion", 0)));
//        if (BuildConfig.VERSION_CODE > defaultSharedPreferences.getInt("currentVersion", 0)) {
        // only show onboard when first installed
        if (defaultSharedPreferences.getInt("currentVersion", 0) == 0) {
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

        String[] configVersion = BuildConfig.VERSION_NAME.split("-");

        if (configVersion.length > 1 && configVersion[1].equals("experimental")) {
            experimentalBuild = true ;
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
        pluginCreators = MainActivity.loadJSONFromAsset("creator.json");
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
                public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && list != null) {
                        for (Purchase purchase : list) {
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
            color = getResources().getColor(primaryColor);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }

        deviceWidth = getWindowManager().getDefaultDisplay().getWidth();
        deviceHeight = getWindowManager().getDefaultDisplay().getHeight();

        tabletMode = defaultSharedPreferences.getBoolean("tablet_mode", true);
        if (tabletMode && (float) (1.0f * deviceWidth / deviceHeight) > 0.7f)
            tabletMode = true ;
        else
            tabletMode = false ;

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

//        Log.d(TAG, String.format ("[orientation]: %s", getWindowManager().getDefaultDisplay().getRotation()));


        if (experimentalBuild)
            alert("Experimental beta version", "For testing only. Please report issues on Github or via the app's feedback bug reporting system.");

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            proVersion = true ;
        }

        register_bt_callback();
//        proVersion = false;
//        defaultSharedPreferences.edit().putBoolean("pro", false).apply();
    }

    void showMediaPlayerDialog() {
        if (lastRecordedFileName == null) {
            Log.e(TAG, "showMediaPlayerDialog: no last recorded audio");
            return;
        }

        if (mediaPlayerDialog == null)
            mediaPlayerDialog = new MediaPlayerDialog(this, mediaPlayer);
        mediaPlayerDialog.dialog.show();
        if (mediaPlayerDialog.dialog != null)
            return;
        Log.d(TAG, "showMediaPlayerDialog: " + lastRecordedFileName);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.media_player_dialog, null);
        SurfaceView surface = constraintLayout.findViewById(R.id.video_player_dialog);

        LinearLayout surfaceLayout = constraintLayout.findViewById(R.id.surface_ll);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (lastRecordedFileName.endsWith(".mp4")) {
                    int w = mp.getVideoWidth(), h = mp.getVideoHeight();
                    surfaceLayout.setVisibility(VISIBLE);
                    surface.getHolder().setFixedSize(w, h);
                    Log.d(TAG, "onPrepared: ends with mp4");
                } else {
                    surfaceLayout.setVisibility(GONE);
                }
            }
        });

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
                        if (lastRecordedFileName.endsWith(".mp4")) {
                            surfaceLayout.setVisibility(VISIBLE);
                            Log.d(TAG, "onCheckedChanged: ends with mp4");
                        } else {
                            surfaceLayout.setVisibility(GONE);
                            Log.d(TAG, "onCheckedChanged: no end");
                        }

                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(getApplicationContext(), uri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                        toast("Cannot load media file: " + e.getMessage());
                        return;
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        MainActivity.toast(e.getMessage());
                        return;
                    }

                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_pause_24);
                    mediaPlayer.start();
                } else {
                    mediaPlayer.pause();
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                    surface.setVisibility(GONE);
                }
            }
        });

        SeekBar seekBar = constraintLayout.findViewById(R.id.media_seekbar);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                seekBar.setProgress(0);
                surface.setVisibility(GONE);
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
            mainActivity.sampleRateLabel.setText(null);
            srLayout.setVisibility(GONE);
            mainActivity.latencyWarnLogo.setVisibility(GONE);
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

        nag ++ ;
        if (nag == 3 && ! proVersion) {
            startActivity(new Intent(this, com.shajikhan.ladspa.amprack.Purchase.class));
        }
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
            /* todo:
            1. Check if file is audio OR better plugin name
            2. If it is, do below
            3. If it is not, send filename to NAM plugin
            4. maybe a switch here, if NAM do this, if Looper do that

             */

            int plugin = requestCode - 5000 ;
            Uri returnUri = data.getData();

            if (returnUri != null) {
                String mimeType = getContentResolver().getType(returnUri);
                Log.d(TAG, String.format ("[mimetype]: %s", mimeType));
                if (!mimeType.startsWith("audio")) {
                    DocumentFile file = DocumentFile.fromSingleUri(mainActivity, returnUri);
                    Log.d(TAG, String.format ("ayyo filename: %s", file.getName()));
                    String path = file.getName();
//                    String path = returnUri.getPath();
                    if (path == null)
                        return;
                    String ext = path.substring(path.toString().lastIndexOf('.')+1) ;
                    Log.d(TAG, "onActivityResult: got mime type " + mimeType + ": " + path + " (" + ext + ")");
                    String dir = context.getExternalFilesDir(
                            Environment.DIRECTORY_DOWNLOADS) + "/" + AudioEngine.getActivePluginName(plugin);
                    DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
                    switch (ext.toLowerCase()) {
                        case "nam":
                        default: // aye
                            Log.d(TAG, String.format("setFileName: %s", path));
                            String s = getFileContent(returnUri);
//                            AudioEngine.setPluginFilename(s, plugin);
                            String basename = path ; //returnUri.getLastPathSegment();
                            basename = basename.substring(basename.lastIndexOf(":") + 1);
                            Log.d(TAG, String.format("[basename]: %s", basename));
                            String dest = dir + "/" + basename;
                            File fDir = new File (dir) ;
                            if (! fDir.exists())
                                if (!fDir.mkdirs())
                                    alert("Cannot create directory", "Error loading model: " + dir);

                            writeFile(dest, s);
                            int position = setSpinnerFromDir(holder.modelSpinner, dir, basename);
                            holder.modelSpinner.setSelection(position);
                            //                            ((DataAdapter.ViewHolder) mainActivity.recyclerView.findViewHolderForAdapterPosition(plugin)).modelSpinnerLayout.setVisibility(View.GONE);
                            toast("Loaded model successfully: " + basename);
                            return;
                        case "zip":
                            unzipNAMModel(dir, returnUri);
                            setSpinnerFromDir(holder.modelSpinner, dir, null);
                            return;
                    }
                }
            }

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

                Uri audioFile = data.getData();
                getContentResolver().takePersistableUriPermission(audioFile, Intent.FLAG_GRANT_READ_URI_PERMISSION) ;

                Log.d(TAG, String.format ("[audiofile]: %s", audioFile));
                float [] samples = audioDecoder.decode(audioFile, null, samplerate);
                if (samples == null)
                    return;
                AudioEngine.setPluginBuffer(samples, plugin);
                Log.d(TAG, String.format ("[decoder]: %d", samples.length));
                DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
                holder.audioFile = audioFile.toString();
            } catch (IOException e) {
                toast(e.getMessage());
                Log.e(TAG, "onActivityResult: ", e);
            }
        }

        if (resultCode == RESULT_OK && requestCode > 9999 && requestCode < 100000) {
            Uri returnUri = data.getData();
            int parse = requestCode - 10000 ;
            int plugin = parse / 100 ;
            int control = parse - (plugin * 100) ;
            DocumentFile file = DocumentFile.fromSingleUri(mainActivity, returnUri);
            Log.d(TAG, String.format ("ayyo filename: %s [%s]", file.getName(), file.getUri()));
            Log.d(TAG, String.format ("[load atom]: %d %d %d", requestCode, plugin, control));
            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(plugin);
            if (returnUri != null) {
                String dir = context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + AudioEngine.getActivePluginName(plugin);

//                String basename = file.getName() ; //returnUri.getLastPathSegment();
//                basename = basename.substring(basename.lastIndexOf(":") + 1);
//                Log.d(TAG, String.format("[basename]: %s", basename));
                String dest = dir + "/" + file.getName();
                File fDir = new File (dir) ;
                if (! fDir.exists()) {
                    if (!fDir.mkdirs()) {
                        alert("Cannot create directory", "Error loading model: " + dir);
                        return;
                    }
                }

                String path = file.getName();
                String ext = path.substring(path.toString().lastIndexOf('.')+1) ;

                Log.d(TAG, String.format ("extension: %s", ext));
                if (ext.equalsIgnoreCase("zip")) {
                    unzipNAMModel(dir, returnUri);
                    Spinner spinner = dataAdapter.holders.get(plugin).atomSpinners.get(control) ;
                    if (spinner == null) {
                        Log.d(TAG, "onActivityResult: spinner is null! why???");
                    } else
                        setSpinnerFromDir(dataAdapter.holders.get(plugin).atomSpinners.get(control), dir, null);
                    return;
                }

                try {
//                    copy (new File(file.getUri().getPath()), new File(dest));
                    copyFile(returnUri, Uri.parse("file://" + dest));
                } catch (IOException e) {
                    alert("Error loading file", e.getMessage());
                    Log.e(TAG, "onActivityResult: ", e);
                    return;
                }

                Log.d(TAG, String.format ("[copy file]: %s -> %s", returnUri.getPath(), dest));
                Log.d(TAG, String.format ("[load atom]: got filename %s", dest));
                int selection = setSpinnerFromDir(dataAdapter.holders.get(plugin).atomSpinners.get(control), dir, file.getName());
                dataAdapter.holders.get(plugin).atomSpinners.get(control).setSelection(selection);
                AudioEngine.setAtomPort(plugin, control, dest);
            }

            return;
        }

        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            Log.i(TAG, "onActivityResult: bluetooth device found");
            if (resultCode == Activity.RESULT_OK && data != null) {
                ScanResult scanResult = null ;
                if (data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) instanceof ScanResult) {
                    scanResult = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
                    deviceToPair = scanResult.getDevice();
                } else if (data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) instanceof BluetoothDevice) {
                    deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) ;
                } else {
                    Toast.makeText(context, "Unknown device type selected", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (deviceToPair != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        Log.i(TAG, "onActivityResult: no bluetooth permission");
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                PERMISSION_REQUEST_CODE_BLUETOOTH);
                        Toast.makeText(mainActivity, "Missing Bluetooth permission", Toast.LENGTH_SHORT).show();
                        return;
                    }

//                    deviceToPair.createBond();
//                    deviceToPair.connectGatt(mainActivity, true, new BluetoothGattCallback() {
//                        @Override
//                        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
//                            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
//                        }
//                    });
                    Log.i(TAG, "onActivityResult: bluetooth device connected");
                    // ... Continue interacting with the paired device.
                    defaultSharedPreferences.edit().putString("last_bt", deviceToPair.getAddress()).apply();
                    midiManager.openBluetoothDevice(deviceToPair, new MidiManager.OnDeviceOpenedListener() {
                        @Override
                        public void onDeviceOpened(MidiDevice device) {
                            midiDevice = device ;
                            midiLastConnectedDevice = device.getInfo().getProperties().getString("name", device.getInfo().toString());
                            MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
                            Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));
                            for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
                                Log.d(TAG, String.format("[midi device] %s: %s",
                                        midiDeviceInfo.getId(), midiDeviceInfo.toString()));
                                Log.d(TAG, String.format("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
                                int outputPort = -1;
                                for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfo.getPorts()) {
                                    Log.d(TAG, String.format("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                                        outputPort = portInfo.getPortNumber();
                                        Log.d(TAG, String.format("[midi port] output port: %d", outputPort));
                                        break;
                                    }
                                }

                                midiOutputPort = midiDevice.openOutputPort(outputPort);
                                midiOutputPort.connect(midiReciever);
                                defaultSharedPreferences.edit().putString("last_midi", midiLastConnectedDevice).commit();
                                defaultSharedPreferences.edit().putInt("last_midi_port", outputPort).commit();
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((findViewById(R.id.bt_icon))).setVisibility(VISIBLE);
                                        ((TextView)mainActivity.findViewById(R.id.midi_name)).setText(midiLastConnectedDevice);
                                    }
                                });
                            }
                        }
                    }, null);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
//            initCamera.start();
            mainActivity.rack.toggleVideo.setChecked(false);
//            mainActivity.rack.toggleVideo.setChecked(true);
        }

        if (PERMISSION_REQUEST_CODE_BLUETOOTH == requestCode &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                scanBLE();
            }
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

        Button clear = linearLayoutPluginDialog.findViewById(R.id.search_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText("");
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

        List <String> creators = new ArrayList<>();
        keys = pluginCreators.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Log.d(TAG, "pluginCreator: key " + key);
            creators.add(key);
        }

        ArrayAdapter<String> creatorDataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, creators);

        String [] sortBy = {
                "Category",
                "Creator"
        } ;

        ArrayAdapter<String> sortByAdaptor = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, sortBy);

        pluginDialogSortBy = (Spinner) linearLayoutPluginDialog.findViewById(R.id.sort_by);
        pluginDialogSortBy.setAdapter(sortByAdaptor);

        pluginDialogSortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        pluginDialogCategorySpinner.setAdapter(categoriesDataAdapter);
                        break;
                    case 1:
                        pluginDialogCategorySpinner.setAdapter(creatorDataAdapter);
                        break ;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Drop down layout style - list view with radio button
        sortByAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoriesDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        creatorDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pluginDialogCategorySpinner = (Spinner) linearLayoutPluginDialog.findViewById(R.id.plugin_types);
        // attaching data adapter to spinner
        pluginDialogCategorySpinner.setAdapter(categoriesDataAdapter);
        pluginDialogCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (view == null)
                    return;
                String category = ((TextView) view).getText().toString();
                Log.d(TAG, "onItemSelected: selected category " + category);
                pluginDialogAdapter.filterByCategory(category);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pluginDialogWallpaper = linearLayoutPluginDialog.findViewById(R.id.pl_wallpaper);
        Button closeButton = linearLayoutPluginDialog.findViewById(R.id.pl_close);

        if (! tabletMode)
            builder.setView(linearLayoutPluginDialog);
        else {
            mainActivity.rack.pane_2.addView(linearLayoutPluginDialog);
            closeButton.setVisibility(GONE);
        }

        AlertDialog pluginDialog = builder.create();
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
            // query the plugin and replace the following with
            // if plugin is LADSPA
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
        AudioEngine.setMainActivityClassName("com/shajikhan/ladspa/amprack/MainActivity");

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
        JSONObject midiJSON = midiPluginControlsAsJSON();

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

            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            int spinnerValue = -1 ;
            String audioFile = null ;
            if (holder == null) {
                Log.w(TAG, "presetToString: holder is null" );
            } else {
                audioFile = holder.audioFile;
                if (holder.hasFileSpinner) {
                    spinnerValue = holder.modelSpinner.getSelectedItemPosition();
                } else {
                    Log.d(TAG, "presetToString: preset has holder, but no file spinner!");
                }
            }

            try {
                jo.put("name", AudioEngine.getActivePluginName(i));
                if (midiJSON.has(String.valueOf(i))) {
                    jo.put("midi", midiJSON.getJSONArray(String.valueOf(i)));
                }

                jo.put("controls", vals);
                if (spinnerValue > -1)
                    jo.put("selectedModel", spinnerValue);
                if (audioFile != null)
                    jo.put("audioFile", audioFile);
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

            DataAdapter.ViewHolder holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            int spinnerValue = -1 ;
            String audioFile = null ;

            if (holder == null) {
                Log.w(TAG, "presetToString: holder is null" );
            } else {
                audioFile = holder.audioFile;
                if (holder.hasFileSpinner) {
                    spinnerValue = holder.modelSpinner.getSelectedItemPosition();
                } else {
                    Log.d(TAG, "presetToString: preset has holder, but no file spinner!");
                }
            }

            float[] values = AudioEngine.getActivePluginValues(i);
            for (int k = 0; k < values.length; k++) {
                vals += values[k];
                if (k < values.length - 1) {
                    vals += ";";
                }
            }

            jo.put("name", AudioEngine.getActivePluginName(i));
            jo.put("controls", vals);
            jo.put("midi", mainActivity.midiControlsForDB(i));
            if (spinnerValue > -1)
                jo.put("selectedModel", String.valueOf(spinnerValue));
            if (audioFile != null)
                jo.put ("audioFile", audioFile);

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
        saveGlobalMidi();
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
        
        loadGlobalMidi();
        
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
        resetPluginMIDI();

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
            int spinnerValue = -1 ;
            String audioFile = null ;
            try {
                name = jo.getString("name");
                controls = jo.getString("controls");

                if (jo.has("midi") && ! jo.getString("midi").isEmpty()) {
                    JSONArray jsonArray ;
                    String _s = jo.getString("midi");
                    if (! _s.contains(";"))
                        jsonArray = jo.getJSONArray("midi");
                    else {
                        jsonArray = new JSONArray();
                        String [] v = _s.split("\\|");
                        for (String s : v) {
                            if (s.isEmpty())
                                continue;

                            String[] vv = s.split(";");
                            JSONObject _j = new JSONObject();
                            _j.put("plugin", vv[0]);
                            _j.put("pluginControl", vv[1]);
                            _j.put("channel", vv[2]);
                            _j.put("control", vv[3]);
                            jsonArray.put(_j);
                        }

                        Log.i(TAG, "loadPreset: firestore midi controls for " + plugin + " " + jsonArray.toString());
                    }

                    for (int d = 0 ; d < jsonArray.length(); d ++) {
                        JSONObject jControl = jsonArray.getJSONObject(d);
                        MIDIControl midiControl = new MIDIControl();
                        midiControl.plugin = plugin ;
                        midiControl.pluginControl = jControl.getInt("pluginControl");
                        midiControl.scope = MIDIControl.Scope.PLUGIN ;
                        midiControl.channel = jControl.getInt("channel");
                        midiControl.control = jControl.getInt("control");
                        midiControls.add(midiControl);
                        Log.i(TAG, "loadPreset: added midi control " + midiControl.get());
                    }
                }

                if (jo.has("selectedModel"))
                    spinnerValue = jo.getInt("selectedModel");
                if (jo.has("audioFile"))
                    audioFile = jo.getString("audioFile");
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
            int pos = dataAdapter.getItemCount();

            if (spinnerValue > -1 )
                dataAdapter.setSelectedModel(plugin, spinnerValue);
            if (audioFile != null)
                dataAdapter.setAudioFiles(plugin, audioFile);

            plugin++;

//            holder = (DataAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(dataAdapter.getItemCount() - 1);
//            if (holder.hasFileSpinner) {
//                holder.modelSpinner.setSelection(spinnerValue);
//            }
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
//        if (proVersion == false) {
//            AudioEngine.setExportFormat(0);
//        }
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

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
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
        Log.d(TAG, String.format ("[orientation]: %d", mainActivity.getResources().getConfiguration().orientation));

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
            mainActivity.handler.post(() -> {
                        if (inputValue > 1f)
                            inputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        else
                            inputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                        inputMeter.setProgress((int) (inputValue * 100));
                    });
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
            mainActivity.handler.post(() -> {
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
                            mainActivity.lastRecordedFileName = mainActivity.dir.getAbsolutePath() + "/" + mainActivity.lastRecordedFileName;
                            AudioEngine.setFileName(mainActivity.lastRecordedFileName);
                            switch (mainActivity.exportFormat) {
                                case "0":
                                default:
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".wav";
                                    break;
                                case "1":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".ogg";
                                    break;
                                case "2":
                                    mainActivity.lastRecordedFileName = mainActivity.lastRecordedFileName + ".mp3";
                                    break;
                            }

                            mainActivity.triggerRecordedSomething = true;
                            mainActivity.recording = true;
                            Log.d(TAG, "setMixerMeterSwitch: triggering recording");
                            AudioEngine.toggleRecording(true);
//                        mainActivity.triggerRecordToggle.setChecked(false);
                        }
                    }
                } else {
                    if (inputValue > 1f)
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    else
                        outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                }
            });

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

        try {
            avBuffer.put(buffer);
        } catch (InterruptedException e) {
            Log.e(TAG, "pushToVideo: error adding avbuffer to queue", e);
            throw new RuntimeException(e);
        }
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

    public static double rootMeanSquare(float[] nums) {
        double sum = 0.0;
        for (double num : nums)
            sum += num * num;
        return Math.sqrt(sum / nums.length);
    }

    public static void cameraTest () {
        SensorManager sensorManager;
        final float[] rotationMatrix = new float[9];
        float[] accelerometerReading = new float[9];
        float[] magnetometerReading = new float[9];
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        final float[] orientationAngles = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        Log.d(TAG, String.format ("[orientation]: %f %f %f",
                orientationAngles[0],
                orientationAngles[1],
                orientationAngles[2]
                ));
    }

    public static void setSampleRateDisplay (int sampleRateDisplay, boolean lowLatency) {
        Log.d(TAG, String.format ("[audio]: %d (%b)", sampleRateDisplay, lowLatency));
//        double inputLatency = AudioEngine.getLatency(true), outputLatency = AudioEngine.getLatency(false) ;

        mainActivity.handler.post(() -> {
            srLayout.setVisibility(VISIBLE);
            mainActivity.sampleRateLabel.setText(String.format("%dkHz", sampleRateDisplay));
            if (lowLatency)
                mainActivity.latencyWarnLogo.setVisibility(GONE);
            else
                mainActivity.latencyWarnLogo.setVisibility(VISIBLE);

            while (engineStartListeners.size() > 0) {
                OnEngineStartListener onEngineStartListener = engineStartListeners.poll();
                if (onEngineStartListener != null)
                    onEngineStartListener.run();
            }
        });
    }

    public static void lowLatencyDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Latency Warning")
                .setMessage(R.string.low_latency_warning)
                .setNegativeButton("Close", null)
                .setIcon(R.drawable.baseline_warning_24);
        builder.create().show();
    }

    /*  I think this is pretty amazing.
        Am I OOP yet?
     */
    public static abstract class OnEngineStartListener {
        abstract void run () ;

        OnEngineStartListener () {
            engineStartListeners.add(this);
        }
    }

    public String getFileContent(Uri uri) {
        InputStreamReader inputStreamReader = null;
        try {
            if (!uri.toString().startsWith("/"))
                inputStreamReader = new InputStreamReader(getContentResolver().openInputStream(uri));
            else {
                inputStreamReader = new InputStreamReader(new FileInputStream(new File(String.valueOf(uri))));
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, "getFileContent: ", e);
//            throw new RuntimeException(e);
            return null;
        }

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder sb = new StringBuilder();
        String s;
        while (true) {
            try {
                if (!((s = bufferedReader.readLine()) != null)) break;
            } catch (IOException e) {
                Log.e(TAG, "getFileContent: ", e);
                throw new RuntimeException(e);
            }

            sb.append(s);
        }
        String fileContent = sb.toString();
        return fileContent;
    }

    public void unzipNAMModel (String dir, Uri uri) {
        InputStream inputStream = null;
        String basename = null ;
        try {
            inputStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            MainActivity.alert("Cannot load model", e.getMessage());
            Log.e(TAG, "onActivityResult: ", e);
        }
        try {
            basename = SkinEngine.unzip(inputStream, dir);
        } catch (IOException e) {
            MainActivity.alert("Cannot unzip model", e.getMessage());
            Log.e(TAG, "onActivityResult: ", e);
        }

        toast("Successfully extracted Model " + basename);
    }

    public void manageNAMModels (Spinner spinner, String dir) {
        DocumentFile root = DocumentFile.fromFile(new File(dir));
        DocumentFile [] files = root.listFiles() ;
        ArrayList <String> models = new ArrayList<>();
        for (DocumentFile file: files) {
            Log.d(TAG, String.format ("%s: %s", file.getName(), file.getUri()));
            models.add(file.getName());
        }

        ArrayList <Integer> selectedItems = new ArrayList();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title.
        builder.setTitle("Manage NAM Models")
                // Specify the list array, the items to be selected by default (null for
                // none), and the listener through which to receive callbacks when items
                // are selected.
                .setMultiChoiceItems(models.toArray(new CharSequence[models.size()]), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checks the item, add it to the selected
                                    // items.
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // If the item is already in the array, remove it.
                                    selectedItems.remove(which);
                                }
                            }
                        });

        builder.setNegativeButton("Cancel", null)
                .setPositiveButton("Delete Selected", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mainActivity);
                        builder1.setTitle("Delete selected models?");
                        builder1.setMessage("This step cannot be undone.");
                        builder1.setPositiveButton("Delete selected", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
//                                Log.d(TAG, String.format ("delete: %s", selectedItems.toString()));
                                for (int s: selectedItems) {
                                    new File(dir +"/"+ models.get(s)).delete();
                                }

                                setSpinnerFromDir(spinner, dir, null);
                            }
                        });

                        builder1.setNegativeButton("Cancel", null);
                        builder1.show();
                    }
                }).setNeutralButton("Delete All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mainActivity);
                        builder1.setTitle("Delete all models?");
                        builder1.setMessage("This step cannot be undone.");
                        builder1.setPositiveButton("Delete all", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
//                                Log.d(TAG, String.format ("delete: %s", selectedItems.toString()));
                                for (String s: models) {
                                    new File(dir + "/" + s).delete();
                                }
                                setSpinnerFromDir(spinner, dir, null);
                            }
                        });

                        builder1.setNegativeButton("Cancel", null);
                        builder1.show();
                    }
                });
        builder.show();
    }

    public static int setSpinnerFromDir (Spinner spinner, String dir, String toSelect) {
        if (spinner == null) {
            return 0 ;
        }

        int selection = 0 ;
        ArrayList <String> models = new ArrayList<>();
        DocumentFile root = DocumentFile.fromFile(new File(dir));
        DocumentFile [] files = root.listFiles() ;
        int counter = 0 ;
        for (DocumentFile file: files) {
            Log.d(TAG, String.format ("%s: %s", file.getName(), file.getUri()));
            models.add(file.getName());
            if (toSelect != null && file.getName().equals(toSelect))
                selection = counter ;
            counter ++ ;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return selection;
    }

    public static void writeFile (String path, String text) {
        try {
            File file = new File(path);

            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "writeFile: ", e);
            toast(e.getMessage());
        }
    }

    public static void getLatency () {
        if (!mainActivity.running) {
            Toast.makeText(context, "not running", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "getLatency: not running");
            return;
        }
        
        Toast.makeText(context,
                String.format("%.0fms/%.0fms | %d/%d",
                        AudioEngine.getLatency(true),
                        AudioEngine.getLatency(false),
                        AudioEngine.getBufferSizeInFrames(true),
                        AudioEngine.getBufferSizeInFrames(false)
                )
                , Toast.LENGTH_LONG).show();
    }

    public void addFavoritePreset (Map preset) {
        JSONObject jsonObject = new JSONObject(preset);
        File f = new File(new StringJoiner ("/").add(favPresetsDir).add (preset.get("name").toString()).toString());
        if (! f.exists())
            writeFile(f.getPath(), jsonObject.toString());
    }

    public void removeFavoritePreset (Map preset) {
        File f = new File(new StringJoiner ("/").add(favPresetsDir).add (preset.get("name").toString()).toString());
        if (! f.delete()) {
            Toast.makeText(context, "Cannot remove preset " + preset.get("name"), Toast.LENGTH_SHORT).show();
        }
    }

    public static Map JSONtoMap (JSONObject j) {
        try {
            Log.d(TAG, String.format ("json: %s", j.toString(4)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

//        Log.d(TAG, String.format ("%s: %s"));
        HashMap <String,Object> map = new HashMap();
        try {
            String [] params = {
                    "name",
                    "uid",
                    "desc",
                    "path",
                    "public",
                    "timestamp",
                    "likes"
            } ;

            for (String s: params)
                if (j.has(s))
                    map.put(s, j.get(s));

            HashMap <String, HashMap> hashMap = new HashMap();
            map.put("controls", hashMap);

            // todo: do each control individually
            JSONObject controls = j.getJSONObject ("controls");
            Iterator<String> keys = controls.keys();
            if (! keys.hasNext()) {
                Log.e(TAG, "JSONtoMap: no controls found! error! error!");
                return map;
            }
            String next = keys.next();
            while (keys.hasNext()) {
                JSONObject settings = controls.getJSONObject(next);
                HashMap <String, Object> set = new HashMap<>();

                set.put("name", settings.get("name"));
                set.put ("controls", ((String) settings.get("controls")).split (";"));
                hashMap.put(next, set);

                next = keys.next();
            }

        } catch (JSONException e) {
            Toast.makeText(mainActivity, "Cannot load preset " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "JSONtoMap: ", e);
            return null ;
        }

        return map;
    }

    public static void copy(File src, File dst) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try (InputStream in = Files.newInputStream(src.toPath())) {
                try (OutputStream out = Files.newOutputStream(dst.toPath())) {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }

    private void copyFile(Uri pathFrom, Uri pathTo) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(pathFrom)) {
            if(in == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(pathTo)) {
                if(out == null) return;
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    void setMidiControl (View view, int plugin, int control, MIDIControl.Type type, MIDIControl.Scope scope) {
        Log.i(TAG, String.format(
                "[set midi control] plugin: %d, control: %d",
                plugin, control
        ));

        String channel = "";
        String data1 = "";

        MIDIControl old = null;
        
        for (MIDIControl midiControl: midiControls) {
            Log.d(TAG, String.format ("[midi plugin check] %d: %d",
                    midiControl.view.getId(), view.getId()
                    ));

            if (midiControl.view.getId() == view.getId()) {
                old = midiControl ;
                data1 = String.valueOf(midiControl.control);
                channel = String.valueOf(midiControl.channel);
                break ;
            }
        }

        MIDIControl midiControl = new MIDIControl() ;
        midiControl.plugin = plugin ;
        midiControl.scope = scope ;
        midiControl.type = type;
//        if (scope == MIDIControl.Scope.GLOBAL) {
//            midiControl.type = MIDIControl.Type.TOGGLE;
//        }
//        midiControl.control = control ;
        midiControl.pluginControl = control ;
        midiControl.view = view ;

        midiLayout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.midi_add_control, null);
        ((EditText) midiLayout.findViewById(R.id.channel)).setText(channel);
        ((EditText) midiLayout.findViewById(R.id.control)).setText(data1);

        triggerMidiButton = midiLayout.findViewById(R.id.midi_trigger);

        channelEdit = midiLayout.findViewById(R.id.channel) ;
        controlEdit = midiLayout.findViewById(R.id.control) ;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(midiLayout);
        if (scope == MIDIControl.Scope.PLUGIN)
            builder.setTitle("Set MIDI Control for " + AudioEngine.getActivePluginName(plugin) + " " + AudioEngine.getControlName(plugin, control));
        else
            builder.setTitle("Set MIDI Control");

        MIDIControl finalOld = old;
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (finalOld != null)
                    midiControls.remove(finalOld);
                String channel = ((EditText) midiLayout.findViewById(R.id.channel)).getText().toString();
                String program = ((EditText) midiLayout.findViewById(R.id.control)).getText().toString();

                int ch = -1 ;
                int pr =  -1 ;

                try {
                    pr = Integer.parseInt(program);
                    ch = Integer.parseInt(channel);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "onDismiss: ", e);
                    alert("Failed to set MIDI control", e.getMessage());
                    return;
                }

                midiControl.channel = ch ;
                midiControl.control = pr ;
                midiControls.add(midiControl);

                Toast.makeText(MainActivity.this, "MIDI control set successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "[midi controls]: " + midiControls.toString());
                if (scope == MIDIControl.Scope.GLOBAL) {
                    saveGlobalMidi();
                } else {
                    saveActivePreset();
                }
            }
        }) ;

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create() ;

        midiAddDialog = dialog ;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.i(TAG, "onDismiss: midi add dialog set to null");
                midiAddDialog = null ;
            }
        });

        dialog.show();
    }

    void processMIDIMessage (int channel, int data1, int data2) {
//        Log.d(TAG, "processMIDIMessage: process message " +
//                String.format("%d %d %d", channel, data1, data2));
        String msg = String.format(
                "%d %d %d",
                channel, data1, data2
        );

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                midiDisplay.setText(msg);

                if (midiAddDialog != null && triggerMidiButton != null && triggerMidiButton.isChecked()) {
                    EditText ed1 = ((EditText) midiAddDialog.findViewById(R.id.channel));
                    if (ed1 != null)
                        ed1.setText(String.valueOf(channel));
                    EditText ed2 = ((EditText) midiAddDialog.findViewById(R.id.control));
                    if (ed2 != null)
                        ed2.setText(String.valueOf(data1));
                }
            }
        });

        for (MIDIControl midiControl: midiControls) {
//            Log.i(TAG, "processMIDIMessage: probe midiControl " + midiControl);

            if (midiControl.channel == channel && data1 == midiControl.control) {
                midiControl.process(data2);
                break ;
            }
        }
    }

    void loadGlobalMidi () {
        Log.i(TAG, "loadGlobalMidi: loading settings");
        String s = defaultSharedPreferences.getString("global_midi", null);
        if (s == null) {
            Log.i(TAG, "loadGlobalMidi: no saved settings");
            return;
        } else {
            Log.i(TAG, "loadGlobalMidi: saved settings found " + s);
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(s);
            for (int i = 0 ; i < jsonArray.length(); i ++) {
                JSONObject j = jsonArray.getJSONObject(i);
                MIDIControl midiControl = new MIDIControl();
                midiControl.scope = MIDIControl.Scope.GLOBAL;
                String idString = j.getString("view");
                if (idString.isEmpty()) {
                    Log.w(TAG, "loadGlobalMidi: invalid settings, skipping " + j);
                    continue;
                }
                midiControl.view = findViewById(stringToId(idString));
                switch (j.getString ("type")) {
                    case "KNOB":
                        midiControl.type = MIDIControl.Type.KNOB ;
                        break ;
                    case "SLIDER":
                        midiControl.type = MIDIControl.Type.SLIDER ;
                        break ;
                    case "TOGGLE":
                    default:
                        midiControl.type = MIDIControl.Type.TOGGLE ;
                        break ;
                }

                midiControl.scope = MIDIControl.Scope.GLOBAL;
                midiControl.channel = j.getInt("channel");
                midiControl.control = j.getInt("control");
                midiControls.add(midiControl);
                Log.i(TAG, "loadGlobalMidi: added control " + midiControl);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    void saveGlobalMidi () {
        JSONArray jsonArray = new JSONArray();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.PLUGIN || midiControl.view == null)
                continue;

            try {
                JSONObject jsonObject = midiControl.get() ;
                Log.i(TAG, "saveGlobalMidi: " + jsonObject.toString());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(TAG, "saveGlobalMidi: " + jsonArray.toString());
        defaultSharedPreferences.edit().putString("global_midi", jsonArray.toString()).apply();
    }

    int stringToId (String s) {
        return getResources().getIdentifier(s, "id", getPackageName());
    }

    JSONObject midiPluginControlsAsJSON () {
        JSONObject jsonObject = new JSONObject();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL)
                continue;

            try {
                if (! jsonObject.has(String.valueOf(midiControl.plugin)))
                    jsonObject.put(String.valueOf(midiControl.plugin), new JSONArray());
                jsonObject.getJSONArray(String.valueOf(midiControl.plugin)).put(midiControl.get());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(TAG, "midiPluginControlsAsJSON: " + jsonObject);
        return jsonObject;
    }

    JSONObject midiPluginControlsAsJSONObject () {
        JSONObject jsonObject = new JSONObject();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL)
                continue;

            try {
                if (! jsonObject.has(String.valueOf(midiControl.plugin)))
                    jsonObject.put(String.valueOf(midiControl.plugin), new JSONObject());
                if (! (jsonObject.getJSONObject(String.valueOf(midiControl.plugin)).has (String.valueOf(midiControl.pluginControl))))
                    (jsonObject.getJSONObject(String.valueOf(midiControl.plugin))).put(String.valueOf(midiControl.pluginControl), midiControl.get ());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(TAG, "midiPluginControlsAsJSONObject: " + jsonObject);
        return jsonObject;
    }

    void resetPluginMIDI () {
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL)
                continue;

            midiControls.remove(midiControl);
        }
    }

    public static void resetMIDI () {
        mainActivity.midiControls.clear();
    }

    public static void printMidi () {
        Log.i(TAG, "printMidi: " + mainActivity.midiControls);
    }

    String midiControlsForDB (int plugin) {
        StringBuilder s = new StringBuilder();
        for (MIDIControl midiControl: midiControls) {
            if (midiControl.scope == MIDIControl.Scope.GLOBAL|| midiControl.plugin != plugin)
                continue;

            s.append(midiControl.getForDB()).append("|");
        }

        return s.toString();
    }

    /* BLE MIDI Support */

    private static final int SELECT_DEVICE_REQUEST_CODE = 100000;

    Executor executor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    void scanBLE () {
        if (Build.VERSION.SDK_INT < 33) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth MIDI only supported on Android 13 and above.");
            builder.setMessage("To use a Bluetooth MIDI Controller with Amp Rack, install MIDI BLE Connect from the Play store, connect to your BLE MIDI Controller and then restart Amp Rack");
            builder.setPositiveButton("Install MIDI BLE Connect", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String url = "https://play.google.com/store/apps/details?id=com.mobileer.example.midibtlepairing";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });

            builder.show();
            return;
        }

        deviceManager =
                (CompanionDeviceManager) getSystemService(
                        Context.COMPANION_DEVICE_SERVICE
                );

        // To skip filtering based on name and supported feature flags,
        // do not include calls to setNamePattern() and addServiceUuid(),
        // respectively. This example uses Bluetooth.
        BluetoothDeviceFilter deviceFilter =
                new BluetoothDeviceFilter.Builder()
                        .setNamePattern(Pattern.compile("My device"))
                        .addServiceUuid(
                                new ParcelUuid(new UUID(0x123abcL, -1L)), null
                        )
                        .build();

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of device names is presented to the user as
        // pairing options.
        AssociationRequest pairingRequest = new AssociationRequest.Builder()
//                .addDeviceFilter(deviceFilter)
//                .setSingleDevice(true)
                .build();

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest, executor, new CompanionDeviceManager.Callback() {
            // Called when a device is found. Launch the IntentSender so the user can
            // select the device they want to pair with.
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                try {
                    startIntentSenderForResult(
                            chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                    );
                } catch (IntentSender.SendIntentException e) {
                    Log.e("MainActivity", "Failed to send intent");
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            public void onAssociationCreated(AssociationInfo associationInfo) {
                // AssociationInfo object is created and get association id and the
                // macAddress.
                int associationId = associationInfo.getId();
                MacAddress macAddress = associationInfo.getDeviceMacAddress();
            }

            @Override
            public void onFailure(CharSequence errorMessage) {
                // Handle the failure.
            }
        });
    }

    void register_bt_callback () {
        Log.i(TAG, "register_bt_callback: init");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BOND_STATE_CHANGED);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "[bt receiver] onReceive: " + intent.getIntExtra(EXTRA_BOND_STATE, -1));
                //do something based on the intent's action
                if (intent.getIntExtra(EXTRA_BOND_STATE, -1) == 11) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            View icon = mainActivity.findViewById(R.id.bt_icon) ;
                            if (icon != null)
                                icon.setVisibility(VISIBLE);
                        }
                    });
                    Toast.makeText(MainActivity.this, "Bluetooth device connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((findViewById(R.id.bt_icon))).setVisibility(GONE);
                        }
                    });
                }

                Log.i(TAG, "onReceive: bluetooth device connected? " + intent.getIntExtra(EXTRA_BOND_STATE, -1) + ": " + intent.getParcelableExtra(EXTRA_DEVICE));
            }
        };

        registerReceiver(receiver, filter);
    }

    public static void pluginsCrashTest () {
        Handler handler = new Handler();
        ProgressDialog dialog = ProgressDialog.show((Context) mainActivity, "Plugin Crash Test", "Testing ...", false, false, null);
        dialog.setIndeterminate(false);

        dialog.setMax(mainActivity.pluginDialogAdapter.pluginNames.size());
        Runnable runnable = new Runnable() {
            int p = 195;
            Runnable r = this;

            public void run() {
                Log.i(TAG, "plugin crash test: " + String.format("plugin %s [%d of %d]", mainActivity.pluginDialogAdapter.pluginNames.get(p), p, mainActivity.pluginDialogAdapter.pluginNames.size()));
                AudioEngine.clearActiveQueue();
                mainActivity.dataAdapter.reset();

                dialog.setProgress(p);
                dialog.setMessage(p + "/" + totalPlugins + ": " + mainActivity.pluginDialogAdapter.pluginNames.get(p));
                Log.i("plugin crash test", p + "/" + totalPlugins + ": " + mainActivity.pluginDialogAdapter.pluginNames.get(p));
                mainActivity.addPluginByName(mainActivity.pluginDialogAdapter.pluginNames.get(p));
                p++;

                if (p >= totalPlugins) {
                    alert("Test complete", "Successfully, probably, since we didn't crash ...");
                    return;
                }

                handler.postDelayed(r, 500);
            }
        } ;

        handler.postDelayed(runnable, 2000);
    }

    void selectMidiDevice () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.midi_select, null);
        builder.setView(linearLayout);

        MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
        Log.d(TAG, String.format ("[midi] found devices: %d", midiDeviceInfos.length));

        ArrayList <String> devices = new ArrayList<>();
        for (MidiDeviceInfo midiDeviceInfo: midiDeviceInfos) {
            Log.d(TAG, String.format("[midi device] %s: %s",
                    midiDeviceInfo.getId(), midiDeviceInfo.toString()));
            devices.add(midiDeviceInfo.getProperties().getString("name", midiDeviceInfo.toString()));
            Log.d(TAG, String.format("%d %d: %s", midiDeviceInfo.getInputPortCount(), midiDeviceInfo.getOutputPortCount(), midiDeviceInfo.getPorts().toString()));
        }

        Spinner dSpinner = linearLayout.findViewById(R.id.select_midi_device);
        Spinner pSpinner = linearLayout.findViewById(R.id.select_midi_port);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, devices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dSpinner.setAdapter(adapter);

        int x = 0;
        if (midiLastConnectedDevice != null) {
            for (String d : devices) {
                if (d.equals(midiLastConnectedDevice)) {
                    dSpinner.setSelection(x);
                    break;
                }

                x++;
            }
        }

        dSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> ports = new ArrayList<>();
                for (MidiDeviceInfo.PortInfo portInfo : midiDeviceInfos[position].getPorts()) {
                    Log.d(TAG, String.format ("[midi port] %s: %d [%d]", portInfo.getName(), portInfo.getPortNumber(), portInfo.getType()));
                    if (portInfo.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                        int outputPort = portInfo.getPortNumber();
                        Log.d(TAG, String.format ("[midi port] output port: %d", outputPort));
                        ports.add(String.valueOf(outputPort));
                        break ;
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, ports);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pSpinner.setAdapter(adapter);

                if (midiLastConnectedPort != -1) {
                    int x = 0 ;
                    for (MidiDeviceInfo.PortInfo p: midiDeviceInfos [position].getPorts()) {
                        if (midiLastConnectedPort == p.getPortNumber()) {
                            pSpinner.setSelection(x);
                            break;
                        }

                        x++;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        builder.setTitle("Select MIDI Device / Port");
        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int btn) {
                if (midiDevice != null) {
                    try {
                        midiDevice.close();
                    } catch (IOException e) {
                        Log.e(TAG, "onClick: ", e);
                    }
                }

                int which = dSpinner.getSelectedItemPosition();

                Log.i(TAG, "onClick: select device " + which );
                if (which == -1) {
                    Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                midiManager.openDevice(midiDeviceInfos [which], device -> {
                    midiDevice = device ;
                    Log.d(TAG, String.format ("[midi] device opened: opening port..."));

                    midiOutputPort = device.openOutputPort(pSpinner.getSelectedItemPosition());
                    midiLastConnectedDevice = midiDeviceInfos [which].getProperties().getString("name", midiDeviceInfos [which].toString());
                    midiLastConnectedPort = pSpinner.getSelectedItemPosition() ;
                    Log.i(TAG, "onClick: [midi] save midi device " + midiLastConnectedDevice + " " + midiLastConnectedPort);
                    defaultSharedPreferences.edit().putString("last_midi", midiLastConnectedDevice).commit();
                    defaultSharedPreferences.edit().putInt("last_midi_port", midiLastConnectedPort).commit();

                    Log.d(TAG, String.format ("[midi] port opened: port %d", midiOutputPort.getPortNumber()));
                    midiOutputPort.connect(midiReciever);
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "[bt] run: connected device " + midiDevice.getInfo().getType());
                            ((TextView) findViewById(R.id.midi_name)).setText(midiDeviceInfos [which].getProperties().getString("name", ""));
                            if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_USB || midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
                                (findViewById(R.id.midi_icon)).setVisibility(VISIBLE);
                                (findViewById(R.id.midi_icon)).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        selectMidiDevice();
                                    }
                                });
                            }
                            else if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                                (findViewById(R.id.bt_icon)).setVisibility(VISIBLE);
                                (findViewById(R.id.bt_icon)).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        detachBLE();
                                    }
                                });
                            }
                        }
                    });
                }, null);

                Toast.makeText(MainActivity.this, "MIDI Device connected", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }
    
    void detachBLE () {
        Log.i(TAG, "detachBLE: ");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Disconnect and forget Bluetooth device?");
        builder.setPositiveButton("Disconnect and forget", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                defaultSharedPreferences.edit ().remove("last_bt").commit();
                if (deviceToPair != null) {
                    deviceToPair = null ;
                    if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                        try {
                            midiDevice.close();
                            ((TextView) findViewById(R.id.midi_name)).setText("");
                        } catch (IOException e) {
                            Log.e(TAG, "onClick: ", e);
                        }
                    }
                }

                Toast.makeText(mainActivity, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                (findViewById(R.id.bt_icon)).setVisibility(GONE);
            }
        });

        builder.show();
    }

    void midiConnect (MidiDeviceInfo midiDeviceInfo, int outputPort) {
        int finalOutputPort = outputPort;
        Log.d(TAG, String.format ("[midi] opening device: %s", midiDeviceInfo.toString()));
        try {
            midiManager.openDevice(midiDeviceInfo, device -> {
                midiDevice = device;
                Log.d(TAG, String.format("[midi] device opened: opening port..."));
                midiOutputPort = device.openOutputPort(finalOutputPort);
                Log.d(TAG, String.format("[midi] port opened: port %d", midiOutputPort.getPortNumber()));
                midiOutputPort.connect(midiReciever);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (midiDevice == null)
                            return;

                        Log.i(TAG, "[bt] run: connected device " + midiDevice.getInfo().getType() + " >" + midiDeviceInfo.getProperties().getString("name", ""));
                        ((TextView) findViewById(R.id.midi_name)).setText(midiDeviceInfo.getProperties().getString("name", ""));
                        if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_USB || midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
                            (findViewById(R.id.midi_icon)).setVisibility(VISIBLE);
                            (findViewById(R.id.midi_icon)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    selectMidiDevice();
                                }
                            });
                        } else if (midiDevice.getInfo().getType() == MidiDeviceInfo.TYPE_BLUETOOTH) {
                            (findViewById(R.id.bt_icon)).setVisibility(VISIBLE);
                            (findViewById(R.id.bt_icon)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    detachBLE();
                                }
                            });
                        }
                    }
                });
            }, null);
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
//                    Toast.makeText(mainActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void midiMappingsDialog () {
        ArrayList <Integer> selectedItems = new ArrayList();  // Where we track the selected items
        ArrayList <String> mappings = new ArrayList<>();
        for (MIDIControl control: midiControls) {
            if (control.plugin != -1) {
                mappings.add(String.format(
                        "[%d %d] %s %s",
                        control.channel, control.control,
                        AudioEngine.getActivePluginName(control.plugin),
                        AudioEngine.getControlName(control.plugin, control.pluginControl)
                ));
            } else {
                mappings.add(String.format(
                        "[%d %d] %s",
                        control.channel, control.control,
                        control.getID()
                ));
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title.
        builder.setTitle("MIDI Mappings")
                // Specify the list array, the items to be selected by default (null for
                // none), and the listener through which to receive callbacks when items
                // are selected.
                .setMultiChoiceItems((CharSequence[]) mappings.toArray(new CharSequence [mappings.size()]), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checks the item, add it to the selected
                                    // items.
                                    selectedItems.add(which);
                                } else if (selectedItems.contains(which)) {
                                    // If the item is already in the array, remove it.
                                    selectedItems.remove(which);
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User taps OK, so save the selectedItems results
                        // somewhere or return them to the component that opens the
                        // dialog.
                        for (int i: selectedItems) {
                            Log.i(TAG, "onClick: [midi control] remove " + midiControls.get(i));
                            midiControls.remove(i);
                            Toast.makeText(MainActivity.this, "Selected mappings removed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton("Close", null)
                .setNegativeButton("Remove all", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        midiControls.clear();
                        Toast.makeText(MainActivity.this, "MIDI Mappings cleared", Toast.LENGTH_SHORT).show();
                    }
                });

        builder.show();
    }
}


