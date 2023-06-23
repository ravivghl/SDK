package com.geokey.mylibrary.masterlock.bluetoothdelegates;

import android.util.Log;

import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.geokey.mylibrary.masterlock.utility.AuditTrailLogger;

import com.masterlock.mlbluetoothsdk.Interfaces.IMLProductDelegate;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.bluetoothscanner.internal.ClientDevice;
import com.masterlock.mlbluetoothsdk.enums.MLBroadcastState;
import com.masterlock.mlbluetoothsdk.lockstate.LockState;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismStateOptions;
import com.masterlock.mlbluetoothsdk.models.MLFirmwareUpdateStatus;
import com.masterlock.mlbluetoothsdk.models.audittrail.MLAuditTrailEntry;
import com.masterlock.mlbluetoothsdk.producttools.ProductHelper;
import com.masterlock.mlbluetoothsdk.utility.AsyncJob;

/**
 * Implements the MLProductDelegate
 * <p>
 * In this example, we only need Master Lock SDK 2.0's onLockStateChange delegate
 * to drive our app-- we also use didReadAuditEntries to print
 * audit trail results to logcat.
 */
public class MLProductDelegate implements IMLProductDelegate {

    private static MLProductDelegate instance;
    public MasterLockPresenter presenter;

    public MLProductDelegate() {
    }

    public static MLProductDelegate getInstance() {
        if (instance == null) {
            instance = new MLProductDelegate();
        }
        return instance;
    }

    /**
     * called by the SDK when LockState has changed
     *
     * @param mlProduct The MLProduct that the change is for
     * @param lockState The value of the new LockState
     */
    @Override
    public void onLockStateChange(MLProduct mlProduct, LockState lockState) {


    //    Log.d("MasterLock", "onLockStateChange CountDown: " + lockState.getPrimary().countdown());

        Log.d("Demo", "***" + "\n" +
                "\t\t" + "New LockState for " + mlProduct.deviceId + ":\n" +
                "\t\t\t" + "Visibility: " + lockState.getVisibility().name() + "\n" +
                "\t\t\t" + "Primary MechanismState: " + lockState.getPrimary().getValue() + getCountDownIfUnlocked(lockState.getPrimary()) + "\n" +
                "\t\t\t" + "Secondary MechanismState: " + lockState.getSecondary().getValue() + getCountDownIfUnlocked(lockState.getSecondary()) + "\n" +
                "\t\t\t" + "isKeyPadActive: " + lockState.getKeypadActive());

        presenter.onNext(new MasterLockEvents.NewProductState(), null);
    }


    private String getCountDownIfUnlocked(MechanismState mechanismState) {
        Log.d("MasterLock", "onLockStateChange CountDown: " + mechanismState.countdown());
        return (mechanismState.getValue() == MechanismStateOptions.Unlocked) ? " countdown: " + mechanismState.countdown() : "";
    }


    /**
     * called by the SDK with MLAuditTrails Entries that have been read from a lock
     * product: The MLProduct for the lock that the AuditEntries are for
     * entries: An Array of MLAuditTrailEntry objects that were successfully uploaded
     * for this MLProduct's lock
     */
    @Override
    public void didReadAuditEntries(MLProduct mlProduct, MLAuditTrailEntry[] mlAuditTrailEntries) {
        // Any processing you do here should happen on a new thread
        AsyncJob.doInBackground(() -> {
            AuditTrailLogger.logEntries(mlAuditTrailEntries, "MasterLock", false);
        });
    }


    /**
     * Tells the delegate that the product is connected.
     *
     * @param mlProduct The MLProduct that connected.
     */
    @Override
    public void didConnect(MLProduct mlProduct) {
        // Before SDK 2.0.0, tracking this was useful for determining the state of Lock, or scheduling commands
        // In SDK 2.0.+ state can more easily be determined through .onLockStateChange
    }

    /**
     * Tells the delegate that the product is disconnected.
     *
     * @param mlProduct The MLProduct that disconnected.
     */
    @Override
    public void didDisconnect(MLProduct mlProduct) {
        // Before SDK 2.0.0, tracking this was useful for determining the state of Lock, or scheduling commands
        // In SDK 2.0.+ state can more easily be determined through .onLockStateChange
    }

    /**
     * Tells the delegate that the product could not connect
     *
     * @param mlProduct The MLProduct that disconnected.
     * @param e         the cause of the failure
     */
    @Override
    public void didFailToConnect(MLProduct mlProduct, Exception e) {
        e.printStackTrace();
        // The exception may provide guidance as to why the failure occurred, e.g. check your access profile
    }

    /**
     * Tells the delegate that the product's state changed
     * SDK will broadcast
     * MLBroadcastState.awake at 1 second intervals while a session with the lock is open
     * MLBroadcastState.asleep when a session has ended
     * MLBroadcastState.firmwareUpdate when a session is in bootloader mode
     *
     * @param mlProduct        The MLProduct that changed
     * @param mlBroadcastState The new state the lock is in
     */
    @Override
    public void didChangeState(MLProduct mlProduct, MLBroadcastState mlBroadcastState) {
        // Before SDK 2.0.0, tracking this was useful for determining the state of Lock, or scheduling commands
        // In SDK 2.0.+ state can more easily be determined through .onLockStateChange
    }

    /**
     * Tells status when a firmwareUpdate is in progress
     *
     * @param mlProduct              The MLProduct that is being updated
     * @param mlFirmwareUpdateStatus State / PercentComplete / Error for firmware update
     */
    @Override
    public void firmwareUpdateStatusUpdate(MLProduct mlProduct, MLFirmwareUpdateStatus mlFirmwareUpdateStatus) {
        presenter.onNext(new MasterLockEvents.FirmwareUpdateStatusChange(mlProduct.deviceId, mlFirmwareUpdateStatus), null);
    }

    /**
     * The firmware version broadcast by the lock does not match the expected firmware version based
     * on the MLProduct that you have created; Sync your data with ML CPAPI and supply a MLProduct
     * with updated data.
     *
     * @param mlProduct The MLProduct that needs to be updated
     */
    @Override
    public void shouldUpdateProductData(MLProduct mlProduct) {
        // Contact CP API to get the updated firmwareVersion, and a new AccessProfile
        presenter.onNext(new MasterLockEvents.GetDeviceAccessProfile(mlProduct.deviceId), null);
    }

    /**
     * called by the SDK with MLAuditTrails Entries that have been successfully uploaded to SDK telemetry API
     * (Will not be called when the device doesn't have an internet connection)
     *
     * @param mlProduct           The MLProduct for the lock that the AuditEntries are for
     * @param mlAuditTrailEntries An Array of MLAuditTrailEntry objects that were successfully uploaded
     *                            for this MLProduct's lock
     *                            use didReadAuditEntries to ensure data is returned at read time
     *                            when the phone is offline
     */
    @Override
    public void didUploadAuditEntries(MLProduct mlProduct, MLAuditTrailEntry[] mlAuditTrailEntries) {

        // Any processing you do here should be on a new thread
        // We won't do anything here, because we are already tracking the entries in didReadAuditEntries
    }



}
