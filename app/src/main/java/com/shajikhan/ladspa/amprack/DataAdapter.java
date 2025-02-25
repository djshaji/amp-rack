package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.text.LineBreaker;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    String [] bypassContains = {
            "Pull the",
            "witch",
            "oggle"
    };
    String [] bypassIs = {
            "prefilter",
            "bypass",
            "stick it!",
            "vibe",
            "hold"
    } ;

    int totalItems = 0;
    int xOffset = 0, yOffset = 0 ;
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    int primaryColor = com.google.android.material.R.color.design_default_color_primary;
    ArrayList <Integer> plugins = new ArrayList<>();
    ArrayList <ViewHolder> holders = new ArrayList<>();
    HashMap <Integer, Integer> selectedModels = new HashMap<>();
    HashMap <Integer, String> audioFiles = new HashMap<>();
    MainActivity mainActivity;

    @NonNull
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        totalItems++;
        if (context == null) context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin_ui, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holders.add(holder);
        LinearLayout linearLayout = holder.getLinearLayout();
        linearLayout.removeAllViews();
        holder.sliders = new ArrayList<>();
        if (linearLayout == null) {
            Log.wtf(TAG, "linear layout for plugin!") ;
            return ;
        }

        holder.switchMaterial.setUseMaterialThemeColors(true);
        holder.switchMaterial.setChecked(true);
        holder.switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AudioEngine.togglePlugin(holder.getAdapterPosition(), b);
            }
        });

        String pluginName = AudioEngine.getActivePluginName(position) ;
        boolean hasFilePort = AudioEngine.getFilePort(position);
        if (pluginName == null) {
            Log.e(TAG, "onBindViewHolder: plugin name returned null, what are we even doing?", null);
//            notifyItemRemoved(position);
            return;
        } else {
            Log.d(TAG, "onBindViewHolder: creating UI for " + pluginName);
        }

        holder.getTextView().setText(pluginName);

        if (mainActivity.useTheme)
            mainActivity.skinEngine.cardText(holder.getTextView());
        int numControls = AudioEngine.getPluginControls(position);
        int notSliders = 0 ;

        for (int i = 0 ; i < numControls ; i ++) {
            if (AudioEngine.getControlType(position, i) > 1)
                notSliders++;
        }

        LinearLayout knobsLayout = new LinearLayout (mainActivity);
        knobsLayout.setOrientation(LinearLayout.HORIZONTAL);
//        knobsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(knobsLayout);
        int knobslayer = 0;
        holder.pluginsHasKnobs = true;
        JSONObject knobsConfig = null;
        try {
            knobsConfig = mainActivity.knobsLayout.getJSONObject(String.valueOf(numControls - notSliders));
        } catch (JSONException e) {
            holder.pluginsHasKnobs = false ;
            Log.e(TAG, "onBindViewHolder: no json config for knobs: " + numControls, e);
        }

        for (int i = 0 ; i < numControls ; i ++) {
            boolean isAtom = false ;
            if (AudioEngine.getControlType(position, i) == 3)
                isAtom = true ;

            LinearLayout layout = new LinearLayout(context);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            float [] vals = AudioEngine.getPluginControlValues(position, i) ;
            float presetValue = AudioEngine.getPluginPresetValue(position, i) ;
            String string = AudioEngine.getControlName(position, i) ;
            TextView textView = new TextView(context);
//            textView.setRotation(-90f);
            textView.setText(string);
            LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0);
            textViewParams.setMargins(20,0,0,0);

            textView.setLayoutParams(textViewParams);
//            textView.setHeight(30);
//            layout.addView(textView);
            linearLayout.addView(textView);
            linearLayout.addView(layout);

            EditText editText = new EditText(context);
            Slider slider = new Slider(context);

            int finalI6 = i;
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mainActivity.setMidiControl(slider, position, finalI6, MIDIControl.Type.SLIDER, MIDIControl.Scope.PLUGIN);
                    return false;
                }
            });

            slider.setId(100 * position + i);
            Spinner spinner = new Spinner(context);
            SeekBar seekBar = new SeekBar(context);
            boolean isSpinner = false ;
            boolean isBypass = false ;
            Button prev = null, next = null;
            if (string != null) {
                /*
                Log.d(TAG, "onBindViewHolder: control name: " + string +
                        " -> " + string.equalsIgnoreCase("bypass"));

                 */

                for (String s: bypassContains)
                    if (string.contains(s)) {
                        isBypass = true ;
                        break;
                    }

                if (! isBypass) {
                    for (String s: bypassIs)
                        if (string.equalsIgnoreCase(s)) {
                            isBypass = true ;
                            break;
                        }

                    // new: get if control is bypass from json
                    // remember I added this here
                    // coz it wasnt here before
                    if (AudioEngine.getControlType(position, i) == 2)
                        isBypass = true;
                }

//                isBypass = string.equalsIgnoreCase("bypass") ||  || string.contains("witch") || string.equalsIgnoreCase("prefilter");
            }

            if (mainActivity.useTheme) {
                mainActivity.skinEngine.cardText(textView);

                mainActivity.skinEngine.slider(slider);
                holder.toggleButton.setChecked(true);
                mainActivity.skinEngine.toggle(holder.toggleButton, true);
                holder.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d(TAG, "onCheckedChanged() called with: buttonView = [" + buttonView + "], isChecked = [" + isChecked + "]");
                        holder.switchMaterial.setChecked(isChecked);
                        mainActivity.skinEngine.toggle(holder.toggleButton, isChecked);
                    }
                });

            }

