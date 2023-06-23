package com.geokey.mylibrary.masterlock;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.Profile;


import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;

// Utility Class for adding hard coded lock data to MasterLockViewModel
@RequiresApi(api = Build.VERSION_CODES.O)
public class MockLockDataProvider {
    public static  String MLLicense ="" ;
    public static  String MLLicensePassword ="" ;
    public static  String MLBaseURL ="" ;
    static Profile dummyProfile = new Profile("NotARealProfile|ThisWillNotWork",
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC),
            ZonedDateTime.now().plusDays(10));
    
    static LockData dummyLockData = new LockData("ABC123", 123456789, dummyProfile);
    static LockData dummyLockData1 = new LockData("ABC234", 123456789, dummyProfile);

    static LockData[] mockLockData = {
            dummyLockData, dummyLockData1
    };

    public static HashMap<String, LockData> getMockData() {

        HashMap<String, LockData> map = new HashMap<>();

        if (mockLockData.length < 1) return map;

        for (LockData item : mockLockData) {
            map.put(item.deviceIdentifier, item);
        }

        return map;
    }
}
