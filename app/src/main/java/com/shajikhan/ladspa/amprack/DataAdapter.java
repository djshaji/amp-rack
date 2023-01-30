package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    int totalItems = 0;
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
        int numControls = AudioEngine.getPluginControls(position);
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
            boolean isSpinner = false ;
            Log.d(TAG, "onBindViewHolder: " + pluginName);
            if (mainActivity.ampModels.has(pluginName)) {
                JSONObject control ;
                ArrayList <String> models = new ArrayList<>();
                try {
                    control = mainActivity.ampModels.getJSONObject(pluginName) ;
                    if (control.has(String.valueOf(i))) {
                        isSpinner = true ;
                        JSONArray modelsData = control.getJSONArray(String.valueOf(i));
                        for (int x_ = 0 ; x_ < modelsData.length() ; x_++) {
                            models.add(modelsData.getString(x_));
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
        }
    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ArrayList <Slider> sliders ;
        LinearLayout linearLayout ;
        TextView pluginName ;
        SwitchMaterial switchMaterial ;
        MaterialButton deleteButton, moveUpButton, moveDownButton ;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sliders = new ArrayList<>();
            linearLayout = (LinearLayout) itemView ;
            linearLayout = (LinearLayout) linearLayout.getChildAt(0);
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
    }

    void moveItemDown (int index) {
        if (index == getItemCount()) {
            Toast.makeText(context,
                    "Plugin is already at the end",
                    Toast.LENGTH_LONG)
                    .show();
            return ;
        }

        notifyItemMoved(index, index + 1);
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