//            Log.d(TAG, "onBindViewHolder: loading plugin -> " + pluginName );
            if (mainActivity.ampModels.has(pluginName)) {
//                Log.d(TAG, "onBindViewHolder: found amp model " + pluginName);
                JSONObject control ;
                ArrayList <String> models = new ArrayList<>();
                try {
                    control = mainActivity.ampModels.getJSONObject(pluginName) ;
                    if (control.has(String.valueOf(i))) {
//                        Log.d(TAG, "onBindViewHolder: found control " + i);
                        isSpinner = true ;
                        JSONArray modelsData = control.getJSONArray(String.valueOf(i));
                        for (int x_ = 0 ; x_ < modelsData.length() ; x_++) {
//                            Log.d(TAG, "onBindViewHolder: " + modelsData.getString(x_));
                            models.add(modelsData.getString(x_));
                        }

                    }/* else {
                        Log.d(TAG, "onBindViewHolder: no control for " + i);
                    }*/

                } catch (JSONException e) {
                    Log.e(TAG, "onBindViewHolder: error parsing amp model " + pluginName, e);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, models);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
//                spinner.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                prev = new Button(context);
                next = new Button(context);

                prev.setText("<");
                next.setText(">");

                Spinner finalSpinner3 = spinner;
                prev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = finalSpinner3.getSelectedItemPosition();
                        if (selected > 0)
                            finalSpinner3.setSelection(selected - 1);
                    }
                });

                Spinner finalSpinner2 = spinner;
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = finalSpinner2.getSelectedItemPosition();
                        if (selected < adapter.getCount() - 1)
                            finalSpinner2.setSelection(selected + 1);
                    }
                });
            }

            if (isSpinner) {
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                next.setLayoutParams(layoutParams);
                prev.setLayoutParams(layoutParams);

                prev.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                next.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));

                layout.addView(prev);
                layout.addView(spinner);
                layout.addView(next);

                slider.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);
            }

            ToggleButton bypass = new ToggleButton(mainActivity);
            if (isBypass) {
//                Log.d(TAG, "onBindViewHolder: turning on bypass switch");
                int finalI5 = i;
                bypass.setId(100 * position + i);
                bypass.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mainActivity.setMidiControl(v, position, finalI5, MIDIControl.Type.TOGGLE, MIDIControl.Scope.PLUGIN);
                        return false;
                    }
                });

                bypass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked)
                            slider.setValue(0f);
                        else
                            slider.setValue(1f);

//                        if (mainActivity.useTheme)
//                            mainActivity.skinEngine.toggle(bypass, isChecked);
                    }
                });

                slider.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);
                linearLayout.addView(bypass);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(10,10,10,10);
                bypass.setGravity (Gravity.CENTER);
                layoutParams.gravity = Gravity.CENTER;
                bypass.setLayoutParams(layoutParams);
