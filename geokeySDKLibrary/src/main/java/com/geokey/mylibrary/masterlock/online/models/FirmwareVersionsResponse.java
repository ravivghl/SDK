package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

public class FirmwareVersionsResponse {
    @SerializedName("FirmwareVersions")
    public FirmwareVersionItem[] firmwareVersions;

    @SerializedName("Result")
    public String result;

    @SerializedName("Messages")
    public String[] messages;
}
