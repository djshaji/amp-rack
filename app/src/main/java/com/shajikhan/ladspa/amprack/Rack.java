package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Rack extends Fragment {
    MainActivity mainActivity ;
    LinearProgressIndicator quickPatchProgress ;
    String TAG = getClass().getSimpleName();
    PopupMenu optionsMenu ;
    ToggleButton toggleVideo, videoRecord ;
    LinearLayout videoPreview ;
    TextureView videoTexture ;
    LinearLayout mixer ;
    Button patchUp, patchDown ;
    JSONObject jsonObject = new JSONObject();
    ToggleButton swapCamera ;
    LinearLayout rackMaster, pane_2 ;
    boolean mixerInit = false ;
    boolean autoHideMixer = true ;
    /*
    Rack () {
        mainActivity = (MainActivity) getActivity();
    }

    Rack (MainActivity activity) {
        mainActivity = activity ;
    }

     */

    public void printPlugins () {
        Log.d(TAG, "printPlugins: " + jsonObject.toString());
    }

    public void writeJSON () {
        File file = new File(mainActivity.dir, "plugins.json");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            stream.write(jsonObject.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        setRetainInstance(true);
        return inflater.inflate(R.layout.rack,null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        if (mainActivity.pluginDialog != null) {
            // we did this already
            return ;
        }

        quickPatchProgress = mainActivity.findViewById(R.id.patch_loading);

        mainActivity.onOff = view.findViewById(R.id.onoff);
        rackMaster = view.findViewById(R.id.rack_master);
        pane_2 = view.findViewById(R.id.pane_2);
        /*
        mainActivity.onOff.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                return mainActivity.hotkeys(keyCode, event);
            }
        });

         */

        mainActivity.onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainActivity.toggleEffect(!b);
            }
        });

        patchDown = mainActivity.findViewById(R.id.patch_down);
        mainActivity.pluginDialog = mainActivity.createPluginDialog();
        if (mainActivity.useTheme)
            mainActivity.linearLayoutPluginDialog.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card(mainActivity.linearLayoutPluginDialog);

                }
            });

        toggleVideo = mainActivity.findViewById(R.id.video_button);
        if (mainActivity.useTheme) {
            mainActivity.skinEngine.toggleWithKey(mainActivity.rack.toggleVideo, "icons", "camera", "camera", true);

        }

        videoRecord = mainActivity.findViewById(R.id.toggle_video);
        videoPreview = mainActivity.findViewById(R.id.video_preview);
        swapCamera = mainActivity.findViewById(R.id.flip_camera);
        videoTexture = mainActivity.findViewById(R.id.video_texture);

        swapCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (videoRecord.isChecked()) {
                    mainActivity.toast ("Cannot flip camera while recording") ;
                    return;
                }

                toggleVideo.setChecked(false);
                toggleVideo.setChecked(true);
            }
        });
        if (mainActivity.useTheme) {
            Bitmap b = mainActivity.skinEngine.bitmapDrawable("card", "bg").getBitmap();
            if (b != null) {
                Log.d(TAG, "onViewCreated: " + b.getHeight());
                videoPreview.setBackground(new BitmapDrawable(Bitmap.createScaledBitmap(b, 90, (220 / 90) * b.getHeight(), true)));
            }
//            mainActivity.skinEngine.card(videoPreview);
        }

        videoRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mainActivity.record.isChecked()) {
                    buttonView.setChecked(false);
                    MainActivity.toast("Cannot record audio and video at the same time");
                    return;
                }

                if (isChecked)
                    mainActivity.camera2.startRecording();
                else
                    if (toggleVideo.isChecked())
                        toggleVideo.setChecked(false);
            }
        });

        toggleVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (! mainActivity.running && isChecked) {
                    MainActivity.toast("Start the audio engine to begin recording");
                    buttonView.setChecked(false);
                    return;
                }

                mainActivity.RequestCamera();
                mainActivity.videoRecording = isChecked;
                if (!isChecked) {
                    videoPreview.setVisibility(View.GONE);
                    boolean mStarted = mainActivity.camera2.mMuxerStarted ;
                    mainActivity.camera2.closeCamera();
                    mainActivity.rack.videoRecord.setChecked(false);
                    if (mStarted)
                        mainActivity.showMediaPlayerDialog();
//                    mainActivity.avBuffer.clear();
                }
                else {
                    videoPreview.setVisibility(View.VISIBLE);
                    mainActivity.camera2.openCamera();

                }

                AudioEngine.toggleVideoRecording(isChecked);
            }
        });

        mainActivity.tuner = mainActivity.findViewById(R.id.patch_label);
        Switch tunerSwitch = mainActivity.findViewById(R.id.tuner_switch);
        tunerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView _v = (TextView) mainActivity.tuner ;
                _v.setText("Please wait ...");
                mainActivity.tunerEnabled = isChecked;
                if (!mainActivity.tunerEnabled) {
                    AudioEngine.setTunerEnabled(false);
                    mainActivity.tuner.setText("Tuner");
                } else {
                    if (! mainActivity.onOff.isChecked()) {
                        mainActivity.toast ("Starting audio engine");
                        patchDown.performClick();
                    }
                    AudioEngine.setTunerEnabled(true);
                }

            }
        });

        RecyclerView recyclerView1 = (RecyclerView) mainActivity.linearLayoutPluginDialog.findViewById(R.id.plugin_dialog_recycler_view);
        recyclerView1.setLayoutManager(new LinearLayoutManager(mainActivity));
        mainActivity.pluginDialogAdapter = new PluginDialogAdapter();
        mainActivity.pluginDialogAdapter.setMainActivity(getContext(), mainActivity);
        recyclerView1.setAdapter(mainActivity.pluginDialogAdapter);

        mainActivity.triggerRecordToggle = view.findViewById(R.id.record_trigger);
        mainActivity.record = view.findViewById(R.id.record_button);
        mainActivity.triggerRecordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainActivity.outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                mainActivity.record.setEnabled(!isChecked);
                if (isChecked)
                    mainActivity.record.setText("Tri");
                else
                    mainActivity.record.setText("Rec");
                mainActivity.triggerRecord = isChecked;
                if (!isChecked && mainActivity.recording) {
                    AudioEngine.toggleRecording(false);
                    mainActivity.recording = false;
                }
            }
        });

        mainActivity.record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (videoRecord.isChecked()) {
                    compoundButton.setChecked(false);
                    MainActivity.toast("Cannot record audio and video at the same time");
                    return;
                }

                if (!mainActivity.onOff.isChecked()) {
//                        mainActivity.record.setChecked(!b);
                    if (b) {
                        compoundButton.setChecked(false);
                        MainActivity.toast("Turn on the app to start recording");
                    } else {
                        AudioEngine.toggleRecording(b);
                        mainActivity.recording = b ;
                        mainActivity.showMediaPlayerDialog();
                    }

                    return;
                }

                if (b) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!mainActivity.isStoragePermissionGranted()) {
    //                        requestReadStoragePermission();
                            mainActivity.requestWriteStoragePermission();

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
    //                        AudioEngine.setRecordingActive(b);
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

                            AudioEngine.toggleRecording(b);
                            mainActivity.recording = b ;
                        }
                    } else {
                        MainActivity.alert("Feature not supported", "Your device is too old to support this feature. Sorry.");
                        return ;
                    }
                } else {
//                    mainActivity.lastRecordedFileName = AudioEngine.getRecordingFileName();
                    AudioEngine.toggleRecording(b);
                    mainActivity.recording = b ;
                    if (!mainActivity.triggerRecord)
                        mainActivity.showMediaPlayerDialog();
                }
            }
        });

        mainActivity.fab = view.findViewById(R.id.fab);
        mainActivity.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.dataAdapter.plugins.size() > 1 && MainActivity.proVersion == false) {
                    Log.w(TAG, "onClick: " + String.format("already %d plugins in queue", mainActivity.dataAdapter.plugins.size()));
                    Intent intent = new Intent(mainActivity, Purchase.class);
                    startActivity(intent);
                    return;
                }

