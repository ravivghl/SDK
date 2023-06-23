package com.geokey.mylibrary.masterlock.bluetoothdelegates.models;


import com.geokey.mylibrary.masterlock.online.models.AccessProfileResponse;

import java.time.ZonedDateTime;

public class Profile {
    public String accessProfile;
    public ZonedDateTime accessProfileStartDate;
    public ZonedDateTime accessProfileEndDate;

    public Profile(String accessProfile, ZonedDateTime accessProfileStartDate, ZonedDateTime accessProfileEndDate) {
        this.accessProfile = accessProfile;
        this.accessProfileStartDate = accessProfileStartDate;
        this.accessProfileEndDate = accessProfileEndDate;
    }

    public static Profile of(AccessProfileResponse apiProfile, ZonedDateTime start, ZonedDateTime ends) {
        return new Profile(apiProfile.accessProfile, start, ends);
    }

    public boolean isValid() {
        return this.accessProfileStartDate.isBefore(ZonedDateTime.now()) && this.accessProfileEndDate.isAfter(ZonedDateTime.now());
    }
}

