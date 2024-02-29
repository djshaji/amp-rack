package com.shajikhan.ladspa.amprack;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

public class AudioEncoder {
    ArrayList <MediaCodecInfo> codecs ;
    MainActivity mainActivity ;
    String TAG = "Audio Encoder yeah" ;

    AudioEncoder (MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        codecs = new ArrayList<>();
        MediaCodecList supported = new MediaCodecList(MediaCodecList.ALL_CODECS);
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

//                if (!codecInfo.isEncoder()) {
//                    continue;
//                }

            String[] types = codecInfo.getSupportedTypes();
            String feature = "decoder" ;
            if (codecInfo.isEncoder()) {
                feature = "encoder";
                codecs.add(codecInfo);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (codecInfo.isHardwareAccelerated())
                    feature += " hwaccel";
            }
            String typ = "" ;
            for (String s: types)
                typ += s + " ";
            Log.d(TAG, String.format ("found supported codec: %s [%s]", typ, feature));
        }


    }
}
