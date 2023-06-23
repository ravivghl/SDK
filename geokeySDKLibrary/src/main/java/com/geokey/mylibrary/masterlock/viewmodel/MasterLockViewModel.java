package com.geokey.mylibrary.masterlock.viewmodel;

import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;

import com.masterlock.mlbluetoothsdk.MLProduct;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class MasterLockViewModel {
    public boolean isSdkScanning = false;
    public boolean isPermissionDenied = false;
    public boolean isBluetoothDown = false;
    public boolean isSDKInitialized = false;

    public HashMap<String, MLProduct> productHashMap = new HashMap<>();
    public HashMap<String, LockData> lockDataHashMap = new HashMap<>();

    public HashMap<String, MasterLockEvents.DoFirmwareUpdate.FirmwareUpdateStatusListener> firmwareUpdateStatusListenerHashMap = new HashMap<>();
    public MasterLockViewModel() {

    }
    public MasterLockViewModel(HashMap<String, LockData> initialLockData) {
        lockDataHashMap.putAll(initialLockData);
    }

    @NotNull
    @Override
    public String toString() {
        return "isSDKScanning = " + isSdkScanning + "\n" + "isPermissionDenied = " + isPermissionDenied + "\n";
    }
    

}
