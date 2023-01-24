
/*
   An upwaker is something inbetween semaphores and conditionals.

   Similarly to semaphores, it guarantees upwaker_sleep() to wake up, even
   if the upwaker isn't currently sleeping (the next call to upwaker_sleep() will
   not actually go to sleep).

   And similarly to conditionals, it will wake the sleeper up _only_ one time
   when upwaker_wake_up() is called several times in a row, where
   upwaker_sleep() hasn't got a chance to wake up inbetween those calls.

   This implementation only supports one thread calling upwaker_sleep() and
   one thread calling upwaker_wake_up(). Using several sleepers and upwakers on
   the same upwaker_t variable has not been tested or given any thought about
   whether it would work.
*/

#ifndef UPWAKE_H
#define UPWAKE_H

#include <pthread.h>

#include "atomic.h"

typedef struct {
    DEFINE_ATOMIC(bool, please_wakeup);
    pthread_mutex_t mutex;
    pthread_cond_t cond;
} upwaker_t;

upwaker_t *create_upwaker(void);
void upwaker_sleep(upwaker_t *upwaker);
void upwaker_wake_up(upwaker_t *upwaker);

#endif // UPWAKE_H