//                linearLayout.setBackground();
                MainActivity.applyWallpaper(mainActivity, mainActivity.pluginDialog.getWindow(), getResources(), mainActivity.pluginDialogWallpaper, mainActivity.deviceWidth, mainActivity.deviceHeight);
                mainActivity.pluginDialog.show();
            }
        });

        mainActivity.recyclerView = view.findViewById(R.id.recyclerView);
        mainActivity.recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (autoHideMixer && mixer.getVisibility() == View.VISIBLE) {
                    mainActivity.hidePanel.performClick();
                    autoHideMixer = false;
                    mainActivity.recyclerView.getLayoutManager().scrollToPosition(0);
                }
                return false;
            }
        });

        mainActivity.recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        mainActivity.dataAdapter = new DataAdapter();
        mainActivity.dataAdapter.mainActivity = mainActivity ;
        mainActivity.recyclerView.setAdapter(mainActivity.dataAdapter);

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
        AudioEngine.setDefaultStreamValues(getContext());
        if (mainActivity.lazyLoad == false)
            mainActivity.loadPlugins();
        mainActivity.loadActivePreset();

        int libraries = 0 ;
        if (mainActivity.lazyLoad == false)
            libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");

        JSONObject blacklist = ConnectGuitar.loadJSONFromAssetFile(mainActivity, "blacklist.json");
        boolean enableBlacklisted = mainActivity.defaultSharedPreferences.getBoolean("enableBlacklisted", false);

        // run this only once
        if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad == false) {
            for (int i = 0; i < libraries; i++) {
                for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                    JSONObject object = new JSONObject();
                    String name = AudioEngine.getPluginName(i, plugin);
                    int uniqueID = AudioEngine.getPluginUniqueID(i, plugin);

                    int finalI = i;
                    int finalPlugin = plugin;

                    try {
                        object.put("name", name);
                        object.put("id", uniqueID);
                        object.put("plugin", finalPlugin);
                        object.put("library", mainActivity.sharedLibraries[i]);
                        jsonObject.put(String.valueOf((finalI * 100) + finalPlugin), object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.d("Plugin: ", finalI * 100 + finalPlugin + ": " + name + "=>" + uniqueID);
                    mainActivity.pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name, uniqueID);
                }
            }
        } else if (mainActivity.pluginDialogAdapter.plugins.size() == 0 && mainActivity.lazyLoad) {
            JSONObject plugins = mainActivity.availablePluginsLV2 ;
            Iterator<String> keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (blacklist.has(key) && ! enableBlacklisted) {
                        Log.d(TAG, String.format ("[lv2] blacklisted: %s", key));
                        continue;
                    }

                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.getString("name");
                        String id = object.getString("id");
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            plugins = mainActivity.availablePlugins ;
            keys = plugins.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (plugins.get(key) instanceof JSONObject) {
//                        Log.d(TAG, "onCreate: key " + key);
                        JSONObject object = plugins.getJSONObject(key);
                        // do something with jsonObject here
                        String name = object.getString("name");
                        String id = object.getString("id");
                        if (blacklist.has(id) && ! enableBlacklisted) {
                            Log.d(TAG, String.format ("[ladspa] blacklisted: %s", key));
                            continue;
                        }
//                        Log.d(TAG, "[LV2 plugin]: " + name + ": " + id);
                        mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        MainActivity.totalPlugins = mainActivity.pluginDialogAdapter.plugins.size();
        mainActivity.hashCommands.add (this, "printPlugins");
        mainActivity.hashCommands.add (this, "writeJSON");

        MaterialButton optionsBtn = view.findViewById(R.id.menu_button);
        optionsMenu = new PopupMenu(mainActivity, optionsBtn);
        optionsMenu.getMenuInflater().inflate(R.menu.options_menu, optionsMenu.getMenu());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser() ;
        MenuItem settings = optionsMenu.getMenu().getItem(0);
        MenuItem logout = optionsMenu.getMenu().getItem(1);
        MenuItem debug = optionsMenu.getMenu().getItem(2);
        MenuItem getPro = optionsMenu.getMenu().getItem(3);
        if (mainActivity.defaultSharedPreferences.getBoolean("pro", false)) {
            getPro.setVisible(false);
            TextView textView = view.findViewById(R.id.app_main_title);
            textView.setText("Pro");
            (view.findViewById(R.id.pro_label)).setVisibility(View.VISIBLE);
        }

        getPro.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(mainActivity, Purchase.class);
                startActivity(intent);
                return true;
            }
        });

        MenuItem connectGuitar = optionsMenu.getMenu().getItem(4);
        connectGuitar.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getActivity(), ConnectGuitar.class);
                startActivity(intent);

                return true;
            }
        });

        MenuItem bug_item = optionsMenu.getMenu().getItem(5);
        MenuItem featureItem = optionsMenu.getMenu().getItem(6);
        MenuItem.OnMenuItemClickListener menuItemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                LayoutInflater inflater = getLayoutInflater();

                ConstraintLayout linearLayout = (ConstraintLayout) inflater.inflate(R.layout.bug_report, null) ;
                builder.setView(linearLayout) ;

                EditText title = linearLayout.findViewById(R.id.bug_title);
                EditText desc = linearLayout.findViewById(R.id.bug_description);
                EditText email = linearLayout.findViewById(R.id.bug_email);
                CheckBox notify = linearLayout.findViewById(R.id.bug_notify);

                AlertDialog alertDialog = builder.create();
                MaterialButton submit = linearLayout.findViewById(R.id.bug_submit);
                submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        saveBugReport(alertDialog, title.getText().toString(), desc.getText().toString(),
                                email.getText().toString(), notify.isChecked());
                    }
                });

                alertDialog.show();
                return true;
            }
        } ;

        bug_item.setOnMenuItemClickListener(menuItemClickListener) ;
        featureItem.setOnMenuItemClickListener(menuItemClickListener) ;

        MenuItem howToUse = optionsMenu.getMenu().getItem(7);
        howToUse.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String url = "https://amprack.acoustixaudio.org";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);

                return false;
            }
        });

        MenuItem saveCollection = optionsMenu.getMenu().getItem(8);
        saveCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mainActivity.saveCollection();
                return false;
            }
        });

        MenuItem loadCollection = optionsMenu.getMenu().getItem(9);
        loadCollection.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent_upload = new Intent();
