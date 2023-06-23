package com.geokey.mylibrary.masterlock.viewholders;

import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismOptions;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;

import org.jetbrains.annotations.NotNull;

public class LockedLockItemViewHolder extends LockItemViewHolder {
    ImageButton buttonSettings;
    Button buttonBattery;

    public LockedLockItemViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void bind(MLProduct product, MechanismState mechanismState, @Nullable @org.jetbrains.annotations.Nullable LockData lockData, boolean isPrimary, MasterLockPresenter presenter) {
        super.bind(product, mechanismState, lockData, isPrimary, presenter);

        initializeUnlockButton(product, isPrimary, lockData, presenter);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initializeUnlockButton(MLProduct product, boolean isPrimary, LockData lockData, MasterLockPresenter presenter) {
        MechanismOptions unlockType = isPrimary ? MechanismOptions.Primary : MechanismOptions.Secondary;
        MasterLockEvents clickUnlock = new MasterLockEvents.ClickUnlock(product.deviceId, unlockType);
        MasterLockEvents clickBattery = new MasterLockEvents.ClickBattery(product.deviceId);

        buttonAction.setEnabled(true);
        buttonAction.setOnClickListener((v) -> {
            presenter.onNext(clickUnlock, v);
        });

        buttonBattery.setOnClickListener((v) -> {
            presenter.onNext(clickBattery,v);
        });


        if (isPrimary) {
            buttonSettings.setEnabled(true);
            buttonSettings.setOnClickListener((v) -> showSettingsPopUp(v, lockData, presenter));
        } else {
            buttonSettings.setVisibility(View.GONE);
            buttonAction.setText(R.string.unlock_shackle);
        }
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        buttonSettings = itemView.findViewById(R.id.buttonSettings);
        buttonBattery = itemView.findViewById(R.id.buttonActionBattery);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showSettingsPopUp(View v, LockData lockData, MasterLockPresenter presenter) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        int menu;
        if (lockData.isDeadbolt()) {
            menu = R.menu.deadbolt_menu;
        } else {
            menu = R.menu.settings_menu;
        }
        popupMenu.inflate(menu);
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener((item) -> {
            int itemId = item.getItemId();
            if (itemId == R.id.set_left_handed) {
                presenter.onNext(new MasterLockEvents.ClickSetleftHandedTrue(lockData.deviceIdentifier), v);
            } else if (itemId == R.id.set_left_handed_false) {
                presenter.onNext(new MasterLockEvents.ClickSetleftHandedFalse(lockData.deviceIdentifier), v);
            } else if (itemId == R.id.firmware_update) {
                presenter.onNext(new MasterLockEvents.ClickFirmwareUpdate(lockData.deviceIdentifier), v);
            }
            return true;
        });
    }

}
