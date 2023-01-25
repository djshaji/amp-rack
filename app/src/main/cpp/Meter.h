//
// Created by djshaji on 1/22/23.
//

#ifndef AMP_RACK_METER_H
#define AMP_RACK_METER_H

#define JC_MAX(a,b) (((a)>(b))?(a):(b))
#define JC_MIN(a,b) (((a)<(b))?(a):(b))

#define ALIGN_UP(value,alignment) (((uintptr_t)value + alignment - 1) & -alignment)
#define ALIGN_UP_DOUBLE(p) ALIGN_UP(p,sizeof(double)) // Using double because double should always be very large.

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#ifdef __cplusplus
extern "C" {
#include "vringbuffer.h"
}
#endif

//#include "FileWriter.h"
JNIEnv* getEnv() ;

class Meter {
    typedef struct buffer_t{
        int overruns;
        float pos;
//    float data[];
        float *data;
    } buffer_t;

    typedef struct staticBuffer_t{
        int overruns;
        float pos;
        float data[192];
        bool isInput ;
//    float *data;
    } staticBuffer_t;

    static vringbuffer_t * vringbuffer ;
    static vringbuffer_t * vringbufferOutput ;
    static buffer_t *current_buffer;
    float *empty_buffer;
    static int jack_samplerate ;
    int buffer_size_in_bytes = 192;
    static int block_size ;
    static int bufferUsed  ;
    static int bufferUsedOutput  ;
    static staticBuffer_t buffersOutput [1024] ;
    static staticBuffer_t buffers [1024] ;
    static int MAX_STATIC_BUFFER  ;
    static JavaVM *vm ;

public:
    Meter(JavaVM *pVm);
    static jmethodID setMixerMeter ;
    static jclass mainActivity ;
    static JNIEnv *env;
    static jmethodID setMixerMeterOutput ;
    static jclass mainActivityOutput ;
    static JNIEnv *envOutput;
    static bool enabled ;
    static bool isInput ;
    static float lastTotal ;
    static int autoincrease_callback(vringbuffer_t *vrb, bool first_call, int reading_size, int writing_size);

    int64_t seconds_to_frames(float seconds);

    float frames_to_seconds(int frames);

    int seconds_to_blocks(float seconds);

    static float buffers_to_seconds(int buffers);

    static float blocks_to_seconds(int blocks);

    void *my_calloc(size_t size1, size_t size2);

    int seconds_to_buffers(float seconds);

    int update(float *dataIn, float *dataOut, size_t frames);

    static int updateInput(float *data, size_t frames);

    int updateOutput(float *data, size_t frames);

    static enum vringbuffer_receiver_callback_return_t meter_callback_output (vringbuffer_t *vrb,bool first_time,void *element) {
        if (first_time) {
            envOutput = getEnv();
            if (envOutput == nullptr)
                LOGF("envOutput is null");
            mainActivityOutput = findClassWithEnv(envOutput, "com/shajikhan/ladspa/amprack/MainActivity");
            if (mainActivityOutput == nullptr) {
                HERE
                LOGF("cannot find class mainactivityOutput!");
            }

            setMixerMeterOutput = envOutput->GetStaticMethodID(mainActivityOutput, "setMixerMeterSwitch",
                                                               "(FZ)V");
            if (setMixerMeterOutput == nullptr) {
                LOGF("cannot find method!");
            }

        }

        if (first_time==true) {
            return static_cast<vringbuffer_receiver_callback_return_t>(true);
        }

        staticBuffer_t * sbuffer = (staticBuffer_t * ) element ;
        float total = 0 ;
        float max = 0 ;

        for (int i = 0; i < bufferUsedOutput; i++) {
            for (int j = 0; j < sbuffer[i].pos; j++) {
                if (sbuffer[i].data[j] > max)
                    max = sbuffer[i].data[j];
            }
        }

//        LOGD ("%f", max);
        envOutput->CallStaticVoidMethod(mainActivityOutput, setMixerMeterOutput, (jfloat) max, false);
        bufferUsedOutput = 0;
        return VRB_CALLBACK_USED_BUFFER;

    }

    static enum vringbuffer_receiver_callback_return_t meter_callback (vringbuffer_t *vrb,bool first_time,void *element){
//        IN
        if (first_time) {
            env = getEnv();
            if (env == nullptr)
                LOGF("env is null");
            mainActivity = findClassWithEnv(env, "com/shajikhan/ladspa/amprack/MainActivity");
            if (mainActivity == nullptr) {
                HERE
                LOGF("cannot find class mainactivity!");
            }

            setMixerMeter = env->GetStaticMethodID(mainActivity, "setMixerMeterSwitch",
                                                   "(FZ)V");
            if (setMixerMeter == nullptr) {
                LOGF("cannot find method!");
            }

        }


        if (first_time==true) {
            return static_cast<vringbuffer_receiver_callback_return_t>(true);
        }

        staticBuffer_t * sbuffer = (staticBuffer_t * ) element ;
        float total = 0 ;
        float max = 0 ;

        for (int i = 0; i < bufferUsed; i++) {
            for (int j = 0; j < sbuffer[i].pos; j++) {
                if (sbuffer[i].data[j] > max)
                    max = sbuffer[i].data[j];
            }
        }

        env->CallStaticVoidMethod(mainActivity, setMixerMeter, (jfloat) max, true);
        bufferUsed = 0;

        return VRB_CALLBACK_USED_BUFFER;
    }

    static void process(int frames, const float *data, bool isInput);

    void enable();

    static jclass findClass(const char *name);

    static jclass findClassWithEnv(JNIEnv *env, const char *name);
};

#endif //AMP_RACK_METER_H
