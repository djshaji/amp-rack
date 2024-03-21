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
#include "LockFreeQueue.h"

//#include "FileWriter.h"
JNIEnv* getEnv() ;
#define LOCK_FREE_SIZE 4096

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
    static int attached_thread ;
    static bool engine_running ;

public:
    Meter(JavaVM *pVm);
    std::string lastRecordedFileName ;
    static bool tunerEnabled ;
    static jmethodID setMixerMeter ;
    static jclass mainActivity ;
    static JNIEnv *env;
    static jmethodID setMixerMeterOutput ;
    static jmethodID setTuner ;
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

            setTuner = envOutput->GetStaticMethodID(mainActivityOutput, "setTuner",
                                                               "([F)V");
            if (setMixerMeterOutput == nullptr) {
                LOGF("cannot find method!");
            }

            if (setTuner == nullptr) {
                LOGF("cannot find setTuner method!");
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
                if (sbuffer[i].data[j] > max && sbuffer[i].data[j] > 0.01 && sbuffer[i].data[j] < 1.01) {
                    max = sbuffer[i].data[j];
                }
            }
        }

//        max = sbuffer[0].data[0];

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

        if (tunerEnabled && (tunerIndex >= 1024 * 4)) {
            jfloatArray jfloatArray1 = env->NewFloatArray(1024*4);
            env->SetFloatArrayRegion(jfloatArray1, 0, 1024*4, tunerBuffer);
            env->CallStaticVoidMethod(mainActivity, setTuner, jfloatArray1, false);
            tunerIndex = 0 ;
        }

        staticBuffer_t * sbuffer = (staticBuffer_t * ) element ;
        float total = 0 ;
        float max = 0 ;

        for (int i = 0; i < bufferUsed; i++) {
            for (int j = 0; j < sbuffer[i].pos; j++) {
                if (sbuffer[i].data[j] > max /* && sbuffer[i].data[j] > 0.01 */ && sbuffer[i].data[j] < 1.01)
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

    static float tunerBuffer [1024*4];
    static int tunerIndex;

    static int updateMeterOutput(AudioBuffer * buffer);

    void stop();

    void disable();

    void start();

    static _jfloatArray *jfloatArray1;
    static int jfloatArray1_Size;
    static int jfloatArray1_index;
    static jmethodID pushToVideo;
    static bool videoRecording;

public:
    static unsigned char *audioToVideoBytes;
};

#endif //AMP_RACK_METER_H
