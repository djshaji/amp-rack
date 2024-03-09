//
// Created by djshaji on 3/5/24.
//

#include "LockFreeQueue.h"

LockFreeQueue<AudioBuffer *, LOCK_FREE_SIZE> LockFreeQueueManager::lockFreeQueue;
std::thread LockFreeQueueManager::fileWriteThread  ;

void LockFreeQueueManager::init (int _buffer_size) {
    IN
    buffer_size = _buffer_size ;
//    pAudioBuffer = static_cast<AudioBuffer *>(calloc(SPARE_BUFFERS, sizeof(AudioBuffer)));
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        pAudioBuffer [i] = static_cast<AudioBuffer *>(malloc(sizeof(AudioBuffer)));
        pAudioBuffer [i] -> data = static_cast<float *>(malloc(buffer_size * sizeof(float)));
        pAudioBuffer [i] -> raw = static_cast<float *>(malloc(buffer_size * sizeof(float)));
        pAudioBuffer [i] -> pos = 0 ;
    }

    buffer_counter = 0 ;

    ready = true ;
    fileWriteThread = std::thread (&LockFreeQueueManager::main, this);
    LOGD("[LockFreeQueue thread id] %d", gettid ());

    OUT
}

void LockFreeQueueManager::add_function (int (* f) (AudioBuffer *)) {
    IN
    if (functions_count > MAX_FUNCTIONS) {
        HERE LOGE ("already have %d functions added to queue, cannot add any more!", MAX_FUNCTIONS);
        OUT return ;
    }

    functions [functions_count] = reinterpret_cast<void (*)(AudioBuffer *)>(f);
    functions_count ++ ;
    OUT
}

void LockFreeQueueManager::process (float * raw, float * data, int samplesToProcess) {
//    IN
    if (! ready) {
        OUT
//        HERE LOGD("not ready");
        return;
    }

    for (int i = 0 ; i < samplesToProcess ; i ++) {
        pAudioBuffer [buffer_counter]->raw [i] = raw [i] ;
        pAudioBuffer [buffer_counter]->data [i] = data [i] ;
    }

    pAudioBuffer [buffer_counter]->pos = samplesToProcess;
    lockFreeQueue.push (pAudioBuffer [buffer_counter]);

    buffer_counter ++ ;
    if (buffer_counter >= SPARE_BUFFERS) {
        buffer_counter = 0 ;
    }

//    OUT
}

void LockFreeQueueManager::main () {
    IN
    AudioBuffer * buffer ;
    while (ready) {
        if (lockFreeQueue.pop (buffer)) {
            for (int i = 0; i < functions_count; i++) {
                (functions[i])(buffer);
            }
        }

//        std::this_thread::sleep_for(std::chrono::milliseconds (1));

    }
    OUT
}

void LockFreeQueueManager::quit () {
    IN
    ready = false ;
    fileWriteThread.join();
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        free (pAudioBuffer [i] -> data) ;
        free (pAudioBuffer [i] -> raw) ;
        free (pAudioBuffer [i]) ;
    }

    OUT
}
