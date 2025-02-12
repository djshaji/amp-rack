package com.shajikhan.ladspa.amprack;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public class AudioEngine {
    public static native void loadLibrary (String filename);
    public static native void loadPlugins ();
    public static native int getSharedLibraries ();
    public static native void setLazyLoad (boolean lazyLoad);
    public static native String getLibraryName (int library) ;
    public static native int getPlugins (int library) ;
    public static native int getTotalPlugins ();
    public static native String getPluginName (int library, int plugin);
    public static native int getPluginUniqueID (int library, int plugin);
    // activePlugins
    public static native int getActivePlugins ();
    public static native float [] getActivePluginValues (int plugin);
    public static native float getActivePluginValue (int plugin, int control);
    public static native float getActivePluginValueByIndex (int plugin, int control);
    public static native int getActivePluginID (int plugin);
    public static native int getPluginControls (int plugin) ;
    public static native boolean getFilePort (int plugin);
    public static native void setFilePortValue (int plugin, String filename);
    public static native void setFileName (String fileName);
    public static native void setAtomPort (int plugin, int control, String text);
    public static native float [] getPluginControlValues (int plugin, int control) ;
    public static native float getPluginPresetValue (int plugin, int control) ;
    public static native boolean getControlIsLogarithmic (int plugin, int control);
    public static native String getControlName (int plugin, int control) ;
    public static native int getControlType (int plugin, int control);
    public static native String getActivePluginName (int plugin);

    public static native String getRecordingFileName ();
    public static native void setLowLatency (boolean lowLatency) ;
    public static native void setSampleRate (int sampleRate);

    // return active plugin *ID*
    public static native int addPlugin (int library, int plugin) ;
    public static native int addPluginLazy (String library, int plugin);
    public static native int addPluginLazyLV2(String library, int plugin);
    public static native int addPluginByName (String name);
    public static native boolean deletePlugin (int plugin) ;
    public static native void clearActiveQueue ();
    public static native void toggleRecording (boolean state) ;

    public static native void setPluginControl (int plugin, int control, float value);
    public static native void setPluginControlByIndex (int plugin, int control, float value);
    public static native void setPresetValue (int plugin, int control, float value);
    public static native int movePlugin (int plugin, int position) ;
    public static native int movePluginUp (int plugin) ;
    public static native int movePluginDown (int plugin) ;

    public static native boolean togglePlugin (int plugin, boolean state) ;
    public static native boolean getActivePluginEnabled (int plugin);
    public static native void bypass (boolean state) ;
    public static native void pause (boolean state) ;

    public static native void setExportFormat (int format);

    public static native void debugInfo ();
    public static native boolean getTunerEnabled () ;
    public static native void setTunerEnabled (boolean enabled) ;
    public static native void setExternalStoragePath (String path) ;
    public static native void setRecordingActive (boolean active) ;
    public static native void setOpusBitRate (int bitrate);

    public static native boolean create() ;
    public static native boolean wasLowLatency ();
    public static native boolean isAAudioRecommended () ;
    public static native boolean setAPI(int apiType);
    public static native boolean setEffectOn(boolean isEffectOn);
    public static native void popFunction ();
    public static native void setRecordingDeviceId(int deviceId);
    public static native void setPlaybackDeviceId(int deviceId);
    public static native void delete();
    public static native void native_setDefaultStreamValues(int defaultSampleRate, int defaultFramesPerBurst);

    public static native void setLibraryPath (String path);
    public static native void setInputVolume (float volume);
    public static native void setOutputVolume (float volume);
    public static native void toggleMixer (boolean toggle);
    public static native void printActiveChain ();

    public static native void testLV2 ();
    public static native void setPluginBuffer (float  [] data, int plugin);
    public static native void setPluginFilename (String filename, int plugin);
    public static native String tuneLatency ();

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
//        progress.setIcon(R.drawable.logo);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
    }

    void setInputMeter (float value) {

    }

    static void hideProgress () {
        progress.hide();
    }

    static void warnLowLatency (Context context) {
        Log.d("AudioEngine", "warnLowLatency: unable to get low latency");
//        MainActivity.toast(context.getResources().getString(R.string.lowLatencyWarning));
    }

    public static native int getSampleRate () ;
    public static native void toggleVideoRecording (boolean toggle);
    public static native long getTimeStamp () ;
    public static native double getLatency (boolean input) ;
    public static native void setMainActivityClassName (String className);
    public static native void pushToLockFreeBeforeOutputVolumeAaaaaargh (boolean setting) ;
    public static native void setLamePreset (int preset);
    public static native int getActiveEnabledPlugins ();
    public static native int getBufferSizeInFrames (boolean input) ;
    public static native void fixGlitches ();
    public static native void minimizeLatency ();
    public static native void setBufferSizeFactor (float factor);
    public static native void latencyTuner ();
}
