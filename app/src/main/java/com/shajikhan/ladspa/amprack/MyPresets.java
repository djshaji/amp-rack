package com.shajikhan.ladspa.amprack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyPresets extends Fragment {
    MainActivity mainActivity;
    Spinner quickSpinner = null ;
    String TAG = getClass().getSimpleName();
    RecyclerView recyclerView;
    public MyPresetsAdapter myPresetsAdapter ;
    FirestoreDB db ;
    ProgressBar progressBar = null;
    LinearLayout quickHeader = null ;
    PopupMenu sortMenu = null;
    boolean shared = false;
    boolean quick = false ;

    public MyPresets () {

    }

    MyPresets (ProgressBar _progressBar) {
        progressBar = _progressBar;
    }

    MyPresets (boolean _shared) {
        shared = _shared;
    }
    MyPresets (boolean _shared, boolean _quick) {
        shared = _shared;
        quick = _quick;
    }

    void load () {
        db.getFavorites(myPresetsAdapter, shared, quick);
        myPresetsAdapter.quick = quick ;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_presets, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        db = new FirestoreDB (mainActivity);

        recyclerView = (RecyclerView) ((LinearLayout) view).getChildAt(2);
        quickHeader = (LinearLayout) ((LinearLayout) view).getChildAt(0);
        recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        myPresetsAdapter = new MyPresetsAdapter();
        myPresetsAdapter.setMainActivity(mainActivity);
        recyclerView.setAdapter(myPresetsAdapter);
        myPresetsAdapter.quickPatchProgress = mainActivity.rack.quickPatchProgress;

        LinearLayout layout = (LinearLayout) view ;
        LinearLayout lx = (LinearLayout) ((LinearLayout) view).getChildAt(1);
        if (! shared) {
            lx.setVisibility(View.GONE);
        } else {
            EditText editText = (EditText) lx.getChildAt(0);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    Log.d(TAG, "afterTextChanged: " + editable.toString());
                    myPresetsAdapter.updateList(editable.toString());
                }
            });

            ToggleButton toggleButton = (ToggleButton) lx.getChildAt(1);
            toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    myPresetsAdapter.showOnlyFavorites(b);
                    if (b)
                        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
                    else
                        toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
                }
            });
        }

        Button sortByButton = (Button) lx.getChildAt(2);
        sortMenu = new PopupMenu(mainActivity, sortByButton);
        sortMenu.getMenuInflater().inflate(R.menu.sort_by, sortMenu.getMenu());

        /*
        switch (((MainActivity) getActivity()).defaultSharedPreferences.getString("orderBy", "timestamp")) {
            default:
            case "timestamp":
                sortMenu.getMenu().findItem(2).setChecked(true);
                break ;
            case "likes":
                sortMenu.getMenu().findItem(1).setChecked(true);
                break ;
        }

         */

        sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                SharedPreferences sharedPreferences = ((MainActivity) getActivity()).defaultSharedPreferences;
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    default:
                    case R.id.sort_by_time:
                        myPresetsAdapter.sortBy = "timestamp";
                        sharedPreferences.edit().putString("orderBy", "timestamp").apply();
                        break ;
                    case R.id.sort_by_likes:
                        myPresetsAdapter.sortBy = "likes";
                        sharedPreferences.edit().putString("orderBy", "likes").apply();
                        break ;
                }

                myPresetsAdapter.removeAll();
                db.loadUserPresets(myPresetsAdapter,shared, quick);
                return true;
            }
        });

        if (quick) {
            List<String> labels = new ArrayList<String>();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            quickHeader.setVisibility(View.VISIBLE);
            quickSpinner = (Spinner) quickHeader.getChildAt(1);
            labels.add("Factory Presets");
            Set<String> vals = sharedPreferences.getStringSet("collections", null) ;
            if (vals != null) {
                Log.d(TAG, "onViewCreated: adding collections " + vals);
                for (String v: vals) {
                    labels.add(v);
                }
            }

            ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, labels);
            quickSpinner.setAdapter(categoriesDataAdapter);
            quickSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    myPresetsAdapter.removeAll();
                    if (position == 0) {
                        db.getFavorites(myPresetsAdapter, shared, quick);
                        return;
                    }

                    Set<String> vals = sharedPreferences.getStringSet("collections", null) ;
                    Log.d(TAG, "onItemSelected: " + vals);
                    String name = (String) vals.toArray() [position - 1];
                    String s = sharedPreferences.getString(name, null);
                    if (s == null) {
                        MainActivity.toast("Cannot load collection " + name);
                        return;
                    }

                    Log.d(TAG, "onItemSelected: loading collection " + name + "\n" + s);

                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        Iterator<String> keys = jsonObject.keys();

                        while(keys.hasNext()) {
                            String key = keys.next();
                            JSONObject preset = jsonObject.getJSONObject(key);
                            Log.d(TAG, "onCreate: key " + key);
                            HashMap<String, Object> yourHashMap = new Gson().fromJson(preset.toString(), HashMap.class);

                            myPresetsAdapter.addPreset(yourHashMap);
                        }
                    } catch (JSONException e) {
                        MainActivity.toast("Cannot load collection " + name + "\n"+e.getLocalizedMessage());
                        Log.e(TAG, "onItemClick: ", e);
                        return;
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        if (!shared) {
            sortByButton.setVisibility(View.GONE);
        }

        sortByButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortMenu.show();
            }
        });

        if (progressBar != null)
            myPresetsAdapter.setProgressBar(progressBar);
        db.getFavorites(myPresetsAdapter, shared, quick);
    }
}