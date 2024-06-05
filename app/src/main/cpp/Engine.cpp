#include <cassert>

#include "Engine.h"
#include "logging_macros.h"

jmethodID Engine::pushAudio = nullptr;

Engine::Engine () {
    assert(mOutputChannelCount == mInputChannelCount);
//    queueManager = new LockFreeQueueManager ();
    fileWriter = new FileWriter ();
    queueManager.add_function (fileWriter->disk_write);
    queueManager.add_function (meter->updateMeterOutput);
//    discoverPlugins();
//    loadPlugins();
}

void Engine::setRecordingDeviceId(int32_t deviceId) {
    mRecordingDeviceId = deviceId;
}

void Engine::setPlaybackDeviceId(int32_t deviceId) {
    mPlaybackDeviceId = deviceId;
}

bool Engine::isAAudioRecommended() {
    return oboe::AudioStreamBuilder::isAAudioRecommended();
}

bool Engine::setAudioApi(oboe::AudioApi api) {
    if (mIsEffectOn) return false;
    mAudioApi = api;
    return true;
}

bool Engine::setEffectOn(bool isOn) {
    IN ;
    bool success = true;
    if (isOn != mIsEffectOn) {
        if (isOn) {
            success = openStreams() == oboe::Result::OK;
            if (success) {
                /*
                for (int i = 0 ; i < activePlugins.size() ; i ++) {
                    activePlugins.at(i)->print();
                }
                */

//                mFullDuplexPass.queue = queueManager.process ;
                mFullDuplexPass.lockFreeQueueManager = &queueManager ;
                mFullDuplexPass.start();

                fileWriter->setSampleRate (mSampleRate);

//                char buffer [80];
//
//                time_t rawtime;
//                struct tm * timeinfo;
//                time (&rawtime);
//                timeinfo = localtime (&rawtime);
//
//                strftime (buffer,80,"%d-%m-%y__%I.%M%p",timeinfo);

//                fileWriter->setFileName(externalStoragePath + std::string ("/AmpRack/") + std::string (buffer)) ;
//                fileWriter->setFileName(externalStoragePath + "/" + std::string (buffer)) ;
//                fileWriter->setBufferSize(mFullDuplexPass.mBufferSize);
                int bufferSizeInFrames = mRecordingStream->getBufferSizeInFrames() ;
                fileWriter->setBufferSize (mPlayStream->getBufferSizeInFrames()/mPlayStream->getChannelCount());

                queueManager.init (mPlayStream->getBufferSizeInFrames()/mPlayStream->getChannelCount());

                if (mFullDuplexPass.inSamples != NULL)
                    free (static_cast<void *>(mFullDuplexPass.inSamples));

                if (mFullDuplexPass.raw != NULL)
                    free (static_cast<void *>(mFullDuplexPass.raw));

                mFullDuplexPass.inSamples = static_cast<float *>(malloc(mFullDuplexPass.mBufferSize));
                LOGD("[engine] allocated %d bytes to raw samples", mFullDuplexPass.mBufferSize);
                mFullDuplexPass.raw = static_cast<float *>(malloc(mFullDuplexPass.mBufferSize));
//                for (int i = 0 ; i < mFullDuplexPass.mBufferSize; i ++)
//                    mFullDuplexPass.raw [i] = 0.0f ;
                fileWriter->setChannels(mOutputChannelCount);
                /*
                if (mFullDuplexPass.recordingActive) {
                    fileWriter->startRecording();
                }
                 */

                meter->lastRecordedFileName = fileWriter->filename;
                meter->jack_samplerate = mSampleRate ;
                meter->lowLatency = lowLatencyMode ;
                meter->enable();
                meter->start();
//                addPluginToRack(0, 0);
                mIsEffectOn = isOn;
            }
        } else {
            if (mFullDuplexPass.recordingActive) {
                fileWriter->stopRecording() ;
                mFullDuplexPass.recordingActive = false;
            }

//            LOGD("stopping meter");
            meter->stop();

            mFullDuplexPass.stop();
            closeStreams();
            mIsEffectOn = isOn;
        }
    }

    OUT
    return success;
}


