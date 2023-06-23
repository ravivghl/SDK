package com.geokey.mylibrary.masterlock.online.models;

import com.google.gson.annotations.SerializedName;

public class ApiErrorDetails {

    @SerializedName("Result")
    private String result;
    @SerializedName("Messages")
    private String[] messages;


    public String message;
    public int statusCode;
    public RequestType requestType;

    public ApiErrorDetails() {
    }

    public enum RequestType {
        Token,
        Profile,
    }



    public ApiErrorDetails(RequestType requestType, String message) {
        this.requestType = requestType;
        this.message = message;
    }

    public int status() {
        return statusCode;
    }

    public String result() {
        return result;
    }

    public String message() {
        return message;
    }

    public String[] messages() {
        return messages;
    }
}