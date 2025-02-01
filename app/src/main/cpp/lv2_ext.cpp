//
// Created by djshaji on 5/3/24.
//

#include "lv2_ext.h"

int lv2_urid_map (URID * handle, const char * string) {
    IN
    auto f = std::find(handle->urids.begin(), handle->urids.end(),std::string (string)) ;
    if (f == handle->urids.end ()) {
        handle->urids.push_back(std::string(string));
        OUT
        return handle->urids.size();
    } else {
        OUT
        return std::distance(handle->urids.begin(), f) + 1 ;
    }
}

void lv2_urid_unmap (URID * handle, int at) {
    handle -> urids.erase(std::next(handle -> urids.begin(), at));
}


//int logger_printf (LV2_Log_Handle handle, LV2_URID type, const char* fmt, va_list ap) {
//    return LOGD(fmt, ap);
//}

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

    return nullptr;

}

int logger_vprintf(LV2_Log_Handle handle,
               LV2_URID       type,
               const char*    fmt,
               va_list        ap) {
    LOGD(fmt, ap);
    return 0 ;
}

int logger_printf(LV2_Log_Handle handle, LV2_URID type, const char *fmt, ...) {
    return 0;
}

LV2_URID ampMap_map (LV2_URID_Map_Handle handle, const char* uri) {
    IN
    std::vector <std::string> * v = static_cast<std::vector<std::string> *>(handle);
    auto i = std::find(v->begin(), v->end(),std::string (uri)) ;
    if (i == v->end ()) {
        v->push_back(std::string (uri));
        LOGD ("[map] %s -> %d", uri, v->size() + 1);
        OUT
        return v->size() + 1;
    } else {
        OUT
        LOGD ("[map] %s -> %d", uri, std::distance(v->begin(), i) + 2 );
        return std::distance(v->begin(), i) + 2 ;
    }
}

LV2_URID_Map * ampMap_new () {
    IN
    LV2_URID_Map * ampMap = (LV2_URID_Map *) malloc (sizeof (LV2_URID_Map)) ;
//    ampMap->handle = new std::vector <std::string> ();
//    ampMap->handle = symap_new();
    ampMap->map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(symap_map);
    return ampMap;
    OUT
}