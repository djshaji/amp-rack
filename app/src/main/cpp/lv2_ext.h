//
// Created by djshaji on 5/3/24.
//

#ifndef AMP_RACK_LV2_EXT_H
#define AMP_RACK_LV2_EXT_H

#include "lv2.h"
#include "lv2/urid/urid.h"
#include <list>
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

#endif //AMP_RACK_LV2_EXT_H