//                intent_upload.setType("application/json");
                intent_upload.setType("*/*");
                intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().startActivityForResult(intent_upload,100);
                return true;
            }
        });

        MenuItem tuneLatency = optionsMenu.getMenu().getItem(10);
        tuneLatency.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                MainActivity.toast(AudioEngine.tuneLatency());
                return false;
            }
        });

        MenuItem glitch = optionsMenu.getMenu().findItem(R.id.glitch);
        glitch.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                if (! mainActivity.running)
                    return false;

                AudioEngine.fixGlitches();
                Toast.makeText(mainActivity,
                        String.format(
                                "Latency: %.0f, Buffer size: %d",
                                AudioEngine.getLatency(true) + AudioEngine.getLatency(false),
                                AudioEngine.getBufferSizeInFrames(false)
                        )
                        , Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        MenuItem minimize = optionsMenu.getMenu().findItem(R.id.minimize);
        minimize.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                if (! mainActivity.running)
                    return false;

                AudioEngine.minimizeLatency();
                Toast.makeText(mainActivity,
                        String.format(
                                "Latency: %.0f, Buffer size: %d",
                                AudioEngine.getLatency(true) + AudioEngine.getLatency(false),
                                AudioEngine.getBufferSizeInFrames(false)
                        )
                        , Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        MenuItem exit_item = optionsMenu.getMenu().findItem(R.id.menu_exit);
        exit_item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mainActivity.finish();
                return true;
            }
        });

        debug.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
