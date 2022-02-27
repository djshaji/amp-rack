#include <jni.h>
#include <string>
#include "logging_macros.h"
#include "ladspa.h"

#include "Engine.h"
static const int kOboeApiAAudio = 0;
static const int kOboeApiOpenSLES = 1;

static Engine * engine = nullptr;


extern "C" JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_create(JNIEnv *env, jclass clazz) {
    IN ;
    if (engine == nullptr) {
        engine = new Engine () ;
    }

    OUT ;
    return (engine != nullptr) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_isAAudioRecommended(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine "
                "before calling this method");
        return JNI_FALSE;
    }

    return engine->isAAudioRecommended() ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setAPI(JNIEnv *env, jclass clazz, jint api_type) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine "
                "before calling this method");
        return JNI_FALSE;
    }

    oboe::AudioApi audioApi;
    switch (api_type) {
        case kOboeApiAAudio:
            audioApi = oboe::AudioApi::AAudio;
            break;
        case kOboeApiOpenSLES:
            audioApi = oboe::AudioApi::OpenSLES;
            break;
        default:
            LOGE("Unknown API selection to setAPI() %d", api_type);
            return JNI_FALSE;
    }

    return engine->setAudioApi(audioApi) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setEffectOn(JNIEnv *env, jclass clazz,
                                                        jboolean is_effect_on) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return JNI_FALSE;
    }

    return engine->setEffectOn(is_effect_on) ? JNI_TRUE : JNI_FALSE;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setRecordingDeviceId(JNIEnv *env, jclass clazz,
                                                                 jint device_id) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return;
    }

    engine->setRecordingDeviceId(device_id);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setPlaybackDeviceId(JNIEnv *env, jclass clazz,
                                                                jint device_id) {
    if (engine == nullptr) {
        LOGE(
                "Engine is null, you must call createEngine before calling this "
                "method");
        return;
    }

    engine->setPlaybackDeviceId(device_id);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_delete(JNIEnv *env, jclass clazz) {
    if (engine) {
        engine->setEffectOn(false);
        delete engine;
        engine = nullptr;
    }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_native_1setDefaultStreamValues(JNIEnv *env,
                                                                           jclass clazz,
                                                                           jint default_sample_rate,
                                                                           jint default_frames_per_burst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) default_sample_rate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) default_frames_per_burst;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setMode(JNIEnv *env, jclass clazz, jint mode) {
    // TODO: implement setMode()
    if (engine == nullptr) return ;
    LOGD("setting mode to %d", mode) ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getSharedLibraries(JNIEnv *env, jclass clazz) {
    // TODO: implement getSharedLibraries()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return engine->libraries.size() ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPlugins(JNIEnv *env, jclass clazz, jint library) {
    // TODO: implement getPlugins()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return engine->libraries.at(library)->total_plugins ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPluginControls(JNIEnv *env, jclass clazz,
                                                                jint plugin) {
    // TODO: implement getPluginControls()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return engine->activePlugins.at(plugin)->pluginControls.size();
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPluginControlValues(JNIEnv *env, jclass clazz,
                                                                     jint plugin, jint control) {
    // TODO: implement getPluginControlValues()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    // [default, min, max, type]
    PluginControl *p = engine ->activePlugins.at(plugin)->pluginControls.at(control) ;
    jfloatArray r = env ->NewFloatArray(4);
    float res [] = {p->getDefault(), p->getMin(), p->getMax(), static_cast<float>(p->type)};
    env->SetFloatArrayRegion(r, 0, 4, res);
    return r ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_addPlugin(JNIEnv *env, jclass clazz, jint library,
                                                        jint plugin) {
    // TODO: implement addPlugin()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    engine->addPluginToRack(library, plugin);
    return engine->activePlugins.size();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_deletePlugin(JNIEnv *env, jclass clazz, jint plugin) {
    // TODO: implement deletePlugin()
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setPluginControl(JNIEnv *env, jclass clazz,
                                                               jint plugin, jint control,
                                                               jfloat value) {
    // TODO: implement setPluginControl()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }
    engine->activePlugins.at(plugin)->pluginControls.at(control)->setValue(value);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_movePlugin(JNIEnv *env, jclass clazz, jint plugin,
                                                         jint position) {
    // TODO: implement movePlugin()
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_togglePlugin(JNIEnv *env, jclass clazz, jint plugin,
                                                           jboolean state) {
    // TODO: implement togglePlugin()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return false;
    }
    engine->activePlugins.at(plugin)->active = state ;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getLibraryName(JNIEnv *env, jclass clazz,
                                                             jint library) {
    // TODO: implement getLibraryName()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    return env->NewStringUTF(engine->libraries.at(library)->so_file.c_str()) ;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPluginName(JNIEnv *env, jclass clazz, jint library,
                                                            jint plugin) {
    // TODO: implement getPluginName()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    return env->NewStringUTF(engine->libraries.at(library)->descriptors.at(plugin)->Name);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getActivePluginName(JNIEnv *env, jclass clazz,
                                                                  jint plugin) {
    // TODO: implement getActivePluginName()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    return env ->NewStringUTF(engine->activePlugins.at(plugin)->descriptor->Name);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getControlName(JNIEnv *env, jclass clazz, jint plugin,
                                                             jint control) {
    // TODO: implement getControlName()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    return env ->NewStringUTF(engine->activePlugins.at(plugin)->descriptor->PortNames [control]);
}