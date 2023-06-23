package com.geokey.mylibrary.masterlock.online;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.geokey.mylibrary.masterlock.MockLockDataProvider;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.Profile;
import com.geokey.mylibrary.masterlock.online.apis.MLCpapiAPI;
import com.geokey.mylibrary.masterlock.online.models.AccessProfileRequest;
import com.geokey.mylibrary.masterlock.online.models.AccessProfileResponse;
import com.geokey.mylibrary.masterlock.online.models.ApiErrorDetails;
import com.geokey.mylibrary.masterlock.online.models.CpapiResponse;
import com.geokey.mylibrary.masterlock.online.models.DeviceLookupRequest;
import com.geokey.mylibrary.masterlock.online.models.DeviceLookupResponse;
import com.geokey.mylibrary.masterlock.online.models.FirmwareVersionsResponse;
import com.geokey.mylibrary.masterlock.online.models.IpapiAuthRequest;
import com.geokey.mylibrary.masterlock.online.models.IpapiAuthResponse;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.MLCommandCallback;
import com.masterlock.mlbluetoothsdk.utility.AsyncJob;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MLConnectedProductApiClient {
    public static MLConnectedProductApiClient instance;
    private MasterLockPresenter presenter;
    private final MLCpapiAPI cpapiAPI;
    private String token;
    private ZonedDateTime expiration = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private HandlerThread thread = new HandlerThread("AppTokenWatchDog");

    private static Retrofit retrofit;

    public static Retrofit getRetrofit() {
        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build();

            String url = MockLockDataProvider.MLBaseURL;

            retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private MLConnectedProductApiClient() {

        cpapiAPI = getRetrofit().create(MLCpapiAPI.class);

        getToken();
        startTokenWatchDog();
    }

    public void stopClient() {
        instance = null;
    }

    private void startTokenWatchDog() {
        thread.start();
        new Handler(thread.getLooper()).post(() -> {
            boolean interrupted = false;
            do {
                try {
                    Thread.sleep(30000L);
                    if (expiration.isBefore(ZonedDateTime.now().plusMinutes(2))) {
                        getToken();
                    }
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            } while (instance != null && !interrupted);
        });
    }

    public static MLConnectedProductApiClient getClient(MasterLockPresenter presenter) {
        if (instance == null) {
            instance = new MLConnectedProductApiClient();
            instance.presenter = presenter;
        }
        return instance;
    }

    public void lookupDeviceAndFetchProfile(String deviceId, ZonedDateTime starts, ZonedDateTime ends) {
        presenter.state.lockDataHashMap.remove(deviceId); // remove old data if present
        DeviceLookupRequest req = new DeviceLookupRequest(deviceId);
        AsyncJob.doInBackground(() -> {
            Call<CpapiResponse<DeviceLookupResponse>> call = cpapiAPI.lookupDevice(token, req);
            try {
                Response<CpapiResponse<DeviceLookupResponse>> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    LockData lockData = new LockData();
                    lockData.deviceIdentifier = response.body().device.deviceIdentifier;
                    lockData.firmwareVersion = response.body().device.firmwareVersion;
                    Optional<DeviceLookupResponse.DeviceTrait> model =
                            response.body().device.deviceTraitList
                                    .stream()
                                    .filter((deviceTrait -> deviceTrait.name.equals("ModelNumber")))
                                    .findAny();
                    lockData.modelNumber = (model.isPresent()) ? model.get().value : "";
                    getAvailableFirmwareVersions(lockData, (updatedLockData, error) -> {
                        AsyncJob.doInBackground(() -> fetchProfile(updatedLockData, starts, ends));
                    });

                } else {
                    presenter.state.lockDataHashMap.remove(deviceId);
                    handleError(response, ApiErrorDetails.RequestType.Profile);
                }
            } catch (IOException e) {
                presenter.state.lockDataHashMap.remove(deviceId);
                handleIOException(e, ApiErrorDetails.RequestType.Profile);
            }
        });

    }

    private void handleIOException(IOException e, ApiErrorDetails.RequestType requestType) {
        AsyncJob.doOnMainThread(() -> {
            presenter.onNext(new MasterLockEvents.ApiError(new ApiErrorDetails(requestType, e.getLocalizedMessage())), null);
        });

    }

    private void handleError(Response<?> response, ApiErrorDetails.RequestType requestType) {
        ApiErrorDetails error = ErrorUtil.parseError(response);
        error.requestType = requestType;

        AsyncJob.doOnMainThread(() -> {
            presenter.onNext(new MasterLockEvents.ApiError(error), null);
        });
    }

    public void fetchProfile(LockData lockData, ZonedDateTime starts, ZonedDateTime ends) {
        AccessProfileRequest request = getAccessProfileRequest(lockData.deviceIdentifier, starts, ends);

        AsyncJob.doInBackground(() -> {
            Call<AccessProfileResponse> call = cpapiAPI.getAccessProfile(token, request);

            try {
                Response<AccessProfileResponse> profileResponse = call.execute();

                if (profileResponse.isSuccessful() && profileResponse.body() != null) {
                    lockData.profile = Profile.of(profileResponse.body(), starts, ends);
                    presenter.state.lockDataHashMap.put(lockData.deviceIdentifier, lockData);
                    AsyncJob.doOnMainThread(() -> {
                        presenter.onNext(new MasterLockEvents.NewProductState(), null);
                    });

                }
            } catch (IOException e) {
                AsyncJob.doOnMainThread(() -> {
                    handleIOException(e, ApiErrorDetails.RequestType.Profile);
                });
            }
        });
    }

    private void getAvailableFirmwareVersions(LockData lockData, MLCommandCallback<LockData> callback) {
        AsyncJob.doInBackground(() -> {
            Call<FirmwareVersionsResponse> call = cpapiAPI.getAvailableFirmwareVersions(token, new DeviceLookupRequest(lockData.deviceIdentifier));
            try {
                FirmwareVersionsResponse resp = call.execute().body();
                if (resp == null) {
                    AsyncJob.doOnMainThread(() -> {
                        callback.result(lockData, null);
                    });
                    return;
                }
                Collections.addAll(lockData.firmwareVersions, resp.firmwareVersions);
                AsyncJob.doOnMainThread(() -> {
                    callback.result(lockData, null);
                });

            } catch (Exception e) {
                AsyncJob.doOnMainThread(() -> {
                    callback.result(null, e);
                });

            }

        });
    }


    public AccessProfileRequest getAccessProfileRequest(String deviceId, ZonedDateTime starts, ZonedDateTime ends) {
        AccessProfileRequest req = new AccessProfileRequest();

        req.DeviceIdentifier = deviceId;

        req.UserId = 0; // need a valid id here

        req.AccessScheduleDays = "Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday";
        req.ProfileActivation = starts.toString();
        req.ProfileExpiration = ends.toString();
        req.AccessStartTime = starts.toString() + ":" + starts.getMinute();  // TODO don't use hard coded start
        req.AccessEndTime = ends.getHour() + ":" + ends.getMinute();
        // TODO set end time based on business rules / use case
        // TODO adjust permissions based on your org's business rules / use case
        req.Permissions = getPermissions();
        return req;
    }

    private String getLicense() {
        return MockLockDataProvider.MLLicense;
    }

    private String getPassword() {
        return MockLockDataProvider.MLLicensePassword;
    }

    public void getToken() {
        IpapiAuthRequest req = new IpapiAuthRequest();
        req.license = getLicense();
        req.password = getPassword();

        Call<IpapiAuthResponse> call = cpapiAPI.authenticate(req);


        AsyncJob.doInBackground(() -> {
            try {
                Response<IpapiAuthResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    IpapiAuthResponse result = response.body();
                    token = result.ApiToken;
                    expiration = ZonedDateTime.parse(result.TokenExpires);
                } else {
                    handleError(response, ApiErrorDetails.RequestType.Token);
                }

            } catch (IOException e) {
                handleIOException(e, ApiErrorDetails.RequestType.Token);
            }
        });
    }

    private String[] getPermissions() {
        return new String[]{
                "Unlock",
                "UnlockDoor",
                "UnlockShackle",
                "WriteTime",
                "NudgeTime",
                "AuditTrail",
                "WriteConfiguration",
                "ReadPrimaryPasscode",
                "WritePrimaryPasscode",
                "ReadSecondaryPasscodes",
                "WriteSecondaryPasscodes"};
    }
}
