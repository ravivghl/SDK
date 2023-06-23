package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

public class AccessProfileResponse {
    @SerializedName("AccessProfile")
    public String accessProfile;

    @SerializedName("Result")
    public String result;

    @SerializedName("Messages")
    public String[] messages;
}