//                mainActivity.printDebugLog();
                AudioEngine.clearActiveQueue();
                mainActivity.dataAdapter.reset();
                return true;
            }
        });

        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return false;
            }
        });

        logout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage("You are logged in as " + firebaseAuth.getCurrentUser().getEmail())
                        .setTitle("Are you sure you want to log out?");

                builder.setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("983863263684-6ggjm8spjvvftm5noqtpl97v0le5laft.apps.googleusercontent.com")
                                .requestEmail()
                                .build();

                        GoogleSignIn.getClient(getContext(), gso).signOut();
                        firebaseAuth.signOut();

                        mainActivity.presets.loginNotice.setVisibility(View.VISIBLE);
                        mainActivity.presets.tabLayout.setVisibility(View.INVISIBLE);
                        if (mainActivity.presets.fragmentStateAdapter.myPresets.myPresetsAdapter != null)
                            mainActivity.presets.fragmentStateAdapter.myPresets.myPresetsAdapter.removeAll();
                        logout.setVisible(false);
                        mainActivity.presets.progressPreset.setVisibility(View.INVISIBLE);

                        Toast.makeText(mainActivity.getApplicationContext(),
                                "You have been logged out",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

                return false;
            }
        });

        if (user == null) {
            logout.setVisible(false);
        }

        optionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsMenu.show();
            }
        });

        ImageView logoBtn = view.findViewById(R.id.logo_img) ;
        /*
        logoBtn.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                if (event.getAction() == KeyEvent.ACTION_UP)
                    mainActivity.hotkeys(keyCode, event);
                return false;
            }
        });

         */

        logoBtn.setLongClickable(true);
        logoBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                HashCommands commands = new HashCommands(mainActivity.context);
                commands.mainActivity = mainActivity;
                commands.show();
                return true;
            }
        });

        ///| TODO: Remove this
        logoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mainActivity.drummer ();
