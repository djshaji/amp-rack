/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SAMPLES_FULLDUPLEXPASS_H
#define SAMPLES_FULLDUPLEXPASS_H

#include <dlfcn.h>
#include "ladspa.h"
#include "FullDuplexStream.h"
#include "FileWriter.h"

class FullDuplexPass : public FullDuplexStream {
    //| TODO: Limit number of plugins active in free version?
public:
    #define MAX_PLUGINS 10 // for now
    void (*connect_port [MAX_PLUGINS])(LADSPA_Handle Instance,
                         unsigned long Port,
                         LADSPA_Data * DataLocation);
    void (*run [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*run_adding [MAX_PLUGINS])(LADSPA_Handle Instance,
                unsigned long SampleCount);
    void (*set_run_adding_gain [MAX_PLUGINS])(LADSPA_Handle Instance,
                                LADSPA_Data   Gain);

    void * handle [MAX_PLUGINS] ;
    LADSPA_Data run_adding_gain [MAX_PLUGINS] ;
    const LADSPA_Descriptor * descriptor [MAX_PLUGINS] ;
    int inputPorts [MAX_PLUGINS] ;
    int inputPorts2 [MAX_PLUGINS] ;
    int outputPorts [MAX_PLUGINS] ;
    int outputPorts2 [MAX_PLUGINS] ;
    int activePlugins =  0 ;
    bool recordingActive = false ;

    virtual oboe::DataCallbackResult
    onBothStreamsReady(
            std::shared_ptr<oboe::AudioStream> inputStream,
            const void *inputData,
            int   numInputFrames,
            std::shared_ptr<oboe::AudioStream> outputStream,
            void *outputData,
            int   numOutputFrames) {
        // This code assumes the data format for both streams is Float.
        const float *inputFloats = static_cast<const float *>(inputData);
        float *outputFloats = static_cast<float *>(outputData);

        // It also assumes the channel count for each stream is the same.
        int32_t samplesPerFrame = outputStream->getChannelCount();
        int32_t numInputSamples = numInputFrames * samplesPerFrame;
        int32_t numOutputSamples = numOutputFrames * samplesPerFrame;

        // It is possible that there may be fewer input than output samples.
        int32_t samplesToProcess = std::min(numInputSamples, numOutputSamples);

        // this
        // am I devloper yet?
//        memcpy(outputData, inputData, samplesToProcess);
        process(inputFloats, numInputSamples);
        /* this is not supposed to be called directly.
         * hence the entire vringbuffer stuff
         */

        for (int32_t i = 0; i < samplesToProcess; i++) {
            *outputFloats++ = *inputFloats++  * 0.95; // do some arbitrary processing
        }

        // If there are fewer input samples then clear the rest of the buffer.
        int32_t samplesLeft = numOutputSamples - numInputSamples;
        for (int32_t i = 0; i < samplesLeft; i++) {
            *outputFloats++ = 0.0; // silence
        }

//        OUT ;
        return oboe::DataCallbackResult::Continue;
    }

    void process (const float * data, int samplesToProcess) {
//        float dummySecondChannel [200];// arbitrary!

        for (int i = 0 ; i < activePlugins ; i ++) {
            // shouldnt we connect ports in build_plugin_chain ?
            if (inputPorts [i] != -1)
                connect_port [i] (handle [i], inputPorts [i], (LADSPA_Data *) data);
            if (outputPorts [i] != -1)
                connect_port [i] (handle [i], outputPorts [i], (LADSPA_Data *) data);

//            LOGD("I/O Ports %d %d", inputPorts [i], outputPorts [i]);

            if (inputPorts2 [i] != -1)
                connect_port [i] (handle [i], inputPorts2 [i], (LADSPA_Data *) data);
            if (outputPorts2 [i] != -1)
                connect_port [i] (handle [i], outputPorts2 [i], (LADSPA_Data *) data);

//            LOGD("I/O 2 Ports %d %d", inputPorts2 [i], outputPorts2 [i]);

            if (run [i] == NULL)
                LOGF ("run %d is null", i);
            else
                run [i] (handle [i], samplesToProcess);
            /*
            if (set_run_adding_gain [i] != NULL)
                set_run_adding_gain [i] (handle [i], run_adding_gain [i]) ;
            if (run_adding [i] != NULL)
                run [i] (handle [i], samplesToProcess);
            */
        }


        if (recordingActive) {
            FileWriter::process(samplesToProcess, data);
        }

    }
};
#endif //SAMPLES_FULLDUPLEXPASS_H
