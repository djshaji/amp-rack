#ifdef __cplusplus
extern "C" {
#endif

/*
  Kjetil Matheussen, 2010-2011.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/



/*
  vringbuffer is convenient to use when sending data from a realtime
  thread to a non-realtime thread.
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "logging_macros.h"
#include "ringbuffer.h"
#include <unistd.h>



/*
  Increaser:
    while true:
      if (ringbuffer_size(used)>...)
         buffer=my_calloc(1,sizeof(buffer))
         write_ringbuffer(free_new,buffer)
      sleep(...)
  ->
  Increaser:
    while true:
      if (ringbuffer_size(used)>...)
         buffer=my_calloc(1,sizeof(buffer))
         increase_vringbuffer(vringbuffer_t *vrb,1,&buffer)
      sleep(...)


  Disk:
    while true:
      buffer=read_ringbuffer(used)
      write_to_disk(buffer)
      write_ringbuffer(free_used,buffer)
  ->
  Disk:
    while true:
      buffer=read_vringbuffer(vringbuffer)
      write_to_disk(buffer)
      return_vringbuffer(vringbuffer,buffer)


  Process(data):
    if(ringbuffer_size(free_new)>0)
       buffer = read_ringbuffer(free_new)
    else
       buffer = read_ringbuffer(free_used)
    fill_buffer(buffer,data)
    write_ringbuffer(used,buffer)
  ->
  Process(data):
    buffer = read_vringbuffer(vringbuffer);
    fill_buffer(buffer,data);
    return_buffer(vringbuffer,buffer)


vringbuffer_t *vringbuffer_create (int num_elements_during_startup, int max_num_elements);

void  vringbuffer_increase        (vringbuffer_t *vrb, int num_elements, void **elements);

int   vrginbuffer_size            (vringbuffer_t *vrb);

void* vringbuffer_get_reading     (vringbuffer_t *vrb);
void  vringbuffer_return_reading  (vringbuffer_t *vrb, void *data);
int   vringbuffer_reading_size    (vringbuffer_t *vrb);

void* vringbuffer_get_writing     (vringbuffer_t *vrb);
void  vrginbuffer_return_writing  (vringbuffer_t *vrb, void *data);
int   vringbuffer_writing_size    (vringbuffer_t *vrb);
*/


#include "vringbuffer.h"

////////////////// Implementation ///////////////////////////////////


static void *my_malloc(size_t size1, size_t size2) {
    size_t size = size1 * size2;
    void *ret = malloc(size);
    if (ret == NULL) {
        fprintf(stderr, "\nOut of memory. Try a smaller buffer.\n");
        return NULL;
    }

    // Touch all pages. (earlier all memory was nulled out, but that puts a strain on the memory bus)
    # ifdef __linux__
    {
        long pagesize = sysconf(_SC_PAGESIZE);
        char *cret = ret;
        size_t i = 0;
        for (i = 0; i < size; i += pagesize)
            cret[i] = 0;
    }
    # endif

    return ret;
}


static bool vringbuffer_increase_writer1(vringbuffer_t *vrb, int num_elements, bool first_call) {

    if (num_elements + vrb->curr_num_elements > vrb->max_num_elements)
        num_elements = vrb->max_num_elements - vrb->curr_num_elements;

    if (num_elements == 0)
        return true;

    vringbuffer_list_t *element = (vringbuffer_list_t *)my_malloc(1, sizeof(vringbuffer_list_t) +
                                               (num_elements * vrb->element_size));
    if (element == NULL)
        return false;

    pthread_mutex_lock(&vrb->increase_lock);
    {

        element->next = vrb->allocated_mem;
        vrb->allocated_mem = element;

        char *das_buffer = (char *) (element + 1);

        if (first_call) {
            // Make sure at least a certain amount of the buffer is in a cache.
            // Might create a less shocking startup.
            int num = 8;
            if (num > num_elements)
                num = num_elements;
            memset(das_buffer, 0, num * vrb->element_size);
        }

        for (int i = 0; i < num_elements; i++) {
            char *pointer = das_buffer + (i * vrb->element_size);
            #ifdef __linux__
            jack_ringbuffer_write(vrb->for_writer1,
                                  (char *) &pointer,
                                  sizeof(char *));
            # endif                      
        }

        vrb->curr_num_elements += num_elements;

    }
    pthread_mutex_unlock(&vrb->increase_lock);

    return true;
}


vringbuffer_t *
vringbuffer_create(int num_elements_during_startup, int max_num_elements, size_t element_size) {
    IN LOGD("vringbuffer_create(int num_elements_during_startup, int max_num_elements, size_t element_size) %d, %d, %d", num_elements_during_startup, max_num_elements, element_size);
    //fprintf(stderr,"Creating %d %d %d\n",num_elements_during_startup,max_num_elements,element_size);
    vringbuffer_t *vrb = (vringbuffer_t *)calloc(1, sizeof(struct vringbuffer_t));

#ifdef __linux__
    vrb->for_writer1 = jack_ringbuffer_create(sizeof(void *) * max_num_elements);
    vrb->for_writer2 = jack_ringbuffer_create(sizeof(void *) * max_num_elements);
    vrb->for_reader = jack_ringbuffer_create(sizeof(void *) * max_num_elements);
#endif
    vrb->element_size = element_size;
    vrb->max_num_elements = max_num_elements;

    vrb->please_stop = false;

    pthread_mutex_init(&vrb->increase_lock, NULL);

    if (vringbuffer_increase_writer1(vrb, num_elements_during_startup, true) == false)
        return NULL;

    vrb->receiver_trigger = create_upwaker();
    vrb->autoincrease_trigger = create_upwaker();

    OUT
    return vrb;
}

void vringbuffer_stop_callbacks(vringbuffer_t *vrb) {
    IN
    vrb->please_stop = true;

    if (vrb->autoincrease_callback != NULL) {
        upwaker_wake_up(vrb->autoincrease_trigger);
        pthread_join(vrb->autoincrease_thread, NULL);
        vrb->autoincrease_callback = NULL;
    }

    if (vrb->receiver_callback != NULL) {
        upwaker_wake_up(vrb->receiver_trigger);
        pthread_join(vrb->receiver_thread, NULL);
        vrb->receiver_callback = NULL;
    }

    OUT
}

void vringbuffer_delete(vringbuffer_t *vrb) {

    vringbuffer_stop_callbacks(vrb);

    while (vrb->allocated_mem != NULL) {
        vringbuffer_list_t *next = vrb->allocated_mem->next;
        free(vrb->allocated_mem);
        vrb->allocated_mem = next;
    }
#ifdef __linux__
    jack_ringbuffer_free(vrb->for_writer1);
    jack_ringbuffer_free(vrb->for_writer2);
    jack_ringbuffer_free(vrb->for_reader);
#endif
    free(vrb);
}


static void *autoincrease_func(void *arg) {
    IN
    vringbuffer_t *vrb = (vringbuffer_t *) arg;
    vrb->autoincrease_callback(vrb, true, 0, 0);
    SEM_SIGNAL(vrb->autoincrease_started);
    while (vrb->please_stop == false) {
        int reading_size = vringbuffer_reading_size(vrb);
        int writing_size = vringbuffer_writing_size(vrb);

        int num_new_elements = vrb->autoincrease_callback(vrb, false, reading_size, writing_size);
        if (num_new_elements > 0)
            vringbuffer_increase_writer1(vrb, num_new_elements, false);

        if (vrb->autoincrease_interval == 0)
            upwaker_sleep(vrb->autoincrease_trigger);
        else
            usleep(vrb->autoincrease_interval);
    }

    OUT
    return NULL;
}


void vringbuffer_trigger_autoincrease_callback(vringbuffer_t *vrb) {
    IN
    upwaker_wake_up(vrb->autoincrease_trigger);
    OUT
}

void vringbuffer_set_autoincrease_callback(vringbuffer_t *vrb,
                                           Vringbuffer_autoincrease_callback callback,
                                           useconds_t interval) {
    IN
    vrb->autoincrease_callback = callback;
    vrb->autoincrease_interval = interval;

    SEM_INIT(vrb->autoincrease_started);

    pthread_create(&vrb->autoincrease_thread, NULL, autoincrease_func, vrb);

    SEM_WAIT(vrb->autoincrease_started);
    OUT
}

void vringbuffer_increase(vringbuffer_t *vrb, int num_elements, void **elements) {
    IN
    if (num_elements + vrb->curr_num_elements > vrb->max_num_elements)
        num_elements = vrb->max_num_elements - vrb->curr_num_elements;

    if (num_elements == 0) {
        OUT
        return;
    }

    pthread_mutex_lock(&vrb->increase_lock);
    {
#ifdef __linux__
        jack_ringbuffer_write(vrb->for_writer1, (char *) elements, sizeof(void *) * num_elements);
        vrb->curr_num_elements += num_elements;
#endif
    }
    pthread_mutex_unlock(&vrb->increase_lock);
    OUT
}


/* Non-realtime */

void *vringbuffer_get_reading(vringbuffer_t *vrb) {
    void *ret = NULL;
    IN
    #ifdef __linux__
    jack_ringbuffer_read(vrb->for_reader, (char *) &ret, sizeof(void *));
    #endif
    OUT
    return ret;
}

void vringbuffer_return_reading(vringbuffer_t *vrb, void *data) {
    IN
    #ifdef __linux__
    jack_ringbuffer_write(vrb->for_writer2, (char *) &data, sizeof(void *));
    #endif
    OUT
}

int vringbuffer_reading_size(vringbuffer_t *vrb) {
    IN OUT
    #ifdef __linux__
    return (jack_ringbuffer_read_space(vrb->for_reader) / sizeof(void *));
    #endif
    return 0 ;
}


static void *receiver_func(void *arg) {
    IN
    vringbuffer_t *vrb = (vringbuffer_t *)arg;
//    LOGD("[ringbuffer id] %d", gettid ());

    vrb->receiver_callback(vrb, true, NULL);
    SEM_SIGNAL(vrb->receiver_started);

    void *buffer = NULL;

    while (vrb->please_stop == false) {
        upwaker_sleep(vrb->receiver_trigger);
//        LOGD("[ringbuffer id] %d", gettid ());

        while (vringbuffer_reading_size(vrb) > 0) {
//            LOGD("looping reciever");

            if (buffer == NULL)
                buffer = vringbuffer_get_reading(vrb);

            if (vrb->receiver_callback(vrb, false, buffer) == VRB_CALLBACK_DIDNT_USE_BUFFER) {
//                LOGD("reciever break");
                break;
            }

            vringbuffer_return_reading(vrb, buffer);
            buffer = NULL;
        }
    }

    OUT
    return NULL;
}

void vringbuffer_set_receiver_callback(vringbuffer_t *vrb,
                                       Vringbuffer_receiver_callback receiver_callback) {
    IN
    vrb->receiver_callback = receiver_callback;

    SEM_INIT(vrb->receiver_started);

    pthread_create(&vrb->receiver_thread, NULL, receiver_func, vrb);

    SEM_INIT(vrb->receiver_started);
    OUT
}


/* Realtime */

void *vringbuffer_get_writing(vringbuffer_t *vrb) {
    IN
    void *ret = NULL;
    #ifdef __linux__
    if (jack_ringbuffer_read(vrb->for_writer2, (char *) &ret, sizeof(void *)) ==
        0) // Checking writer2 first since that memory is more likely to already be in the cache.
        jack_ringbuffer_read(vrb->for_writer1, (char *) &ret, sizeof(void *));
    #endif
    OUT
    return ret;
}

void vringbuffer_return_writing(vringbuffer_t *vrb, void *data) {
    IN
    vringbuffer_buffer_t  * bf = (vringbuffer_buffer_t *) data ;
//    vrb->receiver_callback(vrb, false, data) ;
//    return;
#ifdef __linux__
    jack_ringbuffer_write(vrb->for_reader, (char *) &data, sizeof(void *));
#endif    
    upwaker_wake_up(vrb->receiver_trigger);
    OUT
}

int vringbuffer_writing_size(vringbuffer_t *vrb) {
    IN OUT
    #ifdef __linux__
        return
            ((jack_ringbuffer_read_space(vrb->for_writer1) +
              jack_ringbuffer_read_space(vrb->for_writer2))
             / sizeof(void *));
    #else return 0;
    #endif
}

#ifdef __cplusplus
}
#endif
