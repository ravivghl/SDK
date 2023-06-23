package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

public class FirmwareVersionItem {
    @SerializedName("Version")
    public int version;

    @SerializedName("ReleaseDate")
    public String releaseDate;

    @SerializedName("Description")
    public String description;

    @SerializedName("ReleaseNotes")
    public String releaseNotes;

}