//                if (BuildConfig.DEBUG)
//                    mainActivity.cameraPreview();
//                else
                MainActivity.setAudioDevice();
            }
        });

        LinearLayout inputMixer = mainActivity.findViewById(R.id.mixer_input);
        LinearLayout outputMixer = mainActivity.findViewById(R.id.mixer_output);
        mixer = mainActivity.findViewById(R.id.mixer);

        mainActivity. toggleMixer = mainActivity.findViewById(R.id.mixer_toggle);

        mainActivity.hidePanel = mainActivity.findViewById(R.id.hide_panel);
        mainActivity.hidePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.toggleMixer.setChecked(!mainActivity.toggleMixer.isChecked());
            }
        });

        mainActivity.toggleMixer.setOnCheckedChangeListener((compoundButton, b) -> {
            Log.d(TAG, "onViewCreated() called with: view = [" + view + "], savedInstanceState = [" + savedInstanceState + "]");
            AudioEngine.toggleMixer(!b);
            if (mainActivity.useTheme)
                mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", !b);
            if (!b) {

                if (! mixerInit) {
                    mixer.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run: theming mixer");
                            if (mainActivity.useTheme)
                                mainActivity.skinEngine.card(mixer);

                        }
                    });
                }
                mixer.setVisibility(View.VISIBLE);
            } else {
                mixer.setVisibility(View.GONE);
            }
        });

        mainActivity.toggleMixer.setChecked(mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", false));
        Log.d(TAG, "onViewCreated: toggle mixer: " + mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", false));
        mainActivity. inputVolume = mainActivity.findViewById(R.id.mixer_input_slider);
        mainActivity. outputVolume = mainActivity.findViewById(R.id.mixer_output_slider);

        mainActivity.inputMeter = mainActivity.findViewById(R.id.mixer_input_progress);
        mainActivity.outputMeter = mainActivity.findViewById(R.id.mixer_output_progress);
        mainActivity.inputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        mainActivity.outputMeter.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

        mainActivity. inputVolume.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                AudioEngine.setInputVolume(value);
            }
        });

        mainActivity. outputVolume.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                AudioEngine.setOutputVolume(value);
            }
        });

        mainActivity.inputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f));
        mainActivity.outputVolume.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f));

        ToggleButton toggleButton = mainActivity.findViewById(R.id.onofftoggle);
        patchUp = mainActivity.findViewById(R.id.patch_up);
        TextView patchName = mainActivity.findViewById(R.id.patch_name),
                patchNo = mainActivity.findViewById(R.id.patch_no);

        TextView patchDesc = mainActivity.findViewById(R.id.patch_desc);
        mainActivity.patchDesc = patchDesc ;
        mainActivity.patchName = patchName ;
        mainActivity.patchNo = patchNo;

        patchDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patchName.performClick();
            }
        });

        LinearLayout patchMaster = mainActivity.findViewById(R.id.patch_master);
        patchMaster.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (patchName.getText().equals("Tap to load"))
                patchName.performClick();
                return false;
            }
        });

        patchName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (! patchName.getText().equals("Tap to load"))
