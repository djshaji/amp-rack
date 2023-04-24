package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.graphics.Typeface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    int totalItems = 0;
    int xOffset = 0, yOffset = 0 ;
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    int primaryColor = com.google.android.material.R.color.design_default_color_primary;
    ArrayList <Integer> plugins = new ArrayList<>();
    ArrayList <ViewHolder> holders = new ArrayList<>();
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
    public void onBindViewHolder(@NonNull DataAdapter.ViewHolder holder, int position) {
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
        holder.getTextView().setText(pluginName);
        if (mainActivity.useTheme)
            mainActivity.skinEngine.cardText(holder.getTextView());
        int numControls = AudioEngine.getPluginControls(position);

        LinearLayout knobsLayout = new LinearLayout (mainActivity);
        knobsLayout.setOrientation(LinearLayout.HORIZONTAL);
//        knobsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(knobsLayout);
        int knobslayer = 0;
        boolean pluginsHasKnobs = true;
        JSONObject knobsConfig = null;
        try {
            knobsConfig = mainActivity.knobsLayout.getJSONObject(String.valueOf(numControls));
        } catch (JSONException e) {
            pluginsHasKnobs = false ;
            Log.e(TAG, "onBindViewHolder: no json config for knobs: " + numControls, e);
        }

        for (int i = 0 ; i < numControls ; i ++) {
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
            Spinner spinner = new Spinner(context);
            SeekBar seekBar = new SeekBar(context);
            boolean isSpinner = false ;
            boolean isBypass = false ;
            if (string != null) {
                Log.d(TAG, "onBindViewHolder: control name: " + string +
                        " -> " + string.equalsIgnoreCase("bypass"));
                isBypass = string.equalsIgnoreCase("bypass");
            }

            if (mainActivity.useTheme) {
                mainActivity.skinEngine.cardText(textView);

                mainActivity.skinEngine.slider(slider);
                holder.toggleButton.setChecked(true);
                mainActivity.skinEngine.toggle(holder.toggleButton, true);
                holder.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        holder.switchMaterial.setChecked(isChecked);
                        mainActivity.skinEngine.toggle(holder.toggleButton, isChecked);
                    }
                });

            }

            Log.d(TAG, "onBindViewHolder: loading plugin -> " + pluginName );
            if (mainActivity.ampModels.has(pluginName)) {
                Log.d(TAG, "onBindViewHolder: found amp model " + pluginName);
                JSONObject control ;
                ArrayList <String> models = new ArrayList<>();
                try {
                    control = mainActivity.ampModels.getJSONObject(pluginName) ;
                    if (control.has(String.valueOf(i))) {
                        Log.d(TAG, "onBindViewHolder: found control " + i);
                        isSpinner = true ;
                        JSONArray modelsData = control.getJSONArray(String.valueOf(i));
                        for (int x_ = 0 ; x_ < modelsData.length() ; x_++) {
                            Log.d(TAG, "onBindViewHolder: " + modelsData.getString(x_));
                            models.add(modelsData.getString(x_));
                        }

                    } else {
                        Log.d(TAG, "onBindViewHolder: no control for " + i);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "onBindViewHolder: error parsing amp model " + pluginName, e);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                        android.R.layout.simple_spinner_item, models);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
//                spinner.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
            }


            if (isSpinner) {
                layout.addView(spinner);
                slider.setVisibility(View.GONE);
                editText.setVisibility(View.GONE);
            }

            ToggleButton bypass = new ToggleButton(mainActivity);
            if (isBypass) {
                Log.d(TAG, "onBindViewHolder: turning on bypass switch");
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

            Log.d(TAG, "onBindViewHolder: " +
                    String.format("[%s] %f",
                            string,
                            slider.getValue()));
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
                    Log.d(TAG, "Changing plugin control " + string + " [" + _position + " : " + _i + "] to " + value) ;
                    AudioEngine.setPluginControl(_position, _i, value);
                    mainActivity.saveActivePreset();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            if (mainActivity.useTheme && mainActivity.skinEngine.hasKnob() && pluginsHasKnobs) {
                if (! isSpinner && ! isBypass) {
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

                        Log.d(TAG, "onBindViewHolder: " + String.format (
                                "row %s: %s", key, arrayList.toString()
                        ));

                        if (knobPos >= arrayList.length()) {
                            row ++ ;
                            Log.d(TAG, "onBindViewHolder: " + String.format(
                                    "knobpos %d > row length %d = %d",
                                    knobPos, arrayList.length(), knobPos - arrayList.length()
                            ));
                            knobPos = knobPos - arrayList.length() ;
                            Log.d(TAG, "onBindViewHolder: knobpos truncated to " + knobPos);
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
                    TextView label = new TextView(mainActivity),
                            display = new TextView(mainActivity);

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
                    label.setPadding(10, 10, 10, 10);
                    display.setPadding(10, 10, 10, 10);
                    LinearLayout.LayoutParams layoutParamsL = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
                    layoutParamsL.setMargins(0, 10, 0, 30);
                    layoutParamsL.gravity = Gravity.CENTER;
                    label.setLayoutParams(layoutParamsL);
                    label.setGravity(Gravity.CENTER);

                    mainActivity.skinEngine.rotary(rotarySeekbar, knobType, slider.getValueFrom(), slider.getValueTo(), slider.getValue());
                    knobsLayout.addView(layoutRotary);
                    Log.d(TAG, "onBindViewHolder: " + String.format(
                            "Setting size for knob type %d", knobType
                    ));

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
        ToggleButton toggleButton ;
        MaterialButton deleteButton, moveUpButton, moveDownButton ;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sliders = new ArrayList<>();
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