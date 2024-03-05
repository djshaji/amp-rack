
#include <stdbool.h>
#include <unistd.h>

#include <pthread.h>

#include "ringbuffer.h"
#include "upwaker.h"
#include "sema.h"

#ifndef VRINGBUFFER_H
#define VRINGBUFFER_H

typedef struct vbuffer_t{
    int overruns;
    int pos;
//    float data[];
    float *data;
    int size ;
} vringbuffer_buffer_t;

enum vringbuffer_receiver_callback_return_t {
    VRB_CALLBACK_DIDNT_USE_BUFFER,
    VRB_CALLBACK_USED_BUFFER
};

struct vringbuffer_t;

typedef  int (*Vringbuffer_autoincrease_callback) (struct vringbuffer_t *vrb, bool first_time, int reading_size, int writing_size);
typedef  enum vringbuffer_receiver_callback_return_t (*Vringbuffer_receiver_callback) (struct vringbuffer_t *vrb, bool first_time, void *buffer);

typedef struct vringbuffer_list_t{
    struct vringbuffer_list_t *next;
}vringbuffer_list_t;

typedef struct vringbuffer_t{
    jack_ringbuffer_t *for_writer1;
    jack_ringbuffer_t *for_writer2;
    jack_ringbuffer_t *for_reader;

    size_t element_size;

    int curr_num_elements;
    int max_num_elements;
    vringbuffer_list_t *allocated_mem;

    pthread_mutex_t increase_lock;

    bool please_stop;

    // Receiver callback
    pthread_t receiver_thread;
    upwaker_t *receiver_trigger;
    SEM_TYPE_T receiver_started;
    Vringbuffer_receiver_callback receiver_callback;

    // Autoincrease callback
    pthread_t autoincrease_thread;
    upwaker_t *autoincrease_trigger;
    SEM_TYPE_T autoincrease_started;
    Vringbuffer_autoincrease_callback autoincrease_callback;
    useconds_t autoincrease_interval;
}vringbuffer_t;




vringbuffer_t* vringbuffer_create	(int num_elements_during_startup, int max_num_elements,size_t element_size);
void vringbuffer_stop_callbacks(vringbuffer_t* vrb);
void vringbuffer_delete(vringbuffer_t* vrb);

void	vringbuffer_increase		(vringbuffer_t *vrb, int num_elements, void **elements);

// Creates a new autoincrease thread. The callback is called each 'interval' microseconds.
// The callback does not have to allocate memory itself, but instead the function returns the number of elements (not bytes) to allocate.
// The callback is also responsible for setting nicety and priority of the thread. This should be done when called the first time (i.e. when first_time==true).
//
// The return value for autoincrease_callback is also ignored the first time, and both reading_size and writing_size will have the values 0.
// Furthermore, the first time autoincrease_callback is called, it is called before vringbuffer_set_autoincrease_callback() returns
//
// An autoincrease callback could be implemented like this:
/*
  static int autoincrease_callback(vringbuffer_t *vrb, bool first_call, size_t reading_size, size_t writing_size){
     if(first_call){
       set_high_priority();
       return 0;
     }
     if(writing_size < 1024)
       return 2;
     return 0;
  }

  int main(){
    ...
    vringbuffer_t *vrg = vrngbuffer_create(quite_large_number,very_large_number,element_size);
    vringbuffer_set_autoincrease_callback(vrg,autoincrease_callback,quite_often);
    ...
  }
*/

// Set interval==0 if you wan't the callback to be triggered if using the trigger_autoincrease_callback function.
void    vringbuffer_set_autoincrease_callback  (vringbuffer_t *vrg, Vringbuffer_autoincrease_callback callback, useconds_t interval);

// Don't call unless interval==0
void vringbuffer_trigger_autoincrease_callback(vringbuffer_t *vrb);

void*	vringbuffer_get_reading		(vringbuffer_t *vrb);
void    vringbuffer_return_reading	(vringbuffer_t *vrb, void *data);
// Returns available number of elements in the ringbuffer
int	vringbuffer_reading_size	(vringbuffer_t *vrb);

void vringbuffer_set_receiver_callback(vringbuffer_t *vrb,Vringbuffer_receiver_callback receiver_callback);

void*	vringbuffer_get_writing		(vringbuffer_t *vrb);
void	vringbuffer_return_writing	(vringbuffer_t *vrb, void *data);
// Returns available number of elements in the ringbuffer
int	vringbuffer_writing_size	(vringbuffer_t *vrb);


#endif