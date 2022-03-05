#include "PluginControl.h"

LADSPA_Data PluginControl::control_rounding(LADSPA_Data _val)
{
    if (type == INT || type == TOGGLE)
        return nearbyintf(_val);
    return _val;
}

void PluginControl::setValue (float value) {
    val = value ;
}

void PluginControl::setSampleRate (unsigned long rate) {
    sample_rate = rate ;
}

PluginControl::PluginControl(const LADSPA_Descriptor *descriptor, int _port) {
    IN ;
    LOGD("Setting up control %d: %s for %s", _port, descriptor -> PortNames [_port], descriptor -> Name);
//        return;
    port = _port ;
//        ctrl = _control ;
    desc = & descriptor -> PortDescriptors [port] ;
    hint = & descriptor -> PortRangeHints [port] ;
    LADSPA_PortRangeHintDescriptor ladspaPortRangeHintDescriptor = hint -> HintDescriptor;
    LADSPA_Data lower_bound = hint -> LowerBound;
    LADSPA_Data upper_bound = hint -> UpperBound;
    name = descriptor -> PortNames [port] ;

    /* control->min, control->max */
    if (LADSPA_IS_HINT_SAMPLE_RATE(ladspaPortRangeHintDescriptor)) {
        lower_bound *= sample_rate;
        upper_bound *= sample_rate;
    }

    if ( LADSPA_IS_HINT_BOUNDED_BELOW(ladspaPortRangeHintDescriptor) &&
         LADSPA_IS_HINT_BOUNDED_ABOVE(ladspaPortRangeHintDescriptor) )
    {
        min = lower_bound;
        max = upper_bound;
    }
    else if (LADSPA_IS_HINT_BOUNDED_BELOW(ladspaPortRangeHintDescriptor)) {
        min = lower_bound;
        max = 1.0;
    }
    else if (LADSPA_IS_HINT_BOUNDED_ABOVE(ladspaPortRangeHintDescriptor)) {
        min = 0.0;
        max = upper_bound;
    }
    else {
        min = -1.0;
        max = 1.0;
    }
    /* control->def */
//        return ;
    if (LADSPA_IS_HINT_HAS_DEFAULT(ladspaPortRangeHintDescriptor)) {
        /// TODO: Free this memory
        /// this causes memory corruption
//            def = (LADSPA_Data *)malloc(sizeof(LADSPA_Data));
//            def = new float ();
//            return ;

//            if (def == NULL) {
//                LOGE("Failed to allocate memory!");
//                return ;
//                OUT;
//            }

        def = &default_value ;
        switch (ladspaPortRangeHintDescriptor & LADSPA_HINT_DEFAULT_MASK) {
            case LADSPA_HINT_DEFAULT_MINIMUM:
                *def = lower_bound;
                break;
            case LADSPA_HINT_DEFAULT_LOW:
                *def = lower_bound * 0.75 + upper_bound * 0.25;
                break;
            case LADSPA_HINT_DEFAULT_MIDDLE:
                *def = lower_bound * 0.5 + upper_bound * 0.5;
                break;
            case LADSPA_HINT_DEFAULT_HIGH:
                *def = lower_bound * 0.25 + upper_bound * 0.75;
                break;
            case LADSPA_HINT_DEFAULT_MAXIMUM:
                *def = upper_bound;
                break;
            case LADSPA_HINT_DEFAULT_0:
                *def = 0.0;
                break;
            case LADSPA_HINT_DEFAULT_1:
                *def = 1.0;
                break;
            case LADSPA_HINT_DEFAULT_100:
                *def = 100.0;
                break;
            case LADSPA_HINT_DEFAULT_440:
                *def = 440.0;
                break;
            default:
                free(def), def = NULL;
                LOGV("[plugin] %s has no defaults", name);
        }
    }
    else
        def = NULL;

    /* Check the default */
    if (def) {
        if (*def < min) {
            LOGD("[plugin] %s: default smaller than the minimum", name);
            *def = min;
        }
        if (*def > max) {
            LOGD("[plugin] %s: default greater than the maximum\n", name);
            *def = max;
        }
    }

    /* control->inc & Overrides */
    if (LADSPA_IS_HINT_TOGGLED(ladspaPortRangeHintDescriptor)) {
        min = 0.0;
        max = 1.0;
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = TOGGLE;
        if (def) * def = nearbyintf(*def);
    }
    else if (LADSPA_IS_HINT_INTEGER(ladspaPortRangeHintDescriptor)) {
        min = nearbyintf(min);
        max = nearbyintf(max);
        inc.fine = 1.0;
        inc.coarse = 1.0;
        type = INT ;
        if (def) *def = nearbyintf(*def);
    }
    else {
        inc.fine = (max - min) / 500;
        inc.coarse = (max - min) / 50;
        type = FLOAT;
    }

    /* control->sel, control->val */
    if (def)
        sel = *def;
    else
        sel = min;
    val = sel;

    if (! def)
        LOGD("[plugin] %s: found control %s <%f - %f> no default value", descriptor ->Name, name, lower_bound, upper_bound);
    else
        LOGD("[plugin] %s: found control %s <%f - %f> default value %f", descriptor ->Name, name, lower_bound, upper_bound, *def);
    OUT ;
}

LADSPA_Data PluginControl::getMin () {
    return min ;
}

LADSPA_Data PluginControl::getMax () {
    return max ;
}

LADSPA_Data PluginControl::getDefault () {
    return default_value ;
}

LADSPA_Data PluginControl::getValue () {
    return val ;
}

void PluginControl::print () {
    LOGD(
            "\t\t\t-------| Control: %s [%d] |-----------\n\t\t"
            "Current: %f\tDefault: %f\tMin: %f\tMax: %f",
            name, port,
            val, def, min, max
            ) ;
}