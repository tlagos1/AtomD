package com.sorbonne.atom_d.services;

import android.content.Context
import java.text.DateFormat;
import java.util.Date;

import fr.hopcast.app.demo.R;

class ServiceUtils {

//    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";
//
    companion object {
        fun getDeviceToDeviceTitle(context: Context): String {
            return context.getString(
                R.string.service_updated,
                DateFormat.getDateTimeInstance().format(Date())
            )
        }
    }
}
