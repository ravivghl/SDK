package com.geokey.mylibrary.masterlock.online.models;

public class DeviceLookupRequest {
    public String deviceIdentifier;

    public DeviceLookupRequest() { }

    public DeviceLookupRequest(String id) {
        this.deviceIdentifier = id;
    }
}
