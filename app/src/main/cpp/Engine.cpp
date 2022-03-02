#include <cassert>
#include <logging_macros.h>

#include "Engine.h"

Engine::Engine () {
    assert(mOutputChannelCount == mInputChannelCount);
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
//    IN ;
    bool success = true;
    if (isOn != mIsEffectOn) {
        if (isOn) {
            success = openStreams() == oboe::Result::OK;
            if (success) {
                mFullDuplexPass.start();
//                addPluginToRack(0, 0);
                mIsEffectOn = isOn;
            }
        } else {
            mFullDuplexPass.stop();
            closeStreams();
            mIsEffectOn = isOn;
        }
    }

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
    result = inBuilder.openStream(mRecordingStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream. Error %s", oboe::convertToText(result));
        closeStream(mPlayStream);
        OUT ;
        return result;
    }
    warnIfNotLowLatency(mRecordingStream);

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
            ->setSampleRate(sampleRate)
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
            ->setFormatConversionAllowed(true)
            ->setSharingMode(oboe::SharingMode::Exclusive)
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

void Engine::loadPlugin(char * filename) {
    IN
    SharedLibrary * sharedLibrary = new SharedLibrary (filename);
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
    mFullDuplexPass.activePlugins = 0;
    for (Plugin *p: activePlugins) {
        mFullDuplexPass.inputPorts [mFullDuplexPass.activePlugins] = p->inputPort ;
        mFullDuplexPass.outputPorts [mFullDuplexPass.activePlugins] = p->outputPort ;
        mFullDuplexPass.connect_port [mFullDuplexPass.activePlugins] = p->descriptor->connect_port ;
        mFullDuplexPass.run [mFullDuplexPass.activePlugins] = p->descriptor->run ;
        mFullDuplexPass.handle [mFullDuplexPass.activePlugins] = p->handle ;
        mFullDuplexPass.descriptor [mFullDuplexPass.activePlugins] = p->descriptor;
        mFullDuplexPass.activePlugins ++ ;
    }
}

void Engine::addPluginToRack (int libraryIndex, int pluginIndex) {
    IN
    Plugin * plugin = new Plugin (libraries.at(libraryIndex)->descriptors.at(pluginIndex), (long) mSampleRate);
    activePlugins .push_back(plugin);
    buildPluginChain();
    OUT
}

void Engine::loadPlugins () {
    for (SharedLibrary *sharedLibrary: libraries) {
        sharedLibrary->setSampleRate(mSampleRate);
        sharedLibrary->load();
    }
}