void Engine::closeStreams() {
    IN ;
    /*
    * Note: The order of events is important here.
    * The playback stream must be closed before the recording stream. If the
    * recording stream were to be closed first the playback stream's
    * callback may attempt to read from the recording stream
    * which would cause the app to crash since the recording stream would be
    * null.
    */

    queueManager.quit ();

    closeStream(mPlayStream);
    mFullDuplexPass.setOutputStream(nullptr);

    closeStream(mRecordingStream);
    mFullDuplexPass.setInputStream(nullptr);
}

oboe::Result  Engine::openStreams() {
    IN ;
    // Note: The order of stream creation is important. We create the playback
    // stream first, then use properties from the playback stream
    // (e.g. sample rate) to create the recording stream. By matching the
    // properties we should get the lowest latency path
    oboe::AudioStreamBuilder inBuilder, outBuilder;
    setupPlaybackStreamParameters(&outBuilder);
    oboe::Result result = outBuilder.openStream(mPlayStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open output stream. Error %s", oboe::convertToText(result));
        mSampleRate = oboe::kUnspecified;
        OUT ;
        return result;
    } else {
        // The input stream needs to run at the same sample rate as the output.
        mSampleRate = mPlayStream->getSampleRate();
    }
    warnIfNotLowLatency(mPlayStream);

    setupRecordingStreamParameters(&inBuilder, mSampleRate);
//    inBuilder.setBufferCapacityInFrames(160);
//    inBuilder.setFramesPerDataCallback(160);
//    inBuilder.setFramesPerCallback(160);

    result = inBuilder.openStream(mRecordingStream);

    latencyTuner = new oboe::LatencyTuner ( *mRecordingStream);
//    latencyTuner->setMinimumBufferSize(160);
//    mRecordingStream->setBufferSizeInFrames(80);
    latencyTuner->tune();
//    mRecordingStream->setBufferSizeInFrames(80);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream. Error %s", oboe::convertToText(result));
        closeStream(mPlayStream);
        OUT ;
        return result;
    }

    latencyTunerOut = new oboe::LatencyTuner ( *mPlayStream);
    latencyTunerOut->tune();
    warnIfNotLowLatency(mRecordingStream);

//    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
//    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst() * 2);
    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst() * 2);
    mFullDuplexPass.setInputStream(mRecordingStream);
    mFullDuplexPass.setOutputStream(mPlayStream);

    // Load LADSPA Plugin here

    /*
    LOGV("Checking if plugin state is null ...\n");
    if (mFullDuplexPass.duplexPluginState == NULL) {
        LOGV("Assinging plugin\n");
        mFullDuplexPass .duplexPluginState = loadPlugin();
        pluginState = mFullDuplexPass.duplexPluginState;
    } else {
        LOGV("It isn't ... already loaded %s\n", mFullDuplexPass.duplexPluginState -> descriptor -> Name);
    }

    LOGD("%s loadPlugin [ok]: %s %s\n", __PRETTY_FUNCTION__ , mFullDuplexPass .get_plugin() -> descriptor -> Label, mFullDuplexPass .get_plugin() -> descriptor -> Name);
     */
    OUT ;
    return result;
}

/**
 * Sets the stream parameters which are specific to recording,
 * including the sample rate which is determined from the
 * playback stream.
 *
 * @param builder The recording stream builder
 * @param sampleRate The desired sample rate of the recording stream
 */
oboe::AudioStreamBuilder *Engine::setupRecordingStreamParameters(
        oboe::AudioStreamBuilder *builder, int32_t sampleRate) {
    // This sample uses blocking read() because we don't specify a callback
    builder->setDeviceId(mRecordingDeviceId)
            ->setDirection(oboe::Direction::Input)
            ->setSampleRate(mSampleRate)
//            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
//            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(mInputChannelCount);
    return setupCommonStreamParameters(builder);
}

