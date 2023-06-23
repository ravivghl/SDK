package com.geokey.mylibrary.masterlock.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;

import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;

import org.jetbrains.annotations.NotNull;

public class UnlockedItemViewHolder extends LockItemViewHolder {
    TextView countDown;

    public UnlockedItemViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
        initializeViews();
    }

    @Override
    public void initializeViews() {
        countDown = itemView.findViewById(R.id.countDown);
    }

    @Override
    public void bind(MLProduct product,
                     MechanismState mechanismState,
                     @Nullable @org.jetbrains.annotations.Nullable LockData lockData,
                     boolean isPrimary, MasterLockPresenter presenter) {

        product.getFirmwareVersion();

        Integer firmwareVersion = product.getManager().firmwareVersion;


        countDown.setText(String.valueOf(mechanismState.countdown()));

    }
}
