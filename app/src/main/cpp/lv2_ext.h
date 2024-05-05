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

class URID {
public:
    std::list<std::string> urids;
} ;

int urid_map (URID * handle, const char * string) {
    handle -> urids.push_back(std::string (string));
    return handle -> urids.size();
}

void urid_unmap (URID * handle, int at) {
    handle -> urids.erase(std::next(handle -> urids.begin(), at));
}

int logger_printf (LV2_Log_Handle handle, LV2_URID type, const char* fmt, ...) ;

int logger_printf (LV2_Log_Handle handle, LV2_URID type, const char* fmt, va_list ap) {
    return LOGD(fmt, ap);
}

LV2_State_Status
store_callback(LV2_State_Handle handle,
               std::basic_string<char> key,
               const void*      value,
               size_t           size,
               uint32_t         type,
               uint32_t         flags)
{
    if ((flags & LV2_STATE_IS_POD)) {
        // We only care about POD since we're keeping state in memory only.
        // Disk or network use would also require LV2_STATE_IS_PORTABLE.
        std::map<std::string, std::string> state_map ;
        state_map [key] = std::string ((char *) value);
        return LV2_STATE_SUCCESS;;
    } else {
        return LV2_STATE_ERR_BAD_FLAGS; // Non-POD events are unsupported
    }
}


const void* retrieve_callback (LV2_State_Handle handle,
                                           uint32_t         key,
                                           size_t*          size,
                                           uint32_t*        type,
                                           uint32_t*        flags) {



}
#endif //AMP_RACK_LV2_EXT_H