/**
 * Sets the stream parameters which are specific to playback, including device
 * id and the dataCallback function, which must be set for low latency
 * playback.
 * @param builder The playback stream builder
 */
oboe::AudioStreamBuilder *Engine::setupPlaybackStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    builder->setDataCallback(this)
            ->setErrorCallback(this)
            ->setDeviceId(mPlaybackDeviceId)
            ->setDirection(oboe::Direction::Output)
//            ->setSampleRate(48000)
            /* fixme:
             * we do this because
             * 1. sample rate on Android is *almost always™ 48000
             * 2. Android AAC encoder does not support sample rate > 48000
             * 3. Even if it did, it might not be *that* much of an enhancement
             *    in order for it to justify complex resampling in real time
             */
            ->setChannelCount(mOutputChannelCount);

    return setupCommonStreamParameters(builder);
}

/**
 * Set the stream parameters which are common to both recording and playback
 * streams.
 * @param builder The playback or recording stream builder
 */
oboe::AudioStreamBuilder *Engine::setupCommonStreamParameters(
        oboe::AudioStreamBuilder *builder) {
    // We request EXCLUSIVE mode since this will give us the lowest possible
    // latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED
    // mode.
    builder->setAudioApi(mAudioApi)
            ->setFormat(mFormat)
            ->setChannelConversionAllowed(true)
//            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Fastest)
            ->setFormatConversionAllowed(true)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setInputPreset(oboe::VoicePerformance)
//            ->setPerformanceMode(static_cast<oboe::PerformanceMode>(lowLatency));
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency);
    return builder;
}

/**
 * Close the stream. AudioStream::close() is a blocking call so
 * the application does not need to add synchronization between
 * onAudioReady() function and the thread calling close().
 * [the closing thread is the UI thread in this sample].
 * @param stream the stream to close
 */
void Engine::closeStream(std::shared_ptr<oboe::AudioStream> &stream) {
    IN ;
    if (stream) {
        oboe::Result result = stream->stop();
        if (result != oboe::Result::OK) {
            LOGW("Error stopping stream: %s", oboe::convertToText(result));
        }
        result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing stream: %s", oboe::convertToText(result));
        } else {
            LOGW("Successfully closed streams");
        }
        stream.reset();
    }

    OUT ;
}

/**
 * Warn in logcat if non-low latency stream is created
 * @param stream: newly created stream
 *
 */
void Engine::warnIfNotLowLatency(std::shared_ptr<oboe::AudioStream> &stream) {
    if (stream->getPerformanceMode() != oboe::PerformanceMode::LowLatency) {
        LOGW(
                "Stream is NOT low latency."
                "Check your requested format, sample rate and channel count");
        LOGW("Running in mode: %d\tchannels: %d\tsharing mode: %d\tsample rate: %d\tformat: %d",
             stream->getPerformanceMode(),
             stream->getChannelCount(),
             stream->getSharingMode(),
             stream->getSampleRate(),
             stream->getFormat());
        lowLatencyMode = false ;
    } else {
        lowLatencyMode = true ;
        LOGD ("Congratulations, you have achieved Low Latency!");
        LOGD("Running in Low Latency mode: %d\tchannels: %d\tsharing mode: %d\tsample rate: %d\tformat: %d\tbuffer size: %d",
             stream->getPerformanceMode(),
             stream->getChannelCount(),
             stream->getSharingMode(),
             stream->getSampleRate(),
             stream->getFormat(),
             stream->getBufferSizeInFrames());
    }
}

/**
 * Handles playback stream's audio request. In this sample, we simply block-read
 * from the record stream for the required samples.
 *
 * @param oboeStream: the playback stream that requesting additional samples
 * @param audioData:  the buffer to load audio samples for playback stream
 * @param numFrames:  number of frames to load to audioData buffer
 * @return: DataCallbackResult::Continue.
 */
oboe::DataCallbackResult Engine::onAudioReady(
        oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    return mFullDuplexPass.onAudioReady(oboeStream, audioData, numFrames);
}