//                if (mainActivity.useTheme)
//                    mainActivity.skinEngine.toggle(bypass, false);
            }

            if (isAtom) {
                Button fileChooser = new Button(mainActivity);
                fileChooser.setText("Load file");
                if (mainActivity.useTheme)
                    mainActivity.skinEngine.button(fileChooser, SkinEngine.Resize.None, 0);

                LinearLayout layout_button = new LinearLayout(mainActivity);
                linearLayout.addView(layout_button);
                layout_button.addView(fileChooser);

                TextView textView_button = new TextView(mainActivity);
                textView_button.setText("Load from file or import from zip");
                textView_button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                textView_button.setEnabled(false);

                layout_button.setOrientation(LinearLayout.VERTICAL);
                layout_button.setGravity(Gravity.CENTER);
                fileChooser.setGravity(Gravity.CENTER);
                layout_button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                slider.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);

                int finalI1 = i;
                fileChooser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent_upload = new Intent();
                        intent_upload.setType("application/json");
                        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension("json");
                        String [] mimeTypes = {
                                "*/*",
                                "text/*",
                                "audio/*",
                                "application/x-zip",
                                "application/ld-json",
                                "application/json",
                                mimetype
                        };


                        intent_upload.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                        intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                        intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        int requestCode = 10000 + holder.getLayoutPosition() * 100 + finalI1;
                        mainActivity.startActivityForResult(intent_upload,requestCode);
                    }
                });

                String dir = context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + pluginName + "/";

                DocumentFile root = DocumentFile.fromFile(new File(dir));
                DocumentFile [] files = root.listFiles() ;
                ArrayList <String> models = new ArrayList<>();
                models.add("");
                for (DocumentFile file: files) {
                    Log.d(TAG, String.format ("%s: %s", file.getName(), file.getUri()));
                    models.add(file.getName());
                }

                Spinner spinner_f = new Spinner(context);
                holder.atomSpinners.put(i, spinner_f);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, models);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner_f.setAdapter(adapter);

                LinearLayout hz = new LinearLayout(mainActivity);
                holder.modelSpinnerLayout = hz ;
                prev = new Button(context);
                next = new Button(context);

                prev.setText("<");
                next.setText(">");

                Spinner finalSpinner = spinner_f;
                prev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = finalSpinner.getSelectedItemPosition();
                        if (selected > 0)
                            finalSpinner.setSelection(selected - 1);
                    }
                });

                Spinner finalSpinner1 = spinner_f;
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = finalSpinner1.getSelectedItemPosition();
                        if (selected < finalSpinner1.getAdapter().getCount() - 1)
                            finalSpinner1.setSelection(selected + 1);
                    }
                });

                Button manage = new Button (mainActivity);
                manage.setText("⚙️");
                layout.addView(hz);
//                layout.addView(textView);
                hz.addView(prev);
                hz.addView(spinner_f);
                hz.addView(manage);
                hz.addView(next);

                manage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainActivity.manageNAMModels(finalSpinner, dir);
                    }
                });

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                next.setLayoutParams(layoutParams);
                prev.setLayoutParams(layoutParams);
                manage.setLayoutParams(layoutParams);

                prev.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                manage.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                next.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));

                LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                hz.setLayoutParams(l3);

                LinearLayout.LayoutParams spinnerLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                spinnerLayoutParams.setMargins(0,20,0,20);
                spinner_f.setLayoutParams(spinnerLayoutParams);

//                spinner.setSelection(0);

                int finalI2 = i;
                spinner_f.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int _position, long id) {
                        String m = finalSpinner.getAdapter().getItem(_position).toString();

                        Uri ri = Uri.parse("file://" + dir + m);
                        /*
                        if (hasFilePort) {
//                            AudioEngine.setFilePortValue(holder.getAdapterPosition(), ri.getPath());
                            AudioDecoder audioDecoder = new AudioDecoder(mainActivity);
                            try {
                                int samplerate = AudioEngine.getSampleRate() ;
                                if (samplerate < 44100 )
                                    samplerate = 48000 ;
                                String p = ri.getPath() ;
                                Log.d(TAG, "onItemSelected: loading file " + ri.toString());
                                float [] samples = audioDecoder.decode(ri, null, samplerate);
                                AudioEngine.setPluginBuffer(samples, holder.getAdapterPosition());
//                                Log.d(TAG, String.format ("[decoder]: %d", samples.length));
                            } catch (IOException e) {
                                MainActivity.toast(e.getMessage());
                                Log.e(TAG, "onActivityResult: ", e);
                            }

                        }
                        else */
                        {
//                            String s = mainActivity.getFileContent(ri);
//                            Log.d(TAG, String.format("[content]: %s", s));
//                            AudioEngine.setPluginFilename(s, holder.getAdapterPosition());
                            Log.d(TAG, String.format ("sending filename: %s", ri.getPath()));
                            AudioEngine.setAtomPort(holder.getAdapterPosition(), finalI2, ri.getPath());
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                if (selectedModels.containsKey(position)) {
                    if (spinner_f.getAdapter().getCount() > position)
                        spinner_f.setSelection(selectedModels.get(position));
                    else
                        MainActivity.toast("Unable to load preset properly: a user loaded json file is missing");

                    selectedModels.remove(position);
                }
            }

            layout.addView(slider);
            holder.sliders.add(slider);
            /*
            if (mainActivity.rdf.has (String.valueOf(plugins.get(position)))) {
                Log.d(TAG, "onBindViewHolder: loading spinner for " + String.valueOf(plugins.get(position)));
                List<String> list = new ArrayList<>();
                JSONObject pluginRDF = null;
                try {
                     String pluginRDFString = mainActivity.rdf.getString(String.valueOf(plugins.get(position)));
                     pluginRDF = new JSONObject(pluginRDFString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for(int x = 0; x<pluginRDF.names().length(); x++){
                    String name = null ;
                    try {
                        name = pluginRDF.names().getString(x);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (name != null)
                        list.add(name);
                }


                ArrayAdapter<String> categoriesDataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list);
                categoriesDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(categoriesDataAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        editText.setText(String.valueOf(i));

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                if (list.size() > 0)
                    layout.addView(spinner);
                else {
                    layout.addView(slider);
                    holder.sliders.add(slider);
                }
            } else {
                layout.addView(slider);
                holder.sliders.add(slider);
            }
             */


            editText.setMaxLines(1);
            editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });

            slider.setValueFrom(vals [1]);
            slider.setValueTo(vals [2]);
            // aaaah
            /*
            if (vals [2] == 0 || vals [2] == vals [0])
                slider.setValueTo(vals [2] + 10);

             */

