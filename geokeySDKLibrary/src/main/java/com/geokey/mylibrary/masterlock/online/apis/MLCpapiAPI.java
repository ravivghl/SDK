package com.geokey.mylibrary.masterlock.online.apis;



import com.geokey.mylibrary.masterlock.online.models.AccessProfileRequest;
import com.geokey.mylibrary.masterlock.online.models.AccessProfileResponse;
import com.geokey.mylibrary.masterlock.online.models.CpapiResponse;
import com.geokey.mylibrary.masterlock.online.models.DeviceLookupRequest;
import com.geokey.mylibrary.masterlock.online.models.DeviceLookupResponse;
import com.geokey.mylibrary.masterlock.online.models.FirmwareVersionsResponse;
import com.geokey.mylibrary.masterlock.online.models.IpapiAuthRequest;
import com.geokey.mylibrary.masterlock.online.models.IpapiAuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MLCpapiAPI {

    @POST("authenticate")
    Call<IpapiAuthResponse> authenticate(@Body IpapiAuthRequest authRequest);

    @POST("/v2/device/accessprofile/create")
    Call<AccessProfileResponse> getAccessProfile(@Header("X-APIToken") String token,
                                                 @Body AccessProfileRequest request);

    @POST("/v2/device")
    Call<CpapiResponse<DeviceLookupResponse>> lookupDevice(@Header("X-APIToken") String token,
                                                           @Body DeviceLookupRequest request);

    @POST("/v2/firmware/device/listavailable")
    Call<FirmwareVersionsResponse> getAvailableFirmwareVersions(@Header("X-APIToken") String token,
                                                                @Body DeviceLookupRequest request);


}
