package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceLookupResponse {
    @SerializedName(value = "DeviceIdentifier")
    public String deviceIdentifier;

    @SerializedName(value = "ActivationState")
    public String activationState;

    @SerializedName(value = "Traits")
    public List<DeviceTrait> deviceTraitList;

    @SerializedName(value = "FirmwareVersion")
    public int firmwareVersion;

    public static class DeviceTrait {
        @SerializedName("Name")
        public String name;
        @SerializedName("Value")
        public String value;
    }

}