//            Log.d (TAG, String.valueOf(vals [1]) + " " + vals [2]) ;
            boolean valueSet = false ;
            if (presetValue != -1) {
                if (presetValue < vals [1] || presetValue > vals [2]) {
                    Log.e(TAG, string + ": preset value" + presetValue + " < than min " + vals [1] + " or > max " + vals [2]);

                } else {
                    slider.setValue(presetValue);
                    editText.setText(String.valueOf(presetValue));
                    valueSet = true;
                }
            }
            if (vals [0] < vals [1] && ! valueSet) {
                Log.e(TAG, string + ": default value " + vals [0] + " < than min " + vals [1]);
                slider.setValue (vals [1]);
                editText.setText(String.valueOf(vals [1]));

            } else if (vals [0] > vals [2] && !valueSet) {
                Log.e(TAG, string + ": default value " + vals [0] + " > than max " + vals [2]);
                slider.setValue (vals [2]);
                editText.setText(String.valueOf(vals [2]));
            } else if (!valueSet){
                slider.setValue(vals[0]);
                editText.setText(String.valueOf(vals[0]));
            }

            slider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            LinearLayout.LayoutParams spinnerLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            spinnerLayoutParams.setMargins(0,20,0,20);
            spinner.setLayoutParams(spinnerLayoutParams);
            spinner.setSelection((int) slider.getValue());

            /*
            Log.d(TAG, "onBindViewHolder: " +
                    String.format("[%s] %f",
                            string,
                            slider.getValue()));

             */
            if (slider.getValue() == 1f) {
                bypass.setChecked(true);
            } else
                bypass.setChecked(false);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    editText.setText(String.valueOf(i));
                    slider.setValue(Float.valueOf(i));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            int finalI4 = i;
