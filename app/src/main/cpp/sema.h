#ifndef _JACK_CAPTURE_SEMA_H
#define _JACK_CAPTURE_SEMA_H

#include <errno.h>

#ifdef __APPLE__
#include <mach/mach.h>
#include <sys/sysinfo.h>
#else
#include <semaphore.h>
#endif

#ifdef __APPLE__
 #define SEM_TYPE_T semaphore_t
 #define SEM_INIT(Sem) semaphore_create(mach_task_self(), &Sem, SYNC_POLICY_FIFO, 0)
 #define SEM_SIGNAL(Sem) semaphore_signal(Sem)
 #define SEM_WAIT(Sem)   {kern_return_t ret;while((ret=semaphore_wait(Sem))!=KERN_SUCCESS) print_message("Warning: semaphore_wait failed: %d",ret);}
#else
 #define SEM_TYPE_T sem_t
 #define SEM_INIT(Sem) sem_init(&Sem,0,0)
 #define SEM_SIGNAL(Sem) sem_post(&Sem)
 #define SEM_WAIT(Sem)   while(sem_wait(&Sem)==-1) LOGW("Warning: sem_wait failed: %s",strerror(errno));
#endif

#endif
