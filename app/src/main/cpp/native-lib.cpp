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

    for (int x = 0 ; x < engine->activePlugins.size();x++) {
        engine->activePlugins.at(x)->print();
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

    if (device_id == -1)
        engine->setRecordingDeviceId(oboe::kUnspecified);
    else
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

    if (device_id == -1)
        engine->setPlaybackDeviceId(oboe::kUnspecified);
    else
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
    float res [] = {p->getValue(), p->getMin(), p->getMax(), static_cast<float>(p->type)};
//    float res [] = {p->getDefault(), p->getMin(), p->getMax(), static_cast<float>(p->type)};
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
    LOGD("plugins in chain: %d", engine->activePlugins.size());
    return engine->activePlugins.size();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_deletePlugin(JNIEnv *env, jclass clazz, jint plugin) {
    // TODO: implement deletePlugin()
    IN
    LOGD("Deleting plugin at position %d", plugin);
    OUT
    return engine->deletePluginFromRack(plugin);
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

    if (engine ->activePlugins.size() == 0) {
        LOGE("Plugin control set value requested but chain is empty!");
        return ;
    }

    if (plugin >= engine->activePlugins.size()) {
        HERE LOGE("[%d] plugin requested but only %d plugins are active", plugin, engine->activePlugins.size());
        return;
    }

    if (control >= engine->activePlugins.at(plugin)->pluginControls.size()) {
        HERE LOGE ("[%d] control requested but plugin [%s] has only [%d] controls", control, engine->activePlugins.at(plugin)->descriptor->Name, engine->activePlugins.at(plugin)->pluginControls.size());
        return ;
    }

    LOGD("[%s %d] setting control %s to %f", engine->activePlugins.at(plugin)->descriptor->Name, control, engine->activePlugins.at(plugin)->pluginControls.at(control)->name, value);
    engine->activePlugins.at(plugin)->pluginControls.at(control)->setValue(value);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_movePlugin(JNIEnv *env, jclass clazz, jint plugin,
                                                         jint position) {
    // TODO: implement movePlugin()
    LOGF("%s UNIMPLEMENTED", __PRETTY_FUNCTION__ );
    return -1 ;
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
    engine->buildPluginChain();
    return engine->activePlugins.at(plugin)->active ;
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

    if (engine->activePlugins.size() <= plugin) {
        HERE LOGE("[%d] plugin requested but only [%d] plugins in queue.", plugin, engine->activePlugins.size());
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
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_loadLibrary(JNIEnv *env, jclass clazz, jstring filename) {
    // TODO: implement loadLibrary()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }
    const char *nativeString = env->GetStringUTFChars(filename, 0);

    // use your string

    engine->loadPlugin(const_cast<char *>(nativeString));
    env->ReleaseStringUTFChars(filename, nativeString);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_loadPlugins(JNIEnv *env, jclass clazz) {
    // TODO: implement loadPlugins()
    IN
    if (engine == NULL) {
        LOGF ("engine is NULL");
        OUT
        return ;
    }
    engine ->loadPlugins();
    engine -> bootComplete = true ;
    /*
    jmethodID mid = env->GetStaticMethodID(clazz, "hideProgress", "()V");
    env->CallStaticVoidMethod(clazz, mid);
     */
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_debugInfo(JNIEnv *env, jclass clazz) {
    // TODO: implement debugInfo()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }

    HERE
    LOGF("----------------| Active plugin chain |--------------------");
    for (int i = 0 ; i < engine->activePlugins.size(); i ++) {
        engine->activePlugins.at(i)->print();
    }

    LOGD("[%s %d] %s: Loaded plugins: %d", __FILE_NAME__, __LINE__, __PRETTY_FUNCTION__ , engine->libraries.size()) ;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_movePluginDown(JNIEnv *env, jclass clazz,
                                                             jint plugin) {
    // TODO: implement movePluginDown()
    return engine ->moveActivePluginDown(plugin);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_movePluginUp(JNIEnv *env, jclass clazz, jint plugin) {
    // TODO: implement movePluginUp()
    return engine->moveActivePluginUp(plugin);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setExternalStoragePath(JNIEnv *env, jclass clazz,
                                                                     jstring path) {
    // TODO: implement setExternalStoragePath()
    IN ;
    if (engine == NULL) {
        HERE LOGF ("engine is NULL"); OUT
        return ;
    }

    const char *nativeString = env->GetStringUTFChars(path, 0);
    engine->externalStoragePath = std::string (nativeString);
    env->ReleaseStringUTFChars(path, nativeString);
    LOGD("Output file path set to %s", engine->externalStoragePath.c_str());
    OUT
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setRecordingActive(JNIEnv *env, jclass clazz,
                                                                 jboolean active) {
    // TODO: implement setRecordingActive()
    if (engine == NULL) {
        HERE LOGF ("engine is NULL");
        return ;
    }

    engine->mFullDuplexPass.recordingActive = active ;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_addPluginByName(JNIEnv *env, jclass clazz,
                                                              jstring name) {
    // TODO: implement addPluginByName()
    const char *nativeString = env->GetStringUTFChars(name, 0);
    engine ->addPlugintoRackByName(std::string (nativeString));
    env->ReleaseStringUTFChars(name, nativeString);
    return engine->activePlugins.size();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setPresetValue(JNIEnv *env, jclass clazz, jint plugin,
                                                             jint control, jfloat value) {
    // TODO: implement setPresetValue()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return ;
    }

    IN
    if (plugin >= engine -> activePlugins.size()) {
        LOGE("requested plugin no %d greater than total plugins %d", plugin, engine->activePlugins.size());
    } else if (control >= engine -> activePlugins.at(plugin)->pluginControls.size()) {
        LOGE("requested plugin control no %d greater than total plugin controls %d", control, engine->activePlugins.at(plugin)->pluginControls.size());
    } else {
        LOGD("[%s %d] setting control %s to %f", engine->activePlugins.at(plugin)->descriptor->Name, control, engine->activePlugins.at(plugin)->pluginControls.at(control)->name, value);
        engine->activePlugins.at(plugin)->pluginControls.at(control)->setPresetValue(value);
    }

    OUT
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPluginPresetValue(JNIEnv *env, jclass clazz,
                                                                   jint plugin, jint control) {
    // TODO: implement getPluginPresetValue()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return -1;
    }

    return engine->activePlugins.at(plugin)->pluginControls.at(control)->presetValue ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setExportFormat(JNIEnv *env, jclass clazz,
                                                              jint format) {
    // TODO: implement setExportFormat()
        if (engine == NULL) {
        LOGF ("engine is NULL");
            return;
    }

    engine->fileWriter->setFileType(format);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_clearActiveQueue(JNIEnv *env, jclass clazz) {
    // TODO: implement clearActiveQueue()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    engine->activePlugins.clear();
    engine-> buildPluginChain();
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getRecordingFileName(JNIEnv *env, jclass clazz) {
    // TODO: implement getRecordingFileName()
    return env ->NewStringUTF(engine->fileWriter->filename.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setLowLatency(JNIEnv *env, jclass clazz,
                                                            jboolean low_latency) {
    // TODO: implement setLowLatency()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }
    if (low_latency) {
        engine->lowLatency = 12 ;
    } else {
        engine->lowLatency = 10 ;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setSampleRate(JNIEnv *env, jclass clazz,
                                                            jint sample_rate) {
    // TODO: implement setSampleRate()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    engine->mSampleRate = sample_rate;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setOpusBitRate(JNIEnv *env, jclass clazz,
                                                             jint bitrate) {
    // TODO: implement setOpusBitRate()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return;
    }

    engine->fileWriter->bitRate = bitrate;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getTotalPlugins(JNIEnv *env, jclass clazz) {
    // TODO: implement getTotalPlugins()
    if (engine == NULL) return 0;

    int plugins = 0 ;
    for (int i = 0 ; i < engine->libraries.size();i++) {
        plugins += engine->libraries.at(i)->total_plugins;
    }

    return plugins;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_wasLowLatency(JNIEnv *env, jclass clazz) {
    // TODO: implement wasLowLatency()
    if (engine == NULL) return false;

    return engine->lowLatencyMode;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getPluginUniqueID(JNIEnv *env, jclass clazz,
                                                                jint library, jint plugin) {
    // TODO: implement getPluginUniqueID()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return NULL;
    }
    return engine->libraries.at(library)->descriptors.at(plugin)->UniqueID;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getActivePlugins(JNIEnv *env, jclass clazz) {
    // TODO: implement getActivePlugins()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    return  engine->activePlugins.size();
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_getActivePluginValues(JNIEnv *env, jclass clazz,
                                                                    jint plugin) {
    // TODO: implement getActivePluginValues()
    if (engine == NULL) {
        LOGF ("engine is NULL");
        return 0;
    }
    // [default, min, max, type]
    Plugin *p = engine ->activePlugins.at(plugin);
    std::vector<float> controls ;
    for (int i = 0 ; i < p->pluginControls.size(); i ++) {
        controls.push_back(*p->pluginControls.at(i)->def);
    }

    jfloatArray r = env ->NewFloatArray(p->pluginControls.size());
//    float res [] = *controls.data()[0];
//    float res [] = {p->getDefault(), p->getMin(), p->getMax(), static_cast<float>(p->type)};
    env->SetFloatArrayRegion(r, 0, p->pluginControls.size(), controls.data());
    return r ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_setLazyLoad(JNIEnv *env, jclass clazz,
                                                          jboolean lazy_load) {
    // TODO: implement setLazyLoad()
    // so finally i learnt this!
    engine == NULL ? NULL : engine -> lazyLoad = lazy_load;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_addPluginLazy(JNIEnv *env, jclass clazz,
                                                            jstring library, jint plugin) {
    // TODO: implement addPluginLazy()
    if (engine == NULL) return -1 ;
    const char *nativeString = env->GetStringUTFChars(library, 0);
    LOGD("Loading lazy plugin [%s : %d]", nativeString, plugin);
    engine ->addPluginToRackLazy(const_cast<char *>(nativeString), plugin);
    env->ReleaseStringUTFChars(library, nativeString);
    return engine -> activePlugins.size();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_shajikhan_ladspa_amprack_AudioEngine_testLV2(JNIEnv *env, jclass clazz) {
    // TODO: implement testLV2()
    engine ->loadPlugin("rkrlv2.so", SharedLibrary::LV2);
}