/**
 * Oboe notifies the application for "about to close the stream".
 *
 * @param oboeStream: the stream to close
 * @param error: oboe's reason for closing the stream
 */
void Engine::onErrorBeforeClose(oboe::AudioStream *oboeStream,
                                          oboe::Result error) {
    LOGE("%s stream Error before close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));
}

/**
 * Oboe notifies application that "the stream is closed"
 *
 * @param oboeStream
 * @param error
 */
void Engine::onErrorAfterClose(oboe::AudioStream *oboeStream,
                                         oboe::Result error) {
    LOGE("%s stream Error after close: %s",
         oboe::convertToText(oboeStream->getDirection()),
         oboe::convertToText(error));
}

void Engine::addPluginToRackLazy(char* library, int pluginIndex, SharedLibrary::PluginType _type) {
    SharedLibrary * sharedLibrary = new SharedLibrary (library, _type);
    sharedLibrary -> vm = vm ;
    if (mainActivityClassName != "")
        sharedLibrary->mainActivityClassName = mainActivityClassName ;

    sharedLibrary ->setLibraryPath(LIBRARY_PATH);
    sharedLibrary->load();

    LOGD("loaded shared library [ok] ... now trying to load plugin");
    Plugin * plugin = new Plugin (sharedLibrary->descriptors.at(pluginIndex), (long) mSampleRate, _type);
    plugin->sharedLibrary = sharedLibrary;
    if (_type == SharedLibrary::LV2) {
        plugin -> load () ;
    }
    activePlugins .push_back(plugin);
    buildPluginChain();
}

void Engine::loadPlugin(char * filename, SharedLibrary::PluginType type) {
    IN
    LOGD("Loading plugin %s", filename);
    SharedLibrary * sharedLibrary = new SharedLibrary (filename, type);
    libraries.push_back(sharedLibrary);
    OUT
}

void Engine::discoverPlugins () {
    IN

    if (libraries.size() != 0) {
        // we only run this once
        LOGE("tried to re-discover plugins! not allowed") ;
        OUT
        return ;
    }

    SharedLibrary * sharedLibrary = new SharedLibrary ("libamp.so");
    libraries.push_back(sharedLibrary);
    SharedLibrary * sharedLibrary1 = new SharedLibrary ("libdistortionx.so");
    libraries.push_back(sharedLibrary1);
    SharedLibrary * sharedLibrary2 = new SharedLibrary ("libcrybabyx.so");
    libraries.push_back(sharedLibrary2);
    SharedLibrary * sharedLibrary3 = new SharedLibrary ("/data/data/com.shajikhan.ladspa.amprack/lib/libtubex.so");
    libraries.push_back(sharedLibrary3);
    SharedLibrary * sharedLibrary4 = new SharedLibrary ("/data/data/com.shajikhan.ladspa.plugins.tap/lib/libtap_eq.so");
    libraries.push_back(sharedLibrary4);
    OUT
}

void Engine::buildPluginChain () {
    IN
    LOGD("building chain for %d plugins", activePlugins.size());
    mFullDuplexPass.activePlugins = 0;
    for (Plugin *p: activePlugins) {
        if (!p->active)
            continue;
        mFullDuplexPass.inputPorts [mFullDuplexPass.activePlugins] = p->inputPort ;
        mFullDuplexPass.outputPorts [mFullDuplexPass.activePlugins] = p->outputPort ;
        mFullDuplexPass.inputPorts2 [mFullDuplexPass.activePlugins] = p->inputPort2 ;
        mFullDuplexPass.outputPorts2 [mFullDuplexPass.activePlugins] = p->outputPort2 ;
        if (p->type == SharedLibrary::LADSPA) {
            mFullDuplexPass.connect_port [mFullDuplexPass.activePlugins] = p->descriptor->connect_port ;
            mFullDuplexPass.run [mFullDuplexPass.activePlugins] = p->descriptor->run ;
            mFullDuplexPass.run_adding [mFullDuplexPass.activePlugins] = p->descriptor->run_adding ;
            mFullDuplexPass.set_run_adding_gain [mFullDuplexPass.activePlugins] = p->descriptor->set_run_adding_gain ;
            mFullDuplexPass.descriptor [mFullDuplexPass.activePlugins] = p->descriptor;
        } else if (p->type == SharedLibrary::LV2) {
            mFullDuplexPass.connect_port [mFullDuplexPass.activePlugins] = reinterpret_cast<void (*)(
                    LADSPA_Handle, unsigned long, LADSPA_Data *)>(p->lv2Descriptor->connect_port);
            mFullDuplexPass.run [mFullDuplexPass.activePlugins] = reinterpret_cast<void (*)(
                    LADSPA_Handle, unsigned long)>(p->lv2Descriptor->run);
            mFullDuplexPass.descriptor [mFullDuplexPass.activePlugins] = reinterpret_cast<const LADSPA_Descriptor *>(p->lv2Descriptor);
        }

        mFullDuplexPass.run_adding_gain [mFullDuplexPass.activePlugins] = p->run_adding_gain ;
        mFullDuplexPass.handle [mFullDuplexPass.activePlugins] = p->handle ;
        mFullDuplexPass.activePlugins ++ ;
    }

    if (mIsEffectOn) {
        LOGD("adjusting latency ...");

        latencyTuner->tune();
        latencyTunerOut->tune();
    }

    OUT
}

void Engine::addPluginToRack (int libraryIndex, int pluginIndex) {
    IN
    LOGD("Adding plugin %d: %d", libraryIndex, pluginIndex);
    if (libraryIndex > libraries.size()) {
        LOGF ("index %d > libraries size %d", libraryIndex, libraries.size()) ;
        return ;
    }

    Plugin * plugin = new Plugin (libraries.at(libraryIndex)->descriptors.at(pluginIndex), (long) mSampleRate);
    activePlugins .push_back(plugin);
    buildPluginChain();
    OUT
}

int Engine::deletePluginFromRack (int pIndex) {
    IN
    Plugin * p = activePlugins.at(pIndex);
    for (PluginControl * control: p->pluginControls) {
        control->freeMemory();
    }

    if (lazyLoad) {
        p->free();
        p->sharedLibrary->unload();
    }

    delete p ;
    activePlugins.erase( next(begin(activePlugins), pIndex));
    buildPluginChain();
    return activePlugins.size();
    OUT
}

void Engine::loadPlugins () {
    /*
    if (libraries.size()) {
        LOGD("%d pLugins already loaded, not loading again", libraries.size());
        return ;
    }
     */

    for (SharedLibrary *sharedLibrary: libraries) {
        sharedLibrary->setSampleRate(mSampleRate);
        sharedLibrary->load();
    }

    LOGD("[Audio Engine]: Initialized %d plugins!", libraries.size());
}

int Engine :: moveActivePluginDown (int _p) {
    IN
    if (_p == activePlugins.size()) {
        OUT
        return _p ;
    }

    auto it = activePlugins.begin() + _p;
    std::rotate(it, it + 1, activePlugins.end());
    buildPluginChain();
    OUT
    return _p + 1 ;
}

int Engine :: moveActivePluginUp (int _p) {
    IN
    if (_p == 0) {
        OUT
        return _p ;
    }

    auto it = activePlugins.begin() + _p;
    std::rotate(it - 1,  it, activePlugins.end());
    buildPluginChain();
    OUT
    return _p - 1 ;
}

int Engine::addPlugintoRackByName (std::string pluginName) {
    for (int i = 0 ; i < libraries.size() ; i ++) {
        SharedLibrary *sharedLibrary = libraries.at(i);
        for (int j = 0 ; j < sharedLibrary->total_plugins; j ++) {
            if (std::string (sharedLibrary->descriptors [j]->Name) == pluginName) {
                addPluginToRack(i, j);
                return 1 ;
            }
        }
    }

    return  0 ;
}

void Engine::setPluginFilename (std::string filename, int plugin) {
    IN
    mFullDuplexPass.bypass = true;
    activePlugins.at(plugin)->setFileName(filename);
    mFullDuplexPass.bypass = false;
    OUT
}

bool Engine::setPluginBuffer (float * buffer, int buffer_size, int plugin) {
    IN
//    LOGD("plugins in queue %d requested %d", activePlugins.size(), plugin);
    mFullDuplexPass.bypass = true ;
////    activePlugins.at (plugin)->active = false ;
    activePlugins.at (plugin)->setBuffer (buffer, buffer_size);
////    activePlugins.at (plugin)->active = true ;
    mFullDuplexPass.bypass = false ;
    OUT
    return true ;
}

std::string Engine::tuneLatency () {
    IN
    if (! mIsEffectOn)
        return std::string ("Turn on audio engine first") ;

    latencyTuner->requestReset();
    oboe::Result result = latencyTuner->tune() ;
    latencyTunerOut->requestReset();
    oboe::Result resultOut = latencyTunerOut->tune() ;
//    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    char tmp [450];
    // hello, old friend
    sprintf (tmp, "Latency: %.0fms, Xruns: %d/%d, Buffer size: %d/%d",
             getLatency(true) + getLatency(false),
             mRecordingStream->getXRunCount(),mPlayStream->getXRunCount(),
             mRecordingStream->getBufferSizeInFrames(), mPlayStream->getBufferSizeInFrames());
    LOGD ("%s",tmp);
    OUT
    return std::string (tmp) ;
}

void Engine::test () {
    float * sampleData ;

    oboe::Result result ;
    while (mIsEffectOn) {
        result = mPlayStream->read(&sampleData, 192, 0) ;
        LOGD("read %d result", result);
    }
}

int Engine::setTuner (buffer_t * buffer) {

}


int Engine::pushToVideo (AudioBuffer * buffer) {
    if (pushAudio == nullptr) {

    }

}

long Engine::getTimeStamp () {
    const oboe::ResultWithValue<oboe::FrameTimestamp> &frameTimeStamp = mRecordingStream->getTimestamp(
            CLOCK_MONOTONIC);
    return frameTimeStamp.value().timestamp ;
}

void Engine::setSampleRateDisplay (int sampleRate, bool lowLatency) {

}

double Engine::getLatency (bool input) {
    if (mPlayStream->usesAAudio())
        LOGI("using AAudio");

    if (input) {
        const oboe::ResultWithValue<double> &latency = mRecordingStream->calculateLatencyMillis();
        return latency.value();
    } else {
        int frames = mPlayStream->getFramesWritten() - mPlayStream->getFramesRead();
        int bufferLatencyMillis = 1000 * frames / mPlayStream->getSampleRate();

        const oboe::ResultWithValue<double> &latency = mPlayStream->calculateLatencyMillis();
        LOGD("[%f: %d] %d %d %d %d", latency.value(), bufferLatencyMillis,
                mPlayStream->getFramesPerBurst(),
                mPlayStream->getXRunCount().value(),
                mPlayStream->getBufferSizeInFrames(),
                mPlayStream->getBufferCapacityInFrames());
//        mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
        return latency.value() ;// - bufferLatencyMillis;
    }
}

void Engine::popFunction () {
    queueManager.pop_function();
}

int Engine::getBufferFrameSize (bool input) {
    if (input)
        return mRecordingStream->getBufferSizeInFrames();
    else
        return mPlayStream->getBufferSizeInFrames();
}

void Engine::fixGlitches () {
    IN
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());
    mPlayStream->setBufferSizeInFrames(mPlayStream->getBufferCapacityInFrames());
    OUT
}

void Engine::minimizeLatency () {
    IN

    mPlayStream->setBufferSizeInFrames(mPlayStream->getFramesPerBurst());
    mRecordingStream->setBufferSizeInFrames(mRecordingStream->getFramesPerBurst());

    OUT
}