//                    return false;

                if (! mainActivity.running) {
                    if (patchName.getText().equals("Tap to load") && mainActivity.dataAdapter.totalItems == 0) {
                        MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                            @Override
                            void run() {
                                patchDown.performClick();
                            }
                        };
                    } else if (patchName.getText().equals("Tap to load")){
                        patchName.setText("Custom");
                    }

                    if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                        mainActivity.onOff.setChecked(true);

                    else if (mainActivity.useTheme && ! toggleButton.isChecked())
                        toggleButton.setChecked(true);

                } else {
                    if (patchName.getText().equals("Tap to load"))
                        patchDown.performClick();
                }
                return false;
            }
        });
        patchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(true);
                        }
                    };

                } else {
                    patchMove(true);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);

            }
        });

        patchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.running) {
                    MainActivity.OnEngineStartListener engineStartListener = new MainActivity.OnEngineStartListener() {
                        @Override
                        void run() {
                            patchMove(false);
                        }
                    };
                } else {
                    patchMove(false);
                    return;
                }

                if (! mainActivity.useTheme && ! mainActivity.onOff.isChecked())
                    mainActivity.onOff.setChecked(true);
                else if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);
            }
        });

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.cardText(mainActivity.patchDesc);
            mainActivity.skinEngine.cardText(mainActivity.hidePanel);
            mainActivity.skinEngine.cardText(mainActivity.triggerRecordToggle);
            mainActivity.toggleMixer.setCompoundDrawables(null,null,null,null);

            TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label),
                    inLabel = mainActivity.findViewById(R.id.mixer_input_label),
                    patchLabel = mainActivity.findViewById(R.id.patch_label),
                    inRotaryDisplay = mainActivity.findViewById(R.id.rotary_input_display),
                    inRotaryLabel = mainActivity.findViewById(R.id.rotary_input_label),
                    outRotaryDisplay = mainActivity.findViewById(R.id.rotary_output_display),
                    outRotaryLabel = mainActivity.findViewById(R.id.rotary_output_label),
                    outLabel = mainActivity.findViewById(R.id.mixer_output_label);

            mainActivity.skinEngine.cardText(mixerLabel);
            mainActivity.skinEngine.cardText(patchName);
            mainActivity.skinEngine.cardText(inLabel);
            mainActivity.skinEngine.cardText(outLabel);
            mainActivity.skinEngine.cardText(patchLabel);
            mainActivity.skinEngine.cardText(patchNo);

            mainActivity.skinEngine.cardText(inRotaryDisplay);
            mainActivity.skinEngine.cardText(outRotaryDisplay);
            mainActivity.skinEngine.cardText(inRotaryLabel);
            mainActivity.skinEngine.cardText(outRotaryLabel);

            boolean mixerIsVisible  = false ;
            if (mixer.getVisibility() == View.VISIBLE)
                mixerIsVisible = true ;

            mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", mixerIsVisible);

            ImageView wallpaper = mainActivity.findViewById(R.id.wallpaper);
            mainActivity.skinEngine.wallpaper(wallpaper);
            mainActivity.skinEngine.header(mainActivity.findViewById(R.id.master_button_box));

            mainActivity.skinEngine.toggle(toggleButton, false);
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "onCheckedChanged() called with: buttonView = [" + buttonView + "], isChecked = [" + isChecked + "]");
//                    mainActivity.toggleEffect(!isChecked);
                    mainActivity.onOff.setChecked(isChecked);
                    mainActivity.skinEngine.toggle(toggleButton, isChecked);
                }
            });

            toggleButton.setVisibility(View.VISIBLE);
            mainActivity.onOff.setVisibility(View.GONE);

            mainActivity.skinEngine.view (optionsBtn, "menu", "overflow", SkinEngine.Resize.Height, .5f);
            optionsBtn.setCompoundDrawables(null, null, null, null);
            mainActivity.skinEngine.setLogo(mainActivity.findViewById(R.id.logo_img));

            mainActivity.skinEngine.fab(mainActivity.fab,  SkinEngine.Resize.Width, 1);

            mainActivity.skinEngine.slider(mainActivity.inputVolume);
            mainActivity.skinEngine.slider(mainActivity.outputVolume);
            mainActivity.skinEngine.card (mixer);

            if (mixer.getVisibility() == View.VISIBLE) {
                mixerInit = true ;
//                mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", true);

                mixer.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: theming mixer");
                        mainActivity.skinEngine.card(mixer);
                        mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", true);

                    }
                });
            }

            if (mainActivity.skinEngine.hasKnob()) {
                LinearLayout rotaryRack = mainActivity.findViewById(R.id.rotary_rack);
                rotaryRack.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn = mainActivity.findViewById(R.id.rotary_input_volume);
                mainActivity.rotarySeekbarOut = mainActivity.findViewById(R.id.rotary_output_volume);
                mainActivity.displayIn = mainActivity.findViewById(R.id.rotary_input_display);
                mainActivity.displayOut = mainActivity.findViewById(R.id.rotary_output_display);
                mainActivity.skinEngine.rotary(mainActivity.rotarySeekbarIn, 3, 0, 100, 50);

                mainActivity.skinEngine.rotary(mainActivity.rotarySeekbarOut, 3, 0, 100, 50);
                mainActivity.rotarySeekbarIn.setValue(mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f) * 100);
                mainActivity.rotarySeekbarOut.setValue(mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)*100);
                mainActivity.displayIn.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("inputVolume", 1.0f)* 100)));
                mainActivity.displayOut.setText(String.valueOf((int) (mainActivity.defaultSharedPreferences.getFloat("outputVolume", 1.0f)* 100)));

                mainActivity.rotarySeekbarIn.setVisibility(View.VISIBLE);
                mainActivity.rotarySeekbarOut.setVisibility(View.VISIBLE);

                mainActivity.rotarySeekbarIn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarIn.getValue() ;
                        mainActivity.displayIn.setText(String.valueOf((int) value));
                        mainActivity.inputVolume.setValue(value/100);
                        return false;
                    }
                });

                mainActivity.rotarySeekbarOut.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        float value = mainActivity.rotarySeekbarOut.getValue() ;
                        mainActivity.displayOut.setText(String.valueOf((int) value));
                        mainActivity.outputVolume.setValue(value/100);
                        return false;
                    }
                });


                mainActivity.inputVolume.setVisibility(View.GONE);
                mainActivity.outputVolume.setVisibility(View.GONE);
            }
        }

