package com.geokey.mylibrary.masterlock.utility;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.masterlock.mlbluetoothsdk.models.audittrail.MLAuditTrailEntry;

import java.util.Date;

public class AuditTrailLogger {


    public static void logEntries(MLAuditTrailEntry[] entries, String tag, boolean serializeNulls) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                                        .registerTypeAdapter(Date.class, new UtcDateTypeAdapter())
                                        .setPrettyPrinting();
        if (serializeNulls) {
            gsonBuilder.serializeNulls();
        }

        Gson gson = gsonBuilder.create();

        for (MLAuditTrailEntry mlAuditTrailEntry : entries) {
            String mlAuditEntryJson = gson.toJson(mlAuditTrailEntry, MLAuditTrailEntry.class);
            Log.d(tag, "\t\n" + mlAuditEntryJson);
        }
    }
}
