//
// Created by djshaji on 3/5/24.
//

#include "LockFreeQueue.h"

LockFreeQueue<AudioBuffer *, LOCK_FREE_SIZE> LockFreeQueueManager::lockFreeQueue;
std::thread LockFreeQueueManager::fileWriteThread  ;

LockFreeQueueManager::init (int _buffer_size) {
    buffer_size = _buffer_size ;
    pAudioBuffer = calloc (SPARE_BUFFERS, sizeof (AudioBuffer));
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        pAudioBuffer [i] -> data = malloc (buffer_size * sizeof (float)) ;
        pAudioBuffer [i] -> pos = 0 ;
    }

    buffer_counter = 0 ;
    functions_counter = 0 ;

    fileWriteThread = std::thread (&LockFreeQueueManager::main, this);

}

LockFreeQueueManager::add_function (void (* f) (float *, int)) {
    if (functions_counter > MAX_FUNCTIONS) {
        HERE LOGE ("already have %d functions added to queue, cannot add any more!", MAX_FUNCTIONS);
        return ;
    }

    functions [functions_counter] = f ;
    functions_counter ++ ;
}

LockFreeQueueManager::process (AudioBuffer * buffer) {
    for (int i = 0 ; i < buffer -> pos ; i ++) {
        pAudioBuffer [buffer_counter][i] = buffer [i] ;
    }

    lockFreeQueue.push (pAudioBuffer [buffer_counter]);

    buffer_counter ++ ;
    if (buffer_counter > SPARE_BUFFERS) {
        buffer_counter = 0 ;
    }
}

LockFreeQueueManager::main () {
    AudioBuffer * buffer ;
    while (lockFreeQueue.pop (buffer)) {
        for (int i = 0 ; i < MAX_FUNCTIONS ; i ++) {
            (functions [i])(buffer -> data, buffer -> pos) ;
        }
    }
}

LockFreeQueueManager::quit () {
    fileWriteThread.join();
    for (int i = 0; i < SPARE_BUFFERS; i ++) {
        free (pAudioBuffer [i] -> data) ;
    }

    free (pAudioBuffer) ;
}