//            slider.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    mainActivity.setMidiControl(v, position, finalI4, MIDIControl.Type.SLIDER, MIDIControl.Scope.PLUGIN);
//                    return false;
//                }
//            });

            slider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                    editText.setText(String.valueOf(value));
                }
            });

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int _pos = holder.getAdapterPosition();
                    Log.d (TAG, "Deleting plugin at position: " + _pos);
                    AudioEngine.deletePlugin(_pos);
                    linearLayout.removeAllViews();
                    deleteItem(_pos);
                }
            });

            holder.moveUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    moveItemUp(holder.getAdapterPosition());
                }
            });

            holder.moveDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    moveItemDown(holder.getAdapterPosition());
                }
            });

            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            layout.addView(editText);

            int finalI = i;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int __i, int i1, int i2) {
                    float value = Float.parseFloat (charSequence.toString()) ;
                    final int _position = holder.getAdapterPosition(); ;
                    final int _i = finalI;
//                    Log.d(TAG, "Changing plugin control " + string + " [" + _position + " : " + _i + "] to " + value) ;
                    AudioEngine.setPluginControl(_position, _i, value);
                    mainActivity.saveActivePreset();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });


            MIDIControl midiControl = null;
            for (int r = 0 ; r < mainActivity.midiControls.size(); r ++) {
                midiControl = mainActivity.midiControls.get(r) ;
                if (midiControl.scope == MIDIControl.Scope.PLUGIN && midiControl.plugin == position && midiControl.pluginControl == i) {
                    Log.i(TAG, "onBindViewHolder: found midi control " + position + ' ' + i);
                    try {
                        Log.i(TAG, "onBindViewHolder: " + midiControl.get());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                midiControl = null ;
            }

            if (midiControl != null) {
                Log.d(TAG, "onBindViewHolder: midicontrol add slider " + position + ' ' + i);
                midiControl.view = slider;
                midiControl.type = MIDIControl.Type.SLIDER;
            }

            if (mainActivity.useTheme && mainActivity.skinEngine.hasKnob() && holder.pluginsHasKnobs) {
                if (! isSpinner && ! isBypass && ! isAtom) {
                    int row = 0, knobType = 3, knobPos = i ;

                    for (Iterator<String> it = knobsConfig.keys(); it.hasNext(); ) {
                        JSONArray arrayList ;
                        String key = it.next();
                        try {
                            arrayList = knobsConfig.getJSONArray(key);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            break ;
                        }

                        /*
                        Log.d(TAG, "onBindViewHolder: " + String.format (
                                "row %s: %s", key, arrayList.toString()
                        ));

                         */

                        if (knobPos >= arrayList.length()) {
                            row ++ ;
                            /*
                            Log.d(TAG, "onBindViewHolder: " + String.format(
                                    "knobpos %d > row length %d = %d",
                                    knobPos, arrayList.length(), knobPos - arrayList.length()
                            ));

                             */
                            knobPos = knobPos - arrayList.length() ;
//                            Log.d(TAG, "onBindViewHolder: knobpos truncated to " + knobPos);
                            continue;
                        }

                        try {
                            knobType = arrayList.getInt(knobPos);
                        } catch (JSONException e) {
                            Log.wtf(TAG, "onBindViewHolder: unable to parse knob type for control " + i + ", row: " + row, e);
                        }

                        break ;
                    }

                    if (row > knobslayer) {
                        knobsLayout = new LinearLayout(mainActivity);
                        linearLayout.addView(knobsLayout);
                        knobslayer ++ ;
                    }

                    knobsLayout.setOrientation(LinearLayout.HORIZONTAL);
//                    knobsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    LinearLayout.LayoutParams lpk = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lpk.setMargins(0, 10, 0, 10);
                    lpk.gravity = Gravity.CENTER;

                    knobsLayout.setLayoutParams(lpk);

                    RotarySeekbar rotarySeekbar = new RotarySeekbar(mainActivity);
                    if (midiControl != null) {
                        Log.d(TAG, "onBindViewHolder: midicontrol add knob " + position + ' ' + i);
                        midiControl.view = rotarySeekbar;
                        midiControl.type = MIDIControl.Type.KNOB;
                    }

                    int finalI3 = i;
                    rotarySeekbar.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Log.i(TAG, "onLongClick: rotary " + position + ' ' + finalI3);
                            mainActivity.setMidiControl(v, position, finalI3, MIDIControl.Type.KNOB, MIDIControl.Scope.PLUGIN);
                            return false;
                        }
                    });

                    TextView label = new TextView(mainActivity),
                            display = new TextView(mainActivity);

                    rotarySeekbar.setId(100 * position + i);
                    label.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            mainActivity.setMidiControl(rotarySeekbar, position, finalI3, MIDIControl.Type.KNOB, MIDIControl.Scope.PLUGIN);
                            return false;
                        }
                    });

                    mainActivity.skinEngine.cardText(label);
                    mainActivity.skinEngine.cardText(display);

                    Typeface font = Typeface.createFromAsset(mainActivity.getAssets(), "start.ttf");

                    display.setTypeface(font);
//                    label.setTypeface(font);
                    LinearLayout layoutRotary = new LinearLayout(mainActivity);
                    layoutRotary.setOrientation(LinearLayout.VERTICAL);
                    rotarySeekbar.setMinValue(slider.getValueFrom());
                    rotarySeekbar.setMaxValue(slider.getValueTo());
                    rotarySeekbar.setValue(slider.getValue());

                    label.setText(string);
