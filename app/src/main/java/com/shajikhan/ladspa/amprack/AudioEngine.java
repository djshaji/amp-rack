package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

public class AudioEngine {
    static native int getSharedLibraries ();
    static native String getLibraryName (int library) ;
    static native int getPlugins (int library) ;
    static native String getPluginName (int library, int plugin);
    // activePlugins
    static native int getPluginControls (int plugin) ;
    static native float [] getPluginControlValues (int plugin, int control) ;

    // return active plugin *ID*
    static native int addPlugin (int library, int plugin) ;
    static native boolean deletePlugin (int plugin) ;

    static native void setPluginControl (int plugin, int control, float value);
    static native int movePlugin (int plugin, int position) ;

    static native boolean togglePlugin (int plugin, boolean state) ;

    static native boolean create () ;
    static native boolean isAAudioRecommended () ;
    static native boolean setAPI(int apiType);
    static native boolean setEffectOn(boolean isEffectOn);
    static native void setRecordingDeviceId(int deviceId);
    static native void setPlaybackDeviceId(int deviceId);
    static native void delete();
    static native void native_setDefaultStreamValues(int defaultSampleRate, int defaultFramesPerBurst);

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

}
