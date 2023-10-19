package com.shajikhan.ladspa.amprack;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

public class AudioEngine {
    static native void loadLibrary (String filename);
    static native void loadPlugins ();
    static native int getSharedLibraries ();
    static native void setLazyLoad (boolean lazyLoad);
    static native String getLibraryName (int library) ;
    static native int getPlugins (int library) ;
    static native int getTotalPlugins ();
    static native String getPluginName (int library, int plugin);
    static native int getPluginUniqueID (int library, int plugin);
    // activePlugins
    static native int getActivePlugins ();
    static native float [] getActivePluginValues (int plugin);
    static native int getPluginControls (int plugin) ;
    static native void setFileName (String fileName);
    static native float [] getPluginControlValues (int plugin, int control) ;
    static native float getPluginPresetValue (int plugin, int control) ;
    static native String getControlName (int plugin, int control) ;
    static native String getActivePluginName (int plugin);

    static native String getRecordingFileName ();
    static native void setLowLatency (boolean lowLatency) ;
    static native void setSampleRate (int sampleRate);

    // return active plugin *ID*
    static native int addPlugin (int library, int plugin) ;
    static native int addPluginLazy (String library, int plugin);
    static native int addPluginLazyLV2(String library, int plugin);
    static native int addPluginByName (String name);
    static native boolean deletePlugin (int plugin) ;
    static native void clearActiveQueue ();
    static native void toggleRecording (boolean state) ;

    static native void setPluginControl (int plugin, int control, float value);
    static native void setPresetValue (int plugin, int control, float value);
    static native int movePlugin (int plugin, int position) ;
    static native int movePluginUp (int plugin) ;
    static native int movePluginDown (int plugin) ;

    static native boolean togglePlugin (int plugin, boolean state) ;
    static native void bypass (boolean state) ;

    static native void setExportFormat (int format);

    static native void debugInfo ();
    static native void setExternalStoragePath (String path) ;
    static native void setRecordingActive (boolean active) ;
    static native void setOpusBitRate (int bitrate);

    static native boolean create () ;
    static native boolean wasLowLatency ();
    static native boolean isAAudioRecommended () ;
    static native boolean setAPI(int apiType);
    static native boolean setEffectOn(boolean isEffectOn);
    static native void setRecordingDeviceId(int deviceId);
    static native void setPlaybackDeviceId(int deviceId);
    static native void delete();
    static native void native_setDefaultStreamValues(int defaultSampleRate, int defaultFramesPerBurst);

    static native void setLibraryPath (String path);
    static native void setInputVolume (float volume);
    static native void setOutputVolume (float volume);
    static native void toggleMixer (boolean toggle);
    static native void printActiveChain ();

    static native void testLV2 ();

    static void setDefaultStreamValues(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            int defaultSampleRate = Integer.parseInt(sampleRateStr);
            String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);

            native_setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst);
        }
    }

    static ProgressDialog progress ;
    static void showProgress (Context context) {
        progress = new ProgressDialog(context);
        progress.setTitle("Loading");
        progress.setIndeterminate(true);
        progress.setIcon(R.drawable.logo);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
    }

    void setInputMeter (float value) {

    }

    static void hideProgress () {
        progress.hide();
    }

    static void warnLowLatency (Context context) {
        MainActivity.toast(context.getResources().getString(R.string.lowLatencyWarning));
    }

    static native int getSampleRate () ;
}
