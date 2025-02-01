//
// Created by djshaji on 5/3/24.
//

#ifndef AMP_RACK_LV2_EXT_H
#define AMP_RACK_LV2_EXT_H

#include "lv2.h"
#include "lv2/urid/urid.h"
#include "lv2/log/log.h"
#include "lv2/state/state.h"
#include "logging_macros.h"
#include <list>
#include <map>
#include <string>
#include "symap.h"

class URID {
public:
    std::list<std::string> urids;
} ;

LV2_URID_Map * ampMap_new () ;
LV2_URID ampMap_map (LV2_URID_Map_Handle handle, const char* uri) ;

int logger_printf (LV2_Log_Handle handle, LV2_URID type, const char* fmt, ...) ;
int lv2_urid_map (URID * handle, const char * string);
void lv2_urid_unmap (URID * handle, int at);
int logger_vprintf(LV2_Log_Handle handle,
               LV2_URID       type,
               const char*    fmt,
               va_list        ap);
#endif //AMP_RACK_LV2_EXT_H
