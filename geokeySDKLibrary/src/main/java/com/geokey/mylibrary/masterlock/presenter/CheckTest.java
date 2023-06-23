package com.geokey.mylibrary.masterlock.presenter;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.masterlock.mlbluetoothsdk.Interfaces.IMLLockScannerDelegate;
import com.masterlock.mlbluetoothsdk.bluetoothscanner.internal.ClientDevice;
import com.masterlock.mlbluetoothsdk.enums.ProductType;
import com.masterlock.mlbluetoothsdk.producttools.GattCallback;
import com.masterlock.mlbluetoothsdk.producttools.ScanCallback;

public class CheckTest  extends GattCallback {

    public CheckTest(ClientDevice device, com.masterlock.mlbluetoothsdk.Interfaces.IMLLockScannerDelegate delegate, ScanCallback scanCallback) {
        super(device, delegate, scanCallback);
    }
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

      //  ProductType
    }


}
