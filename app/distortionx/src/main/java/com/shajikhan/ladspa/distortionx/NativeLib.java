package com.shajikhan.ladspa.distortionx;

public class NativeLib {

    // Used to load the 'distortionx' library on application startup.
    static {
        System.loadLibrary("distortionx");
    }

    /**
     * A native method that is implemented by the 'distortionx' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}