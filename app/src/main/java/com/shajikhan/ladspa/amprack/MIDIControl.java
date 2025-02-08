package com.shajikhan.ladspa.amprack;

import static com.shajikhan.ladspa.amprack.MainActivity.mainActivity;

import android.util.Log;
import android.view.View;

import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MIDIControl {
    private final String TAG = getClass().getName();

    enum Type {
        SLIDER,
        KNOB,
        TOGGLE
    } ;

    enum Scope {
        GLOBAL,
        PLUGIN
    } ;

    View view ;
    int plugin ;
    int pluginControl ;
    int control ;
    Type type ;
    Scope scope ;

    int channel ;
    int program ;

    String getID () {
        View v = view ;
        if (v == null)
            return "" ;
        if (scope == Scope.PLUGIN)
            return String.valueOf(v.getId());
        else {
            try {
                return (v.getId() == View.NO_ID) ? "" :
                        v.getResources().getResourceName(v.getId()).split(":id/")[1];
            } catch (Exception e) {
                Log.e(TAG, "getID: ", e);
                return "";
            }
        }
    }

    JSONObject get () throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("view", getID());
        jsonObject.put("plugin", plugin);
        jsonObject.put("control", control);
        jsonObject.put("type", type);
        jsonObject.put("channel", channel);
        jsonObject.put("control", control);
        jsonObject.put("pluginControl", pluginControl);
        jsonObject.put("scope", scope);

        return jsonObject;
    }

    @Override
    public String toString() {
        return "MIDIControl{" +
                "TAG='" + TAG + '\'' +
                ", view=" + view.getResources().getIdentifier(getID(), "id", mainActivity.getPackageName()) +
                ", plugin=" + plugin +
                ", control=" + control +
                ", pluginControl=" + pluginControl +
                ", type=" + type +
                ", scope=" + scope +
                ", channel=" + channel +
                ", program=" + program +
                '}';
    }

    void process (int data) {
        float value = 0f ;
        switch (type) {
            case SLIDER:
                Slider slider = (Slider) view;
                value = ((slider.getValueFrom() - slider.getValueTo()) * (data / 127)) + slider.getValueFrom(); 
                Log.i(TAG, String.format("plugin %d control %d value %f", plugin, control, value));
                slider.setValue(value);
                break ;
            case KNOB:
                RotarySeekbar rotarySeekbar = (RotarySeekbar) view;
                value = ((rotarySeekbar.getMaxValue() - rotarySeekbar.getMinValue()) * (data / 127)) + rotarySeekbar.getMinValue() ;
                Log.i(TAG, String.format("plugin %d control %d value %f", plugin, control, value));
                rotarySeekbar.setValue(value);
                break ;
            case TOGGLE:
                Log.i(TAG, "process: view click!");
                view.performClick();
                break ;
            default:
                Log.w(TAG, "process: unknown control type " + type);
        }
    }
}
