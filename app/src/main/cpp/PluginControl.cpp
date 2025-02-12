#include "PluginControl.h"

LADSPA_Data PluginControl::control_rounding(LADSPA_Data _val)
{
    if (type == INT || type == TOGGLE)
        return nearbyintf(_val);
    return _val;
}

void PluginControl::setValue (float value) {
//    val = value ;
//    if (isLogarithmic) {
//        float ex = expf(value) ;
//        LOGD ("is log %f -> %f", value, ex);
//        *def = ex;
//    }
//    else {
    *def = value;
//    }
}

void PluginControl::setPresetValue (float value) {
//    val = value ;
    presetValue = value;
}


void PluginControl::setSampleRate (unsigned long rate) {
    sample_rate = rate ;
}

PluginControl::PluginControl(const LADSPA_Descriptor *descriptor, int _port) {
    IN ;
    //~ LOGD("Setting up control %d: %s for %s", _port, descriptor -> PortNames [_port], descriptor -> Name);
//        return;
    port = _port ;
//        ctrl = _control ;
    desc = & descriptor -> PortDescriptors [port] ;
    hint = & descriptor -> PortRangeHints [port] ;
    LADSPA_PortRangeHintDescriptor ladspaPortRangeHintDescriptor = hint -> HintDescriptor;
    LADSPA_Data lower_bound = hint -> LowerBound;
    LADSPA_Data upper_bound = hint -> UpperBound;
    name = descriptor -> PortNames [port] ;

    //~ LOGD("[control] %s", name);

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
    def = (float *) malloc (sizeof (long int));
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

//        def = &default_value ;
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
//                free(def), def = NULL;
                *def = -678 ;
                LOGD("[plugin] %s has no defaults", name);
        }
    }
    else
        *def = -678;

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

    if (*def == -678) {
        LOGD("[plugin] %s: found control %s <%f - %f> no default value", descriptor->Name, name,
             lower_bound, upper_bound);
        *def = 0 ; /// aaaargh
    }
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
    /*  I've forgotten how I use this, but I don't *set* this variable.
     *  So why do I *get* this?
     *
     *  I wonder what changing this will break?
     */
//    return val ;
    return *def;
}

void PluginControl::print () {
    LOGD(
            "\t\t\t-------| Control: %s [%d] |-----------\n\t\t"
            "Current: %f\tDefault: %f\tMin: %f\tMax: %f",
            name, port,
            val, def, min, max
            ) ;
}

void PluginControl::freeMemory () {
    IN
    free (def);
    if (name_allocated)
        free ((void *) name);
    OUT
}

PluginControl::PluginControl(const LV2_Descriptor *descriptor, nlohmann::json j) {
    IN ;
    if (j.contains("logarithmic") and j.find ("logarithmic").value ()) {
        isLogarithmic = true;
    }

    int _port = j .find("index").value();
    name = strdup (j.find("name").value().dump().c_str());
    name_allocated = true ;
    lv2_name = j.find("name").value();
    port = _port ;

    if (j.find("InputPort").value() == true && j.find("ControlPort").value() == true) {
        if (j.find("toggle").value() == true)
            type = TOGGLE;
        else
            type = FLOAT;
        //~ LOGD("Setting up control %d: %s for %s", _port, descriptor -> URI , name);
        std::string _min  ;
        std::string _max  ;
        LADSPA_Data lower_bound  ;
        LADSPA_Data upper_bound ;

        if (j.find("maximum")->is_string()) {
            _max = j.find("maximum").value();
            upper_bound = std::stof (_max);
        } else {
            upper_bound = j.find("maximum").value();
        }

        if (j.find("minimum")->is_string()) {
            _min = j.find("minimum").value();
            LOGD("minimum: %s is string", _min.c_str());
            lower_bound = std::stof (_min);
        } else {
            lower_bound = j.find("minimum").value();
        }

        //~ LOGD("[control] %s", name);

        min = lower_bound;
        max = upper_bound;

//        if (isLogarithmic) {
//            min = logf (lower_bound);
//            max = logf (upper_bound);
//        }


        def = (float *) malloc (sizeof (long int));
        std::string _def ;
        if (j.find("default")->is_string()) {
            LOGD("plugin default is string ..");
            *def = std::stof(std::string(j.find("default").value()));
        } else {
            *def = j.find("default").value();
//            if (isLogarithmic)
//                * def = logf (* def);
        }

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

        /* control->sel, control->val */
        if (def)
            sel = *def;
        else
            sel = min;
        val = sel;

        //~ LOGD("[LV2 Plugin] %s: found control %s <%f - %f> default value %f",
             //~ descriptor ->URI, name, lower_bound, upper_bound, *def);
    } else if (j.find("InputPort").value() == true && j.find("AtomPort").value() == true) {
        type = ATOM ;
        lv2AtomSequence = (LV2_Atom_Sequence *) malloc (j .find("minimumSize").value());
        def = (float *) malloc (sizeof (long int)); // aye, compatibility!
        min = 0;
        max = 1;
    }

    OUT ;
}
