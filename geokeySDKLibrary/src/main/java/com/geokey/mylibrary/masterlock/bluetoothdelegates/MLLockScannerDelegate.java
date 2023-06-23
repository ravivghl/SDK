package com.geokey.mylibrary.masterlock.bluetoothdelegates;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.Interfaces.IMLLockScannerDelegate;
import com.masterlock.mlbluetoothsdk.MLProduct;


public class MLLockScannerDelegate implements IMLLockScannerDelegate {

    private final static String TAG = MLLockScannerDelegate.class.getSimpleName();

    public MasterLockPresenter presenter;

    private static MLLockScannerDelegate instance;



    public static MLLockScannerDelegate getInstance() {
        if (instance == null) {
            instance = new MLLockScannerDelegate();
        }

        return instance;

    }


    /**
     * Called each time the IMLockScannerDelegate has been set and the Scanner is ready to scan,
     * or if the bluetooth adapter changes from off to on
     */
    @Override
    public void bluetoothReady() {
        presenter.onNext(new MasterLockEvents.SDKReadyToScan(), null);
    }

    /**
     * Called when a bluetooth adapter is not available at IMLockScannerDelegate initialization, or
     * if bluetooth adapter changes from on to off
     */
    @Override
    public void bluetoothDown() {
        Log.d("LockScanner", "bluetoothdown");
        presenter.onNext(new MasterLockEvents.BluetoothDown(), null);
    }

    /**
     * Called when the Lock Scanner detects a broadcast signal from a Master Lock product
     *
     * @param deviceId kms ID of the device detected
     */
    @Override
    public void didDiscoverDevice(String deviceId) {
        Log.d(TAG, "Did Discover " + deviceId);
     //   Log.d("ProductType", "Did Discover ProductType" + ProductType.valueOf(deviceId));

    }

    /**
     * The value returned here determines if the SDK should try to connect to this device
     *
     * @param deviceId     kms ID of the device in question
     * @param rssiStrength Received Signal strength of the device in question
     * @return true if the SDK should try to connect
     */


    @Override
    public boolean shouldConnect(String deviceId, int rssiStrength) {
        return presenter.state.lockDataHashMap.containsKey(deviceId) && presenter.state.lockDataHashMap.get(deviceId).hasValidProfile();
    }

    /**
     * Return an MLProduct for the SDK to report LockState or issue commands on a lock with
     *
     * @param deviceId kms ID of the device ID that the SDK is requesting
     * @return an MLProduct, or null if you do not yet wish to observe/interact with this lock
     * <p>
     * The SDK will call this method several times-- It is important that if you do return an MLProduct
     * that you return the same MLProduct for each deviceID every time.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public MLProduct productForDevice(String deviceId) {
        Log.d(TAG, "Product For " + deviceId);
        MLProduct product;

        product = getMlProduct(deviceId);

        LockData lockData = presenter.state.lockDataHashMap.get(deviceId);

        if (lockData != null && lockData.hasValidProfile()) {
            product.setAccessProfile(lockData.profile.accessProfile, lockData.firmwareVersion);
        }


        // Set options on the product
        product.autoDisconnect = false; // false -> keepAlive is ON, we won't automatically disconnect when command queue is empty
        product.setKeepAliveIntervalSeconds(10); // Connections will be kept alive for at least 10 seconds
        product.keepAliveCallback = (r, e) -> {
        }; // Default keepAlive has a callback that logs each KeepAlive occurrence; We'll override to keep logging quiet

        return product;
    }

    /**
     * In this demo, this method retrieves a cached MLProduct or creates and caches a new one
     * Note that a constructor that takes a IMLProductDelegate as a param is used.
     * An MLProduct must have a delegate in order to receive callbacks to IMLProductDelegate.onLockStateChange()
     *
     * @param deviceId kms ID for the lock
     * @return MLProduct for the given deviceID
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private MLProduct getMlProduct(String deviceId) {
        MLProduct product;
        if (presenter.state.productHashMap.containsKey(deviceId)) {
            product = presenter.state.productHashMap.get(deviceId);
        } else {
            product = new MLProduct(deviceId, presenter.productDelegate);
            presenter.state.productHashMap.put(deviceId, product);
            presenter.onNext(new MasterLockEvents.NewProductState(), null);

        }
        return product;
    }

    /**
     * Indicates that the Android bluetooth stack reported failure for the ble connection
     *
     * @param deviceId the id of the Lock
     * @param code     The BluetoothGatt code returned by the Android bluetooth stack as the cause of failure
     */
    @Override
    public void bluetoothFailedWithDisconnectCode(String deviceId, int code) {
        Log.d("Demo", "Failure code: " + code);
    }

    @Override
    public void onScanFailed(int errorCode) {
        String reason;
        switch (errorCode) {
            case -1:
                reason = "SCANNER IS NULL";
                break;
            case 1:
                reason = "SCAN_FAILED_ALREADY_STARTED";
                break;
            case 2:
                reason = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                break;
            case 3:
                reason = "SCAN_FAILED_INTERNAL_ERROR";
                break;
            case 4:
                reason = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                break;
            default:
                reason = "UNKNOWN REASON";
        }
        Log.d("Demo", "OnScanFailed reason: " + reason);
    }


}