//        mainActivity.audioEncoder = new AudioEncoder(mainActivity);
        mainActivity.camera2 = new Camera2 (mainActivity);
        mainActivity.sampleRateLabel = mainActivity.findViewById(R.id.sample_rate_display);
        mainActivity.srLayout = mainActivity.findViewById(R.id.sr_layout);
        mainActivity.latencyWarnLogo = mainActivity.findViewById(R.id.lowLatencyWarning);
        mainActivity.latencyWarnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.lowLatencyDialog();
            }
        });

        if (mainActivity.tabletMode) {
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(mainActivity.deviceWidth/2, mainActivity.deviceHeight);
            pane_2.setLayoutParams(layoutParams);
//            rackMaster.setLayoutParams(layoutParams);

            ConstraintLayout constraintLayout = mainActivity.findViewById(R.id.super_parent);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            constraintSet.connect(R.id.pane_2, ConstraintSet.RIGHT,R.id.super_parent,ConstraintSet.RIGHT,0);
//            constraintSet.connect(R.id.rack_master, ConstraintSet.LEFT,R.id.super_parent,ConstraintSet.LEFT,0);
            constraintSet.connect(R.id.pane_2,ConstraintSet.TOP,R.id.super_parent,ConstraintSet.TOP,0);
//            constraintSet.connect(R.id.pane_2,ConstraintSet.LEFT,R.id.rack_master,ConstraintSet.RIGHT,0);
            constraintSet.applyTo(constraintLayout);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            rackMaster.setLayoutParams(lp);
            MainActivity.applyWallpaper(mainActivity, mainActivity.pluginDialog.getWindow(), getResources(), mainActivity.pluginDialogWallpaper, mainActivity.deviceWidth, mainActivity.deviceHeight);

            mainActivity.fab.setVisibility(View.GONE);
            Log.w(TAG, "onViewCreated: tablet mode activated");
        } else {
            Log.d(TAG, String.format ("[display dimensions]: %d x %d {%f}", mainActivity.deviceWidth, mainActivity.deviceHeight, (float) (1.0 * mainActivity.deviceWidth/mainActivity.deviceHeight)));
        }

        if (mainActivity.experimentalBuild) {
            TextView mixerLabel = mainActivity.findViewById(R.id.mixer_label);
            mixerLabel.setText("Beta " + String.valueOf(BuildConfig.VERSION_CODE));
//            mixerLabel.setTextColor(getResources().getColor(R.color.dark_red));
//            mixerLabel.setBackgroundColor(getResources().getColor(R.color.wheat));
        }

    }

    public void saveBugReport (AlertDialog dialog, String title, String description, String email, boolean notify) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();

        data.put("title", title);
        data.put ("desc", description);
        data.put ("email", email) ;
        data.put ("notify", notify);
        data.put ("timestamp",  FieldValue.serverTimestamp());
        db.collection("bug_reports")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        dialog.dismiss();
                        Toast.makeText(mainActivity,
                                        "Bug report sent successfully",
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        MainActivity.toast(e.getMessage());
                        Log.e(TAG, "onFailure: cannot save bug report", e);
                    }
                });
    }

    public void patchMove (boolean up) {
        String text = String.valueOf(mainActivity.patchNo.getText()) ;
        if (text == "-")
            text = "0" ;
        int p = Integer.valueOf(text);
        if (up)
            p ++ ;
        else
            p -- ;

        if (p >= mainActivity.quickPatch.myPresetsAdapter.allPresets.size() || p < 0)
            return ;

        if (mainActivity.quickPatch.myPresetsAdapter.allPresets.isEmpty()) {
            MainActivity.alert("Restart the app to load patches", "Patches are not loaded. Restart the app to load patches.");
            return ;
        }

        mainActivity.loadPreset(mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p));
        mainActivity.patchNo.setText(String.valueOf(p));
        mainActivity.patchName.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("name"));
        mainActivity.patchDesc.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("desc"));

    }

    public void latencyDialog () {
        LinearLayout linearLayout = (LinearLayout) mainActivity.getLayoutInflater().inflate(R.layout.latency_tuner, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity);
        builder.setTitle("Latency Tuner")
                .setView(linearLayout)
                .setPositiveButton("Close", null);
    }
}
