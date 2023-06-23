package com.geokey.mylibrary.masterlock.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.geokey.mylibrary.masterlock.viewholders.LockItemViewHolder;
import com.geokey.mylibrary.masterlock.viewholders.LockedLockItemViewHolder;
import com.geokey.mylibrary.masterlock.viewholders.NeedProfileLockItemViewHolder;
import com.geokey.mylibrary.masterlock.viewholders.UnlockedDeadboltItemViewHolder;
import com.geokey.mylibrary.masterlock.viewholders.UnlockedItemViewHolder;
import com.masterlock.mlbluetoothsdk.MLProduct;
import com.masterlock.mlbluetoothsdk.lockstate.MechanismState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class LockListAdapter extends RecyclerView.Adapter<LockItemViewHolder> {
    final ListType listType;
    public List<MLProduct> items;
    public HashMap<String, LockData> metaData;
    final private MasterLockPresenter presenter;

    public enum ListType {
        PortableLockBoxPrimary,
        PortableLockBoxSecondary,
        SingleLock,
    }
    public LockListAdapter(ListType listType,
                           List<MLProduct> items,
                           HashMap<String, LockData> metaData,
                           MasterLockPresenter presenter) {
        this.listType = listType;
        this.items = items;
        this.metaData = metaData;
        this.presenter = presenter;
    }

    public void setItems(HashMap<String, MLProduct> productHashMap, HashMap<String, LockData> lockDataHashMap) {
        this.items.clear();
        this.items.addAll(filterVisibleItems(productHashMap.values()));
        this.metaData = lockDataHashMap;
        notifyDataSetChanged();
    }

    public enum LockViewType {
        VisibleNoProfile(0),
        VisibleUnknownMechanismState(1),
        Locked(2),
        Unlocked(3),
        Open(4),
        PendingUnlock(5),
        RequestingProfile(6),
        UnlockedDeadbolt(7);

        public final int value;

        LockViewType(int value) {
            this.value = value;
        }

        /**
         * the LockState Represents the State of the Lock and its component locking mechanismState(s).
         * Most locks have only one mechanism (e.g. the shackle on a padlock, or the open switch on a door controller.)
         * A Portable Lock Box has two: the door is the primary mechanism and the removable shackle is secondary.
         * For devices that only have one MechanismState
         * secondary will always have a MechanismState of 'Unknown'.
         */
        static LockViewType of(MechanismState mechanismState, boolean isDeadbolt) {
            switch (mechanismState.getValue()) {
                case Unknown:
                    return LockViewType.VisibleUnknownMechanismState;
                case Locked:
                case OpenLocked:
                    return LockViewType.Locked;
                case PendingUnlock:
                case PendingRelock:
                    return LockViewType.PendingUnlock;
                case Open:
                    return LockViewType.Open;
                case Unlocked:
                    return isDeadbolt ? LockViewType.UnlockedDeadbolt : LockViewType.Unlocked;
            }

            return LockViewType.VisibleUnknownMechanismState;
        }


    }


    private List<MLProduct> filterVisibleItems(Collection<MLProduct> allItems) {
        List<MLProduct> filteredItems;
        switch (listType) {
            case SingleLock:
                filteredItems = allItems
                        .stream()
                        .filter(product -> !hasAShackle(product) && product.state().isVisible())
                        .collect(Collectors.toList());
                return filteredItems;
            case PortableLockBoxPrimary:
            case PortableLockBoxSecondary:
                filteredItems = allItems
                        .stream()
                        .filter(product -> product.state().isVisible() && hasAShackle(product))
                        .collect(Collectors.toList());
                return filteredItems;

            default:
                filteredItems = new ArrayList<>();
                return filteredItems;
        }

    }


    @Override
    public int getItemViewType(int position) {
        MLProduct product = items.get(position);
        LockData productMetaData = metaData.get(product.deviceId);

        if (productMetaData == null) {
            return LockViewType.VisibleNoProfile.value;
        }

        if (productMetaData.isRequestingProfile) {
            return LockViewType.RequestingProfile.value;
        }

        if (!productMetaData.hasValidProfile()) {
            return LockViewType.VisibleNoProfile.value;
        }

        if (listType == ListType.PortableLockBoxSecondary) {
            return LockViewType.of(product.state().getSecondary(), productMetaData.isDeadbolt()).value;
        } else {
            return LockViewType.of(product.state().getPrimary(), productMetaData.isDeadbolt()).value;
        }
    }


    @NonNull
    @NotNull
    @Override
    public LockItemViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;

        switch (LockViewType.values()[viewType]) {

            case Locked:
            case VisibleUnknownMechanismState:
                // In this demo, we are inferring the state of the Visible lock's
                // locking mechanism before the state has actually been read for the first time and showing it the same as "Locked")
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_locked, parent, false);
                return new LockedLockItemViewHolder(view);
            case Unlocked:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_unlocked, parent, false);
                return new UnlockedItemViewHolder(view);
            case VisibleNoProfile:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_available_no_profile, parent, false);
                return new NeedProfileLockItemViewHolder(view);
            case Open:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_open, parent, false);
                return new LockItemViewHolder(view);
            case PendingUnlock:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_pending_unlock, parent, false);
                return new LockItemViewHolder(view);
            case RequestingProfile:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_requesting_profile, parent, false);
                return new LockItemViewHolder(view);
            case UnlockedDeadbolt:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_unlocked_deadbolt, parent, false);
                return new UnlockedDeadboltItemViewHolder(view);
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_lock_available_no_profile, parent, false);
                return new LockItemViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull @NotNull LockItemViewHolder holder, int position) {
        MLProduct product = items.get(position);
        MechanismState lockState = (listType != ListType.PortableLockBoxSecondary) ? product.state().getPrimary() : product.state().getSecondary();
        LockData lockData = metaData.get(product.deviceId);
        holder.bind(product, lockState, lockData, (listType != ListType.PortableLockBoxSecondary), presenter);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean hasAShackle(MLProduct product) {
        return (metaData.values()
                .stream()
                .anyMatch(lockData -> lockData.deviceIdentifier.equals(product.deviceId) && lockData.hasSecondaryLock()))
                || product.mechanismOptions().length > 1;
    }

}