//                    label.setMaxLines(3);
//                    label.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
//                    label.setElegantTextHeight(true);
//                    label.setSingleLine(false);
                    label.setPadding(10, 10, 10, 10);
                    display.setPadding(10, 10, 10, 10);
                    LinearLayout.LayoutParams layoutParamsL = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
                    layoutParamsL.setMargins(0, 10, 0, 30);
                    layoutParamsL.gravity = Gravity.CENTER;
                    label.setLayoutParams(layoutParamsL);
                    label.setGravity(Gravity.CENTER);

                    mainActivity.skinEngine.rotary(rotarySeekbar, knobType, slider.getValueFrom(), slider.getValueTo(), slider.getValue());
                    knobsLayout.addView(layoutRotary);
                    /*
                    Log.d(TAG, "onBindViewHolder: " + String.format(
                            "Setting size for knob type %d", knobType
                    ));

                     */

                    LinearLayout.LayoutParams layoutParams ;
                    int w = 0;
                    switch (knobType) {
                        case 1:
                            w = (int) (180 * mainActivity.skinEngine.scaleFactor);
                            layoutParams = new LinearLayout.LayoutParams(w,w) ;
                            layoutParams.setMargins(2, 0, 2, 0);
                            label.setTextSize(10);
                            display.setTextSize(6);
                            break ;
                        case 2:
                            w = (int) (220 * mainActivity.skinEngine.scaleFactor);
                            layoutParams = new LinearLayout.LayoutParams(w,w) ;
                            layoutParams.setMargins(10, 0, 10, 0);
                            label.setTextSize(12);
                            display.setTextSize(8);
                            break ;
                        case 3:
                        default:
                            w = (int) (300 * mainActivity.skinEngine.scaleFactor);
                            layoutParams = new LinearLayout.LayoutParams(w,w) ;
                            layoutParams.setMargins(20, 0, 20, 0);
                            label.setTextSize(13);
                            display.setTextSize(10);
                            break ;
                    }

                    layoutParams.gravity = Gravity.CENTER;
                    rotarySeekbar.setLayoutParams(layoutParams);
                    rotarySeekbar.setShowValue(false);

                    display.setText (String.valueOf(slider.getValue()));
                    display.setGravity(Gravity.CENTER);
                    rotarySeekbar.setOnValueChangedListener(new RotarySeekbar.OnValueChangedListener() {
                        @Override
                        public void onValueChanged(RotarySeekbar sourceSeekbar, float value) {
                            slider.setValue(rotarySeekbar.getValue());
                            display.setText(String.format("%.2f", rotarySeekbar.getValue()));
                            linearLayout.requestDisallowInterceptTouchEvent(true);

                        }
                    });

                    rotarySeekbar.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            slider.setValue(rotarySeekbar.getValue());
                            display.setText(String.format("%.2f", rotarySeekbar.getValue()));
                            linearLayout.requestDisallowInterceptTouchEvent(true);
                            return false;
                        }


                    });

