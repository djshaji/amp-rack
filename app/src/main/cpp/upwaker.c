#ifdef __cplusplus
extern "C" {
#endif

//#include "logging_macros.h"
#define IN
#define OUT
#include "upwaker.h"
#include <stdlib.h>

upwaker_t *create_upwaker(void) {
    IN
  upwaker_t *upwaker = (upwaker_t *)calloc(1, sizeof(upwaker_t));
  ATOMIC_SET(upwaker->please_wakeup, false);
  pthread_mutex_init(&upwaker->mutex, NULL);
  pthread_cond_init(&upwaker->cond, NULL);
  OUT
  return upwaker;
}

void upwaker_sleep(upwaker_t *upwaker) {
    IN
  if (ATOMIC_GET(upwaker->please_wakeup) == false)
    pthread_cond_wait(&upwaker->cond, &upwaker->mutex);

  ATOMIC_SET(upwaker->please_wakeup, false);
  OUT
}

void upwaker_wake_up(upwaker_t *upwaker) {
    IN
  ATOMIC_SET(upwaker->please_wakeup, true);
  pthread_cond_broadcast(
          &upwaker->cond); // Must call, in case the other thread had finished the 'if' test before this function was called.
  // (i.e. it is not safe to trylock the mutex to check wether to call pthread_cond_broadcast).
  OUT
}

#ifdef __cplusplus
}
#endif
