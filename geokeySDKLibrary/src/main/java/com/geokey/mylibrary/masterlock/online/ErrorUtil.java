package com.geokey.mylibrary.masterlock.online;


import com.geokey.mylibrary.masterlock.online.models.ApiErrorDetails;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;

public class ErrorUtil {

    public static ApiErrorDetails parseError(Response<?> response) {
        Converter<ResponseBody, ApiErrorDetails> converter =
                MLConnectedProductApiClient.getRetrofit().responseBodyConverter(ApiErrorDetails.class, new Annotation[0]);

        int statusCode = response.code();
        if (response.errorBody() != null) {
            return parseErrorBody(response, converter, statusCode);
        }
        return  parseErrorCode(statusCode);
    }

    @NotNull
    private static ApiErrorDetails parseErrorBody(Response<?> response, Converter<ResponseBody, ApiErrorDetails> converter, int statusCode) {
        ApiErrorDetails error;
        try {
            error = converter.convert(response.errorBody());
            error.statusCode = statusCode;
            error.message = (error.messages().length == 0) ? "Result: " + error.result() + ": " + error.status() : error.messages()[0];
            if (error.statusCode == 404) {
                error.message = error.message + "\n\nHave you activated this lock on your platform license?";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return parseErrorCode(statusCode);
        }

        return error;
    }

    @NotNull
    private static ApiErrorDetails parseErrorCode(int statusCode) {
        ApiErrorDetails error;
        error = new ApiErrorDetails();
        error.statusCode = statusCode;

        switch (statusCode) {
            case 400:
                error.message = "Bad Request. Did you enter your API credentials?";
                break;
            case 401:
                error.message = "Bad Request. Invalid API session token. Did you enter your API credentials?";
                break;
            case 500:
                error.message= "The Online Service was unavailable. Try again.";
            default:
                error.message = "Unknown error";
        }

        return error;
    }
}
