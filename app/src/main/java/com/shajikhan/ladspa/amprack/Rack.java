package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Rack extends Fragment {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName();
    PopupMenu optionsMenu ;
    JSONObject jsonObject = new JSONObject();
    boolean mixerInit = false ;
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

        SwitchMaterial onOff = view.findViewById(R.id.onoff);
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mainActivity.toggleEffect(!b);
            }
        });

        mainActivity.pluginDialog = mainActivity.createPluginDialog();

        RecyclerView recyclerView1 = (RecyclerView) mainActivity.linearLayoutPluginDialog.findViewById(R.id.plugin_dialog_recycler_view);
        recyclerView1.setLayoutManager(new LinearLayoutManager(mainActivity));
        mainActivity.pluginDialogAdapter = new PluginDialogAdapter();
        mainActivity.pluginDialogAdapter.setMainActivity(getContext(), mainActivity);
        recyclerView1.setAdapter(mainActivity.pluginDialogAdapter);

        mainActivity.record = view.findViewById(R.id.record_button);
        mainActivity.record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (onOff.isChecked()) {
                        MainActivity.toast("Cannot start or stop recording while playing");
                        mainActivity.record.setChecked(!b);
                        return;
                    }

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
                        AudioEngine.setRecordingActive(b);
                    }
                }
            }
        });

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
                        Log.d(TAG, "[LV2 plugin]: " + name + ": " + id);
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
        bug_item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
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
                Button submit = linearLayout.findViewById(R.id.bug_submit);
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
        }) ;

        MenuItem howToUse = optionsMenu.getMenu().getItem(6);
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

        MenuItem exit_item = optionsMenu.getMenu().getItem(7);
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
                        logout.setVisible(false);

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

        ImageView logoBtn = view.findViewById(R.id.logo_img);
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


        LinearLayout inputMixer = mainActivity.findViewById(R.id.mixer_input);
        LinearLayout outputMixer = mainActivity.findViewById(R.id.mixer_output);
        LinearLayout mixer = mainActivity.findViewById(R.id.mixer);

        mainActivity. toggleMixer = mainActivity.findViewById(R.id.mixer_toggle);

        mainActivity.toggleMixer.setOnCheckedChangeListener((compoundButton, b) -> {
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
        Log.d(TAG, "onViewCreated: toggle mixer: " + mainActivity.defaultSharedPreferences.getBoolean("toggleMixer", true));
        mainActivity. inputVolume = mainActivity.findViewById(R.id.mixer_input_slider);
        mainActivity. outputVolume = mainActivity.findViewById(R.id.mixer_output_slider);

        mainActivity.inputMeter = mainActivity.findViewById(R.id.mixer_input_progress);
        mainActivity.outputMeter = mainActivity.findViewById(R.id.mixer_output_progress);

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
        Button patchUp = mainActivity.findViewById(R.id.patch_up),
                patchDown = mainActivity.findViewById(R.id.patch_down);
        TextView patchName = mainActivity.findViewById(R.id.patch_name),
                patchNo = mainActivity.findViewById(R.id.patch_no);

        patchName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (patchName.getText().equals("Tap to load"))
                    patchDown.performClick();
                return false;
            }
        });
        patchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.useTheme && ! onOff.isChecked())
                    onOff.setChecked(true);

                if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);

                int p = Integer.valueOf(String.valueOf(patchNo.getText()));
                p ++ ;
                if (p >= mainActivity.quickPatch.myPresetsAdapter.allPresets.size())
                    return ;

                if (mainActivity.quickPatch.myPresetsAdapter.allPresets.isEmpty()) {
                    MainActivity.alert("Restart the app to load patches", "Patches are not loaded. Restart the app to load patches.");
                    return ;
                }

                mainActivity.loadPreset(mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p));
                patchNo.setText(String.valueOf(p));
                patchName.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("name"));
            }
        });

        patchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! mainActivity.useTheme && ! onOff.isChecked())
                    onOff.setChecked(true);

                if (mainActivity.useTheme && ! toggleButton.isChecked())
                    toggleButton.setChecked(true);

                int p = Integer.valueOf(String.valueOf(patchNo.getText()));
                p -- ;
                if (p < 0)
                    p = 0 ;

                if (mainActivity.quickPatch.myPresetsAdapter.allPresets.isEmpty()) {
                    MainActivity.alert("Restart the app to load patches", "Patches are not loaded. Restart the app to load patches.");
                    return ;
                }

                mainActivity.loadPreset(mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p));
                patchNo.setText(String.valueOf(p));
                patchName.setText((CharSequence) mainActivity.quickPatch.myPresetsAdapter.allPresets.get(p).get("name"));
            }
        });

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.toggleWithKey(mainActivity.toggleMixer, "icons", "mixer-on", "mixer-off", false);
            ImageView wallpaper = mainActivity.findViewById(R.id.wallpaper);
            mainActivity.skinEngine.wallpaper(wallpaper);
            mainActivity.skinEngine.header(mainActivity.findViewById(R.id.master_button_box));

            mainActivity.skinEngine.toggle(toggleButton, false);
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mainActivity.toggleEffect(!isChecked);
                    mainActivity.skinEngine.toggle(toggleButton, isChecked);
                }
            });

            toggleButton.setVisibility(View.VISIBLE);
            onOff.setVisibility(View.GONE);

            mainActivity.skinEngine.view (optionsBtn, "menu", "overflow", SkinEngine.Resize.Height, .5f);
            optionsBtn.setCompoundDrawables(null, null, null, null);
            mainActivity.skinEngine.setLogo(mainActivity.findViewById(R.id.logo_img));

            mainActivity.skinEngine.fab(fab,  SkinEngine.Resize.Width, 1);

            mainActivity.skinEngine.slider(mainActivity.inputVolume);
            mainActivity.skinEngine.slider(mainActivity.outputVolume);
            mainActivity.skinEngine.card (mixer);

            if (mixer.getVisibility() == View.VISIBLE) {
                mixerInit = true ;
                mixer.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: theming mixer");
                        mainActivity.skinEngine.card(mixer);

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
}