//                    rotarySeekbar.parentLayout = linearLayout;
//                    LinearLayout.LayoutParams layoutParamsContainer = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                    layoutParamsContainer.gravity = Gravity.CENTER;
//                    knobsLayout.setLayoutParams(layoutParamsContainer);

                    layout.setVisibility(View.GONE);
                    textView.setVisibility(View.GONE);

                    layoutRotary.addView(display);
                    layoutRotary.addView(rotarySeekbar);
                    layoutRotary.addView(label);

                }
            }

            if (isBypass && midiControl != null) {
                midiControl.view = bypass;
                midiControl.type = MIDIControl.Type.TOGGLE;
            }
        }

        Button fileChooser = null;
        if (pluginName.equals("AIDA-X") || pluginName .equals( "Looper") || pluginName.equals("Neural Amp Modeler") || pluginName.equals("TAP IR")|| pluginName.equals("SWH Impulse convolver") || pluginName.equals("Simple IR Loader")) {
            if (audioFiles.containsKey(position) && pluginName.equals("Looper") || pluginName.equals("SWH Impulse convolver")|| pluginName.equals("Simple IR Loader")) {
                holder.audioFile = audioFiles.get(position);
                if (holder.audioFile != null) {
                    AudioDecoder audioDecoder = new AudioDecoder(mainActivity);
                    int samplerate = AudioEngine.getSampleRate();
                    if (samplerate < 44100 /*aaaaaaaarghhh*/)
                        samplerate = 48000;
                    float[] samples = new float[0];
                    try {
                        samples = audioDecoder.decode(Uri.parse(holder.audioFile), null, samplerate);
                    } catch (IOException e) {
//                        throw new RuntimeException(e);
                        Log.e(TAG, "onBindViewHolder: cannot load audio file for plugin " + pluginName, e);
                        mainActivity.toast (e.getMessage());
                    }


                    if (samples != null)
                        AudioEngine.setPluginBuffer(samples, position);
                }

                audioFiles.remove(position);
            }

            fileChooser = new Button(mainActivity);
            fileChooser.setText("Load file");
            if (mainActivity.useTheme)
                mainActivity.skinEngine.button(fileChooser, SkinEngine.Resize.None, 0);

            LinearLayout layout = new LinearLayout(mainActivity);
            linearLayout.addView(layout);
            layout.addView(fileChooser);

            TextView textView = new TextView(mainActivity);
            textView.setText("Load from file or import from zip");
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setEnabled(false);

            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            fileChooser.setGravity(Gravity.CENTER);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (pluginName.equals("Neural Amp Modeler")|| pluginName.equals("AIDA-X"))
                holder.hasFileSpinner = true ;
            fileChooser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent_upload = new Intent();
                    if (pluginName.equals("Looper")|| pluginName.equals("SWH Impulse convolver")||pluginName.equals("Simple IR Loader"))
                        intent_upload.setType("audio/*");
                    else {
                        intent_upload.setType("application/zip");
                        String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip");
                        String [] mimeTypes = {
                                "text/*",
                                "application/x-zip",
                                "application/ld-json",
                                "application/json",
                                "application/octet-stream",
                                "application/x-compress",
                                "application/x-compressed",
                                "application/x-zip-compressed",
                                "multipart/x-zip",
                                mimetype
                        };


                        intent_upload.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                    }
                    intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    int requestCode = 5000 + holder.getLayoutPosition();
                    mainActivity.startActivityForResult(intent_upload,requestCode);
                }
            });

            if (pluginName.equals("Neural Amp Modeler") || pluginName.equals("AIDA-X")) {
                String dir = context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS) + "/" + pluginName + "/";

                DocumentFile root = DocumentFile.fromFile(new File(dir));
                DocumentFile [] files = root.listFiles() ;
                ArrayList <String> models = new ArrayList<>();
                for (DocumentFile file: files) {
                    Log.d(TAG, String.format ("%s: %s", file.getName(), file.getUri()));
                    models.add(file.getName());
                }

                Spinner spinner = new Spinner(context);
                holder.modelSpinner = spinner;
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, models);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                LinearLayout hz = new LinearLayout(mainActivity);
                holder.modelSpinnerLayout = hz ;
                Button prev = new Button(context);
                Button next = new Button(context);

                prev.setText("<");
                next.setText(">");

                prev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = spinner.getSelectedItemPosition();
                        if (selected > 0)
                            spinner.setSelection(selected - 1);
                    }
                });

                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selected = spinner.getSelectedItemPosition();
                        if (selected < spinner.getAdapter().getCount() - 1)
                            spinner.setSelection(selected + 1);
                    }
                });

                Button manage = new Button (mainActivity);
                manage.setText("⚙️");
                layout.addView(hz);
