package com.geokey.mylibrary.masterlock.online.models;

public class AccessProfileRequest {
    public String DeviceIdentifier;
    public int UserId;
    public String AccessScheduleDays;
    public String ProfileActivation;
    public String ProfileExpiration;
    public String[] Permissions;
    public String AccessStartTime;
    public String AccessEndTime;
}
