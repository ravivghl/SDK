package com.geokey.mylibrary.masterlock.presenter;

import com.geokey.mylibrary.masterlock.online.models.ApiErrorDetails;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismOptions;
import com.masterlock.mlbluetoothsdk.models.MLFirmwareUpdateStatus;

public abstract class MasterLockEvents {
    EventType eventType;

    enum EventType {
        ClickGetProfile,
        ClickUnlock,
        SDKLicenseError,
        SDKReadyToScan,
        ActivityResumed,
        ActivityPaused,
        UserGrantedPermission,
        LocationPermissionDenied,
        NewProductState,
        ApiError,
        BluetoothDown,
        ClickRelock,
        ClickBattery,
        ClickFirmwareUpdate,
        DoFirmwareUpdate,
        FirmwareUpdateStatusChange,
        ClickSetLeftHandedTrue,
        ClickSetLeftHandedFalse
    }

    public static class GetDeviceAccessProfile extends MasterLockEvents {

        String deviceId;

        public GetDeviceAccessProfile(String deviceId) {
            this.eventType = EventType.ClickGetProfile;
            this.deviceId = deviceId;
        }

    }

    public static class ClickUnlock extends MasterLockEvents {

        String deviceId;
        MechanismOptions mechanismOption;

        public ClickUnlock(String deviceId, MechanismOptions mechanismOption) {
            this.eventType = EventType.ClickUnlock;
            this.deviceId = deviceId;
            this.mechanismOption = mechanismOption;
        }

    }
    public static class SDKLicenseError extends MasterLockEvents {
        public SDKLicenseError() {
            this.eventType = EventType.SDKLicenseError;
        }
    }


    public static class SDKReadyToScan extends MasterLockEvents {
        public SDKReadyToScan() {
            this.eventType = EventType.SDKReadyToScan;
        }
    }

    public static class ActivityResumed extends MasterLockEvents {
        public ActivityResumed() { this.eventType = EventType.ActivityResumed; }
    }

    public static class UserGrantedPermission extends MasterLockEvents {
        public UserGrantedPermission() { this.eventType = EventType.UserGrantedPermission; }
    }

    public static class LocationPermissionDenied extends MasterLockEvents {
        public LocationPermissionDenied() { this.eventType = EventType.LocationPermissionDenied; }
    }

    public static class ActivityPaused extends MasterLockEvents {
        public ActivityPaused() { this.eventType = EventType.ActivityPaused; }
    }

    public static class NewProductState extends MasterLockEvents {
        public NewProductState() { this.eventType = EventType.NewProductState; }
    }

    public static class ApiError extends MasterLockEvents {
        ApiErrorDetails errorDetails;
        String message;
        public ApiError(ApiErrorDetails errorDetails) {
            this.eventType = EventType.ApiError;
            this.message = errorDetails.message();
            this.errorDetails = errorDetails;
        }
    }

    public static class BluetoothDown extends MasterLockEvents {
        public BluetoothDown() { this.eventType = EventType.BluetoothDown; }
    }

    public static class ClickRelock extends MasterLockEvents {
        String deviceId;
        MechanismOptions mechanismOption;

        public ClickRelock(String deviceId) {
            this.eventType = EventType.ClickRelock;
            this.deviceId = deviceId;
            this.mechanismOption = MechanismOptions.Primary;
        }
    }

    public static class ClickBattery extends MasterLockEvents {
        String deviceId;
        MechanismOptions mechanismOption;

        public ClickBattery(String deviceId) {
            this.eventType = EventType.ClickBattery;
            this.deviceId = deviceId;
            this.mechanismOption = MechanismOptions.Primary;
        }
    }
    public static class ClickFirmwareUpdate extends MasterLockEvents {
        String deviceId;
        public ClickFirmwareUpdate(String deviceId) {
            this.eventType = EventType.ClickFirmwareUpdate;
            this.deviceId = deviceId;
        }
    }

    public static class DoFirmwareUpdate extends MasterLockEvents {
        String deviceId;
        int version;

        FirmwareUpdateStatusListener listener;
        public DoFirmwareUpdate(String deviceId, int version, FirmwareUpdateStatusListener listener) {
            this.eventType = EventType.DoFirmwareUpdate;
            this.deviceId = deviceId;
            this.version = version;
            this.listener = listener;
        }

        public interface FirmwareUpdateStatusListener {
            public void onNext(MLFirmwareUpdateStatus status);
        }
    }

    public static class FirmwareUpdateStatusChange extends MasterLockEvents {
        String deviceId;
        MLFirmwareUpdateStatus status;

        public FirmwareUpdateStatusChange(String deviceId, MLFirmwareUpdateStatus status) {
            this.deviceId = deviceId;
            this.eventType = EventType.FirmwareUpdateStatusChange;
            this.status = status;
        }
    }
    public static class ClickSetleftHandedTrue extends MasterLockEvents {
        String deviceId;
        public ClickSetleftHandedTrue(String deviceId) {
            this.deviceId = deviceId;
            this.eventType = EventType.ClickSetLeftHandedTrue;
        }
    }
    public static class ClickSetleftHandedFalse extends MasterLockEvents {
        String deviceId;
        public ClickSetleftHandedFalse(String deviceId) {
            this.deviceId = deviceId;
            this.eventType = EventType.ClickSetLeftHandedFalse;
        }
    }
}
