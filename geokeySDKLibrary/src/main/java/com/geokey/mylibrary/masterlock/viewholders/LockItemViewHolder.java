package com.geokey.mylibrary.masterlock.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;

import org.jetbrains.annotations.NotNull;

public class LockItemViewHolder extends RecyclerView.ViewHolder {

    TextView textId;
    TextView textStatus;
    TextView buttonAction;



    public LockItemViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
        textId = itemView.findViewById(R.id.textId);
        textStatus = itemView.findViewById(R.id.textStatus);

        initializeViews();
    }

    public void initializeViews() {
        buttonAction = itemView.findViewById(R.id.buttonAction);
    }

    public void bind(MLProduct product,
                     MechanismState mechanismState,
                     @Nullable LockData lockData,
                     boolean isPrimary,
                     MasterLockPresenter presenter) {

        textId.setText(product.deviceId);

    }



}
