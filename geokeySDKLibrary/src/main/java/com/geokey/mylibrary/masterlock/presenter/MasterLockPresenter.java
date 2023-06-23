package com.geokey.mylibrary.masterlock.presenter;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.geokey.mylibrary.masterlock.MockLockDataProvider;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLLockScannerDelegate;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLProductDelegate;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.online.MLConnectedProductApiClient;
import com.geokey.mylibrary.masterlock.utility.MasterLockCommandCallback;
import com.geokey.mylibrary.masterlock.viewmodel.MasterLockViewModel;
import com.masterlock.mlbluetoothsdk.MLBluetoothSDK;
import com.masterlock.mlbluetoothsdk.MLCommandCallback;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.errors.MLCommandNotAllowedError;
import com.masterlock.mlbluetoothsdk.errors.MLSDKNotInitializedException;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismOptions;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * @author Aaron Hartman
 * Handles domain logic of the Demo App
 * <- Takes in events from UI, MLBluetoothSDK, IMLockScannerDelgate, IMLProductDelegate, MLConnectedProductsAPI
 * -> emits new state to Activity in the form of MasterLockViewModel
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class MasterLockPresenter {

    public final MasterLockViewModel state;
    final MLLockScannerDelegate lockScannerDelegate;
    private final MLConnectedProductApiClient apiClient;
    public MLProductDelegate productDelegate;
    LockActivityLauncher lockActivity;
    public MLBluetoothSDK masterLockSDK;


    @RequiresApi(api = Build.VERSION_CODES.O)
    public MasterLockPresenter(LockActivityLauncher lockActivity,
                               MLLockScannerDelegate lockScannerDelegate,
                               MLProductDelegate productDelegate) {
        this.state = new MasterLockViewModel(MockLockDataProvider.getMockData());
        this.lockActivity = lockActivity;
        this.lockScannerDelegate = lockScannerDelegate;
        this.lockScannerDelegate.presenter = this;
        this.productDelegate = productDelegate;
        this.productDelegate.presenter = this;
        apiClient = MLConnectedProductApiClient.getClient(this);

    }

    public void setMasterLockSDK(MLBluetoothSDK masterLockSDK) {
        this.masterLockSDK = masterLockSDK;
        state.isSDKInitialized = true;

        lockActivity.initializeLockScanner(lockScannerDelegate);
        Log.d("MasterLockDemo", "Master Lock SDK Version " + masterLockSDK.version());
    }


    public void onNext(MasterLockEvents event, View v) {
        Log.d("onNext", "Event = " + event.eventType);
        switch (event.eventType) {
            case ActivityResumed:
                lockActivity.updateUi(state);
                if (state.isSDKInitialized) {
                    lockActivity.initializeLockScanner(lockScannerDelegate);
                }
                break;
            case ActivityPaused:
                stopScanning();
                apiClient.stopClient();
                break;
            case ClickGetProfile:
                handleProfileRequest((MasterLockEvents.GetDeviceAccessProfile) event);
                break;
            case ClickUnlock:
                unlock((MasterLockEvents.ClickUnlock) event);
                break;
            case SDKLicenseError:
                lockActivity.showSDKLicenseError();
                break;
            case SDKReadyToScan:
                if (lockActivity.hasBluetoothScanningPermissions()) {
                    state.isPermissionDenied = false;
                    state.isBluetoothDown = false;
                    startScanning();
                } else {
                    if (!state.isPermissionDenied) {
                        lockActivity.getLocationPermission();
                    }
                }
                break;

            case UserGrantedPermission:
                state.isPermissionDenied = false;
                startScanning();
                break;
            case LocationPermissionDenied:
                if (!state.isPermissionDenied) {
                    state.isPermissionDenied = true;
                    lockActivity.updateUi(state);
                }
                break;
            case NewProductState:
                lockActivity.updateUi(state);
                break;
            case ApiError:
                handleApiError((MasterLockEvents.ApiError) event);
                break;
            case BluetoothDown:
                state.isSdkScanning = false;
                state.isBluetoothDown = true;
                lockActivity.updateUi(state);
                break;
            case ClickRelock:
                relock((MasterLockEvents.ClickRelock) event);
                break;
            case ClickBattery:
                battery((MasterLockEvents.ClickBattery) event, v);
                break;
            case ClickFirmwareUpdate:
                MasterLockEvents.ClickFirmwareUpdate clickFirmwareUpdate = (MasterLockEvents.ClickFirmwareUpdate) event;
                lockActivity.launchFirmwareUpdateDialog(clickFirmwareUpdate.deviceId);
                break;
            case DoFirmwareUpdate:
                handleFirmwareUpdate((MasterLockEvents.DoFirmwareUpdate) event);
                break;
            case FirmwareUpdateStatusChange:
                MasterLockEvents.FirmwareUpdateStatusChange firmwareUpdateStatusChange = (MasterLockEvents.FirmwareUpdateStatusChange) event;
                try {
                    state.firmwareUpdateStatusListenerHashMap.get(firmwareUpdateStatusChange.deviceId).onNext(firmwareUpdateStatusChange.status);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                break;
            case ClickSetLeftHandedTrue:
                handleClickSetLeftHanded(((MasterLockEvents.ClickSetleftHandedTrue) event).deviceId, true);
                break;
            case ClickSetLeftHandedFalse:
                handleClickSetLeftHanded(((MasterLockEvents.ClickSetleftHandedTrue) event).deviceId, false);
                break;
        }
    }

    private void handleClickSetLeftHanded(String deviceId, Boolean isLeftHanded) {
        MLProduct product;
        product = state.productHashMap.get(deviceId);
        assert product != null;


        if (product == null) return;
        product.setDeadboltLeftHanded(isLeftHanded, (r, e) -> {
            if (e != null) {
                lockActivity.showToast("Set " + deviceId + " Bolt Configuration to Left Handed Error: " + e.getMessage());
            } else {
                lockActivity.showToast("Set " + deviceId + " Bolt Configuration to Left Handed Success");
            }
        });
    }

    private void handleFirmwareUpdate(MasterLockEvents.DoFirmwareUpdate event) {
        MasterLockEvents.DoFirmwareUpdate doFirmwareUpdate = event;
        String deviceId = event.deviceId;
        MLProduct product;
        product = state.productHashMap.get(deviceId);
        if (product == null) return;
        state.firmwareUpdateStatusListenerHashMap.put(doFirmwareUpdate.deviceId, doFirmwareUpdate.listener);
        try {
            product.updateFirmware(doFirmwareUpdate.version);
        } catch (MLSDKNotInitializedException e) {
            e.printStackTrace();
        } catch (MLCommandNotAllowedError e) {
            e.printStackTrace();
        }
    }

    private void handleApiError(MasterLockEvents.ApiError event) {
        switch (event.errorDetails.requestType) {

            case Token:
                lockActivity.showCPAPIError(event.message);
                break;
            case Profile:
                lockActivity.updateUi(state);
                lockActivity.showCPAPIError(event.message);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleProfileRequest(MasterLockEvents.GetDeviceAccessProfile event) {
        state.lockDataHashMap.put(event.deviceId, new LockData(event.deviceId, true));
        lockActivity.updateUi(state);

        ZonedDateTime starts = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        ZonedDateTime ends = ZonedDateTime.now(ZoneOffset.UTC).plusDays(120);
        apiClient.lookupDeviceAndFetchProfile(event.deviceId, starts, ends);
    }

    private void showFailedProfileRequest(MasterLockEvents.GetDeviceAccessProfile event, Exception profileEx) {
        state.lockDataHashMap.remove(event.deviceId);
        lockActivity.updateUi(state);
        lockActivity.showCPAPIError(profileEx.getMessage());
    }

    private void unlock(MasterLockEvents.ClickUnlock event) {

        MLProduct mlProduct = state.productHashMap.get(event.deviceId);
        assert mlProduct != null;
        // mlProduct.unlock();


        mlProduct.unlock(MechanismOptions.Primary, 60, (result, error) -> {
            Log.e("Unlock",result);
            Log.e("Unlock", error+"");
        });

     /*   mlProduct.writePrimaryPasscode(new String[]{"U","U","U","D","L","R","R"}, (result, error) -> {
            Log.e("Passcode", Arrays.toString(result));
         //   Log.e("Passcode", error.getMessage());
        });*/

        /*mlProduct.readPrimaryPasscode(new MLCommandCallback<String[]>() {
            @Override
            public void result(String[] result, Exception error) {
                Log.e("Passcode", Arrays.toString(result));
                Log.e("Passcode", error.getMessage());
            }
        });
*/

     /*   mlProduct.unlock(10, (result, error) -> {

        });
        mlProduct.unlock(MechanismOptions.Primary, 60, (result, error) -> {

        });*/


     /*   new Handler().postDelayed(() -> {

            mlProduct.set(state.productHashMap.get(event.deviceId));

            MLProduct finalMlProduct = mlProduct.get();

            finalMlProduct.relock((r, e) -> {
            });

        }, 8000);*/

        /*mlProduct.unlock(6, (result, error) -> {
             LockStateManager lockState;

             mlProduct.relock(10,);

             mlProduct.lockState.transitionUnlocked(MechanismOptions.Secondary,6 );
        });
*/
       /* mlProduct.setAutoDisconnect(true);
        // MLProduct builder=new MLProduct.MLProductBuilder(event.deviceId, MLProductDelegate.getInstance()).autoDisconnect(true).build();

        if (mlProduct != null) {
            mlProduct.unlock(event.mechanismOption, 6, (r, e) -> {

            });
        }*//*if (builder != null) {
            builder.unlock(event.mechanismOption, 5, (r, e) -> {
            });
        }*/
    }

    private void unlock(MechanismOptions secondary, int i, MLCommandCallback mlCommandCallback) {
    }

    private void relock(MasterLockEvents.ClickRelock event) {
        MLProduct mlProduct = state.productHashMap.get(event.deviceId);

        if (mlProduct != null) {
            mlProduct.relock((r, e) -> {
            });
        }
    }

    private void battery(MasterLockEvents.ClickBattery event, View v) {
        MLProduct mlProduct = state.productHashMap.get(event.deviceId);

        if (mlProduct != null) {
            mlProduct.readBatteryLevel((r, e) -> {

                ((TextView) v).setText(r.batteryLevel + "%");
                Log.d("BatteryLevel", r.batteryLevel + "");
            });
        }
    }

    private void stopScanning() {
        masterLockSDK.stopScanning();
        state.isSdkScanning = false;
    }

    private void startScanning() {
        masterLockSDK.startScanning();
        state.isSdkScanning = true;
        lockActivity.updateUi(state);
    }

    public void setUpKeys(String mlBaseURL, String mlLicense, String mlPassword) {
        MockLockDataProvider.MLBaseURL = mlBaseURL;
        MockLockDataProvider.MLLicense = mlLicense;
        MockLockDataProvider.MLLicensePassword = mlPassword;
    }

    public void initMasterLock(String mlLicenseFile, Context context) {
        try {
            masterLockSDK = MLBluetoothSDK.getInstance(mlLicenseFile, context);
            Log.e("Version", masterLockSDK.version());
            this.setMasterLockSDK(masterLockSDK);
        } catch (Exception e) {
            this.onNext(new MasterLockEvents.SDKLicenseError(), null);
        }

        setMasterLockSDK(masterLockSDK);
    }

    public void initializeLockScanner(@Nullable MLLockScannerDelegate delegate) {

        try {
            masterLockSDK.setLockScannerDelegate(delegate);
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d("MasterLock", "Did you declare a service element " +
                    " for android:name=\"com.masterlock.mlbluetoothsdk.bluetoothscanner.Scanner\"" +
                    " in your AndroidManifest.xml?");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.d("MasterLock", "Are you running in the foreground?");
        }
    }

    public interface LockActivityLauncher {
        void getLocationPermission();

        boolean hasBluetoothScanningPermissions();

        void showPermissionRationale();

        void showCPAPIError(String message);

        void initializeLockScanner(MLLockScannerDelegate delegate);

        void showSDKLicenseError();

        void updateUi(MasterLockViewModel viewModel);

        void launchFirmwareUpdateDialog(String deviceId);

        void showToast(String text);
    }

    public void unlockMasterLock(String deviceId, MasterLockCommandCallback<String> masterLockCommandCallback) {

        MechanismOptions unlockType =MechanismOptions.Primary ;
        MasterLockEvents clickUnlock = new MasterLockEvents.ClickUnlock(deviceId, unlockType);

        onNext(clickUnlock, null);

      /*  MLProduct mlProduct = state.productHashMap.get(deviceId);
        assert mlProduct != null;
        //  mlProduct.unlock(10, (result, error) -> masterLockCommandCallback.result(result, error));

        mlProduct.unlock(MechanismOptions.Primary, 60, (result, error) -> {

            Log.e("Unlock", result);
            Log.e("Unlock", error + "");

        });*/
    }
}