//                layout.addView(textView);
                hz.addView(prev);
                hz.addView(spinner);
                hz.addView(manage);
                hz.addView(next);

                manage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainActivity.manageNAMModels(spinner, dir);
                    }
                });

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.WRAP_CONTENT);
                next.setLayoutParams(layoutParams);
                prev.setLayoutParams(layoutParams);
                manage.setLayoutParams(layoutParams);

                prev.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                manage.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                next.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));

                LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                hz.setLayoutParams(l3);

                LinearLayout.LayoutParams spinnerLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                spinnerLayoutParams.setMargins(0,20,0,20);
                spinner.setLayoutParams(spinnerLayoutParams);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int _position, long id) {
                        String m = spinner.getAdapter().getItem(_position).toString();

                        Uri ri = Uri.parse("file://" + dir + m);
                        /*
                        if (hasFilePort) {
//                            AudioEngine.setFilePortValue(holder.getAdapterPosition(), ri.getPath());
                            AudioDecoder audioDecoder = new AudioDecoder(mainActivity);
                            try {
                                int samplerate = AudioEngine.getSampleRate() ;
                                if (samplerate < 44100 )
                                    samplerate = 48000 ;
                                String p = ri.getPath() ;
                                Log.d(TAG, "onItemSelected: loading file " + ri.toString());
                                float [] samples = audioDecoder.decode(ri, null, samplerate);
                                AudioEngine.setPluginBuffer(samples, holder.getAdapterPosition());
//                                Log.d(TAG, String.format ("[decoder]: %d", samples.length));
                            } catch (IOException e) {
                                MainActivity.toast(e.getMessage());
                                Log.e(TAG, "onActivityResult: ", e);
                            }

                        }
                        else */
                        {
                            String s = mainActivity.getFileContent(ri);
//                            Log.d(TAG, String.format("[content]: %s", s));
                            AudioEngine.setPluginFilename(s, holder.getAdapterPosition());
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                if (selectedModels.containsKey(position)) {
                    if (spinner.getAdapter().getCount() > position)
                        spinner.setSelection(selectedModels.get(position));
                    else
                        MainActivity.toast("Unable to load preset properly: a user loaded json file is missing");

                    selectedModels.remove(position);
                }
            }
        }

        if (mainActivity.useTheme) {
//            mainActivity.skinEngine.card (holder.root);
            mainActivity.skinEngine.drawableLeft(holder.moveDownButton, "icons", "down", SkinEngine.Resize.Width, .75f);
            mainActivity.skinEngine.drawableLeft(holder.moveUpButton, "icons", "up", SkinEngine.Resize.Width, .75f);
            mainActivity.skinEngine.drawableLeft(holder.deleteButton, "icons", "delete", SkinEngine.Resize.Width, .75f);

            holder.deleteButton.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
            holder.moveDownButton.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
            holder.moveUpButton.setBackgroundColor(mainActivity.getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
            holder.root.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card (holder.root);

                }
            });


        }

    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ArrayList <Slider> sliders ;
        LinearLayout linearLayout ;
        LinearLayout root ;
        TextView pluginName ;
        SwitchMaterial switchMaterial ;
        HashMap<Integer, Spinner> atomSpinners = new HashMap<>();
        ToggleButton toggleButton ;
        int selectedModel = -1 ;
        String audioFile = null ;
        Spinner modelSpinner ;
        LinearLayout modelSpinnerLayout ;
        boolean hasFileSpinner ;
        MaterialButton deleteButton, moveUpButton, moveDownButton ;
        boolean pluginsHasKnobs = false ;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sliders = new ArrayList<>();
            hasFileSpinner = false;
            root = (LinearLayout) itemView ;
            linearLayout = (LinearLayout) itemView ;
            linearLayout = (LinearLayout) linearLayout.getChildAt(0);
            root = (LinearLayout) linearLayout.getChildAt(0);
            linearLayout = (LinearLayout) linearLayout.getChildAt(0);
            // this one is the plugin holder we want
            linearLayout = (LinearLayout) linearLayout.getChildAt(0);

            LinearLayout optionsBox = (LinearLayout) linearLayout.getChildAt(2);
            moveUpButton = (MaterialButton) optionsBox.getChildAt(0) ;
            moveDownButton = (MaterialButton) optionsBox.getChildAt(1) ;
            deleteButton = (MaterialButton) optionsBox.getChildAt(3) ;

            LinearLayout l = (LinearLayout) linearLayout.getChildAt(0);
            pluginName = (TextView) l.getChildAt(0);
            switchMaterial = (SwitchMaterial) l.getChildAt(1);
            toggleButton = (ToggleButton) l.getChildAt(2);
            if (mainActivity.useTheme) {
                switchMaterial.setVisibility(View.GONE);
            } else {
                toggleButton.setVisibility(View.GONE);
            }

            linearLayout = (LinearLayout) linearLayout.getChildAt(1);
        }

        public TextView getTextView () {
            return pluginName ;
        }

        public LinearLayout getLinearLayout () {
            return linearLayout ;
        }
    }

    void setColor (int color) {
        primaryColor = color ;
    }

    void addItem(int pluginID, int index) {
        plugins.add(pluginID);
        notifyItemInserted(index);
    }

    void setSelectedModel (int plugin, int model) {
        selectedModels.put(plugin, model);
    }

    void setAudioFiles (int plugin, String filename) {
        audioFiles.put(plugin, filename);
    }

    void deleteAll () {
        for (int i = 0 ;i  < plugins.size();i++) {
            deleteItem(i);
        }
    }

    void deleteItem(int index) {
        if (index > plugins.size()) {
            Log.w(TAG, "deleteItem: index > plugins size", null);
            return ;
        }

        plugins.remove(index);
        notifyItemRemoved(index);
//        notifyItemRangeChanged(0, getItemCount());
    }

    void moveItemUp (int index) {
        if (index == 0) {
            Toast.makeText(context,
                    "Plugin is already at the top",
                    Toast.LENGTH_LONG)
                    .show();
            return ;
        }

        notifyItemMoved(index, index - 1);
        AudioEngine.movePluginUp(index);

    }

    void moveItemDown (int index) {
        if (index >= getItemCount() - 1) {
            Toast.makeText(context,
                    "Plugin is already at the end",
                    Toast.LENGTH_LONG)
                    .show();
            return ;
        }

        notifyItemMoved(index, index + 1);
        AudioEngine.movePluginDown(index);
    }

    void reset () {
        notifyItemRangeRemoved(0, plugins.size());
        holders.clear();
        plugins.clear();
    }

    @Override
    public long getItemId(int position) {
        return plugins.get(position);
    }
}