//
// Created by djshaji on 3/5/24.
//

#include "LockFreeQueue.h"

LockFreeQueue<AudioBuffer *, LOCK_FREE_SIZE> LockFreeQueueManager::lockFreeQueue;
std::thread LockFreeQueueManager::fileWriteThread  ;
bool LockFreeQueueManager::ready = false;
void (* LockFreeQueueManager::functions [MAX_FUNCTIONS])(AudioBuffer *) ;
AudioBuffer * LockFreeQueueManager::pAudioBuffer [SPARE_BUFFERS]; 
int  LockFreeQueueManager::buffer_counter ;

void LockFreeQueueManager::init (int _buffer_size) {
    IN
    if (buffer_size < _buffer_size) {
        if (pAudioBuffer[0] != nullptr) {
            for (int i = 0; i < SPARE_BUFFERS; i++) {
                free(pAudioBuffer[i]->data);
                free(pAudioBuffer[i]->raw);
                free(pAudioBuffer[i]);
            }
        }

        pAudioBuffer[0] = nullptr ;
    }

    buffer_size = _buffer_size ;
    if (pAudioBuffer [0] == nullptr) {
        //    pAudioBuffer = static_cast<AudioBuffer *>(calloc(SPARE_BUFFERS, sizeof(AudioBuffer)));
        for (int i = 0; i < SPARE_BUFFERS; i++) {
            pAudioBuffer[i] = static_cast<AudioBuffer *>(malloc(sizeof(AudioBuffer)));
            pAudioBuffer[i]->data = static_cast<float *>(malloc(buffer_size * sizeof(float)));
            pAudioBuffer[i]->raw = static_cast<float *>(malloc(buffer_size * sizeof(float)));
            pAudioBuffer[i]->pos = 0;
        }
    }
//        for (int x = 0 ; x < buffer_size ; x ++) {
//            pAudioBuffer [i]->data [x] = 0.0f ;
//            pAudioBuffer [i]->raw [x] = 0.0f ;
//        }

    buffer_counter = 0 ;

    ready = true ;
    if (! thread_started) {
        fileWriteThread = std::thread(&LockFreeQueueManager::main, this);
        thread_started = true;
    }

    LOGD("[LockFreeQueue thread id] %d", gettid ());

//    attach();
    OUT
}

void LockFreeQueueManager::pop_function () {
    functions [functions_count] = nullptr ;
    functions_count -- ;
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
//        OUT
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
//    int size = 0 ;
    while (ready) {
        while (lockFreeQueue.pop (buffer)) {
//            if (size < lockFreeQueue.size())
//                size = lockFreeQueue.size();
//            LOGI("[lockfreequeue] peak size: %d", size);
            for (int i = 0; i < functions_count; i++) {
                (functions[i])(buffer);
            }
        }

        std::this_thread::sleep_for(std::chrono::milliseconds (100));
    }
    OUT
}

void LockFreeQueueManager::quit () {
    IN
//    ready = false ;
    AudioBuffer * buffer ;
    while (lockFreeQueue.pop(buffer))
        1 ; // TIL this can also be a statement

    //    detach();
//    fileWriteThread.join();
//    for (int i = 0; i < SPARE_BUFFERS; i ++) {
//        free (pAudioBuffer [i] -> data) ;
//        free (pAudioBuffer [i] -> raw) ;
//        free (pAudioBuffer [i]) ;
//    }

    OUT
}

void LockFreeQueueManager::detach () {
    IN

    JNIEnv * _env = NULL;
    int status = vm->GetEnv((void**)&_env, JNI_VERSION_1_6);
    if(status > 0) {
        LOGW("detaching thread %d", gettid());
        vm->DetachCurrentThread();
    }

    OUT
}

void LockFreeQueueManager::attach () {
    IN

    JNIEnv *env;
    int status = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if(status < 0) {
        status = vm->AttachCurrentThread(&env, NULL);
        if(status < 0) {
            LOGE("LockFreeQueueManager: failed to attach thread %d", gettid());
        }
    } else
        LOGD("[getenv] attached thread id %d", gettid());

    OUT
}
