//
// Created by djshaji on 3/5/24.
//

#include "LockFreeQueue.h"

LockFreeQueue<AudioBuffer *, LOCK_FREE_SIZE> LockFreeQueueManager::lockFreeQueue;
std::thread LockFreeQueueManager::fileWriteThread  ;

void LockFreeQueueManager::init (int _buffer_size) {
    buffer_size = _buffer_size ;
//    pAudioBuffer = static_cast<AudioBuffer *>(calloc(SPARE_BUFFERS, sizeof(AudioBuffer)));
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        pAudioBuffer [i] -> data = static_cast<float *>(malloc(buffer_size * sizeof(float)));
        pAudioBuffer [i] -> pos = 0 ;
    }

    buffer_counter = 0 ;
    functions_count = 0 ;

    ready = true ;
    fileWriteThread = std::thread (&LockFreeQueueManager::main, this);
}

void LockFreeQueueManager::add_function (int (* f) (float *, unsigned long)) {
    if (functions_count > MAX_FUNCTIONS) {
        HERE LOGE ("already have %d functions added to queue, cannot add any more!", MAX_FUNCTIONS);
        return ;
    }

    functions [functions_count] = reinterpret_cast<void (*)(float *, int)>(f);
    functions_count ++ ;
}

void LockFreeQueueManager::process (float * data, int samplesToProcess) {
    if (! ready)
        return ;

    for (int i = 0 ; i < samplesToProcess ; i ++) {
        pAudioBuffer [buffer_counter]->data [i] = data [i] ;
    }

    pAudioBuffer [buffer_counter]->pos = samplesToProcess;
    lockFreeQueue.push (pAudioBuffer [buffer_counter]);

    buffer_counter ++ ;
    if (buffer_counter > SPARE_BUFFERS) {
        buffer_counter = 0 ;
    }
}

void LockFreeQueueManager::main () {
    AudioBuffer * buffer ;
    while (lockFreeQueue.pop (buffer)) {
        for (int i = 0 ; i < MAX_FUNCTIONS ; i ++) {
            (functions [i])(buffer -> data, buffer -> pos) ;
        }
    }
}

void LockFreeQueueManager::quit () {
    fileWriteThread.join();
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        free (pAudioBuffer [i] -> data) ;
    }

    free (pAudioBuffer) ;
    ready = false ;
}
