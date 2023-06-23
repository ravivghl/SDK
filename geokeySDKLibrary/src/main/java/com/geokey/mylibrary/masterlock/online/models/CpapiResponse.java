package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

public class CpapiResponse<T> {

    @SerializedName(value = "Device")
    public T device;

    public T payload;

    public String result;
    public String[] messages;
}
