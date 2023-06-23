package com.geokey.mylibrary.masterlock.bluetoothdelegates.models;


import com.geokey.mylibrary.masterlock.online.models.FirmwareVersionItem;

import java.util.ArrayList;

public class LockData {
    public String deviceIdentifier;
    public Profile profile = null;
    public Integer firmwareVersion = null;
    public ArrayList<FirmwareVersionItem> firmwareVersions = new ArrayList<>();
    public boolean isRequestingProfile = false;
    public String modelNumber = "";

    public LockData() {}

    public LockData(String deviceIdentifier, boolean isRequestingProfile) {
        this.deviceIdentifier = deviceIdentifier;
        this.isRequestingProfile = isRequestingProfile;
    }

    public LockData(String deviceIdentifier, int firmwareVersion) {
        this.deviceIdentifier = deviceIdentifier;
        this.firmwareVersion = firmwareVersion;
    }

    public LockData(String deviceIdentifier, int firmwareVersion, Profile profile) {
        this.deviceIdentifier = deviceIdentifier;
        this.firmwareVersion = firmwareVersion;
        this.profile = profile;
    }

    public Boolean hasValidProfile() {
        return profile != null
                && profile.isValid();
    }

    public boolean hasSecondaryLock() {
        return modelNumber.startsWith("5440");
    }

    public boolean isDeadbolt() {
        return modelNumber.startsWith("D1000");
    }



}
