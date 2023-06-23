package com.geokey.mylibrary.masterlock.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;

import org.jetbrains.annotations.NotNull;

public class UnlockedDeadboltItemViewHolder extends LockItemViewHolder{

    public UnlockedDeadboltItemViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(MLProduct product, MechanismState mechanismState, @Nullable @org.jetbrains.annotations.Nullable LockData lockData, boolean isPrimary, MasterLockPresenter presenter) {
        super.bind(product, mechanismState, lockData, isPrimary, presenter);

        initializeRelockButton(product, isPrimary, presenter);
    }

    private void initializeRelockButton(MLProduct product, boolean isPrimary, MasterLockPresenter presenter) {
        MasterLockEvents clickEvent =  new MasterLockEvents.ClickRelock(product.deviceId);

        buttonAction.setEnabled(true);
        buttonAction.setOnClickListener((v) -> {
            presenter.onNext(clickEvent, v);
        });

        if (!isPrimary) {
            buttonAction.setText(R.string.unlock_shackle);
        }
    }

}
