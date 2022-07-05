package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Rack extends Fragment {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName();
    PopupMenu optionsMenu ;

    /*
    Rack () {
        mainActivity = (MainActivity) getActivity();
    }

    Rack (MainActivity activity) {
        mainActivity = activity ;
    }

     */

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

        RecyclerView recyclerView1 = (RecyclerView) mainActivity.linearLayoutPluginDialog.getChildAt(2);
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
        mainActivity.loadPlugins();
        mainActivity.loadActivePreset();

        int libraries = AudioEngine.getSharedLibraries();
        Log.d(TAG, "Creating dialog for " + libraries + " libraries");

        // run this only once
        if (mainActivity.pluginDialogAdapter.plugins.size() == 0) {
            for (int i = 0; i < libraries; i++) {
                for (int plugin = 0; plugin < AudioEngine.getPlugins(i); plugin++) {
                    String name = AudioEngine.getPluginName(i, plugin);
                    int uniqueID = AudioEngine.getPluginUniqueID(i, plugin);
                    int finalI = i;
                    int finalPlugin = plugin;
                    mainActivity.pluginDialogAdapter.addItem(finalI * 100 + finalPlugin, name, uniqueID);
                }
            }
        }

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
            textView.setText("Amp Pro");
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

        MenuItem exit_item = optionsMenu.getMenu().getItem(6);
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
