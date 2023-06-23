package com.geokey.mylibrary.masterlock.dialog;

import static com.masterlock.mlbluetoothsdk.enums.MLFirmwareUpdateState.Applying;
import static com.masterlock.mlbluetoothsdk.enums.MLFirmwareUpdateState.Complete;
import static com.masterlock.mlbluetoothsdk.enums.MLFirmwareUpdateState.Validating;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData;
import com.geokey.mylibrary.masterlock.online.models.FirmwareVersionItem;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.masterlock.mlbluetoothsdk.enums.MLFirmwareUpdateState;
import com.masterlock.mlbluetoothsdk.models.MLFirmwareUpdateStatus;
import com.masterlock.mlbluetoothsdk.online.VESdkWebClient;
import com.masterlock.mlbluetoothsdk.online.enums.UpdateType;

import java.util.ArrayList;

public class FirmwareUpdateDialogFragment extends DialogFragment implements MasterLockEvents.DoFirmwareUpdate.FirmwareUpdateStatusListener {
    private ProgressBar progressBar;

    private AutoCompleteTextView versionPicker;

    private ArrayList<FirmwareVersionItem> versions;

    private ViewSwitcher switcher;

    private Button doUpdateButton;

    private Button cancelButton;

    private Integer selectedUpdateVersion = null;

    private TextView statusText;

    private TextView progressText;

    private TextView tvCurrentVersion;
    private boolean isValidated = false;

    private int currentProgress = 0;

    private boolean isStarted = false;

    private MasterLockPresenter presenter;

    private String deviceId;

    private LockData lockData;
    private int startVersion;
    public static FirmwareUpdateDialogFragment newInstance(String deviceId, MasterLockPresenter presenter) {
        FirmwareUpdateDialogFragment fragment = new FirmwareUpdateDialogFragment();
        fragment.presenter = presenter;
        fragment.deviceId = deviceId;
        fragment.lockData = presenter.state.lockDataHashMap.get(deviceId);
        if (fragment.lockData != null) {
            fragment.versions = fragment.lockData.firmwareVersions;
            fragment.startVersion = fragment.lockData.firmwareVersion;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_update_firmware, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        versionPicker = view.findViewById(R.id.firmwareVersionDropDown);
        switcher = view.findViewById(R.id.switcher);
        doUpdateButton = view.findViewById(R.id.doUpdateButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        statusText = view.findViewById(R.id.statusText);
        progressText = view.findViewById(R.id.progressText);
        tvCurrentVersion = view.findViewById(R.id.tvCurrentVersion);
        tvCurrentVersion.setText(String.format(getString(R.string.current_version), startVersion));
        setCancelable(false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setFirmwareVersionPicker(versions);
        observePicker();
        cancelButton.setOnClickListener((v) -> {
            dismiss();
        });
    }

    private void doFirmwareUpdate(int selectedUpdateVersion) {
        if (!isStarted) {
            this.selectedUpdateVersion = selectedUpdateVersion;
            presenter.onNext(new MasterLockEvents.DoFirmwareUpdate(deviceId, selectedUpdateVersion, this), null);
        }
    }

    @Override
    public void onNext(MLFirmwareUpdateStatus status) {
        currentProgress = status.percentComplete;
        getActivity().runOnUiThread(() -> {

            switch (status.state) {

                case Applying:
                    progressBar.setProgress(currentProgress);
                    statusText.setText(R.string.please_wait);
                    progressText.setText(getString(R.string.percent_complete, currentProgress));
                    setCancelable(false);
                    if (startVersion == selectedUpdateVersion && currentProgress == 100) {
                        // Because new version matches old, we won't get validating/complete
                        manuallyAcknowledgeFirmwareUpdate();
                    }
                    break;

                case Complete:
                    currentProgress = 100;
                    if (isValidated) {
                        statusText.setText(status.state.name());
                        dismissAfterdelay();
                    } else {
                        progressText.setText(getString(R.string.percent_complete, currentProgress));
                    }
                    break;


                case Validating:
                    progressText.setText("");
                    statusText.setText(status.state.name());
                    isValidated = true;
                    break;

                case Error:
                    statusText.setText(status.state.name() + ": " + status.error.getMessage());
                    status.error.printStackTrace();
                    setCancelable(true);
                    break;

                default:
                    statusText.setText(status.state.name());
                    setCancelable(false);
                    break;
            }
        });
    }

    private void dismissAfterdelay() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 2000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // when updating to the same version, the automatic acknowledgement will not be triggered.
    // Here we manually ack the update
    private void manuallyAcknowledgeFirmwareUpdate() {
        statusText.setText(Validating.name());
        VESdkWebClient.getClient().acknowledgeUpdateComplete(deviceId, selectedUpdateVersion, UpdateType.Firmware, (r, e) -> {
            if (e != null) {
                statusText.setText("Unable to manually acknowledge update to " + selectedUpdateVersion + "cause: \n" +
                        e.getMessage());
            } else {
                getActivity().runOnUiThread(() -> {
                    statusText.setText(Complete.name());
                });
                dismissAfterdelay();
            }
        });
    }

    private void observePicker() {
        if (switcher.getCurrentView().getId() == R.id.versionPickerLayout) {
            versionPicker.setEnabled(true);
            versionPicker.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    selectedUpdateVersion = Integer.parseInt((String) versionPicker.getText().toString());
                    doUpdateButton.setEnabled(true);
                    doUpdateButton.setOnClickListener(v -> {
                        Log.d("FWUPDATE", "Version picked = " + selectedUpdateVersion);
                        switcher.showNext();
                        doFirmwareUpdate(selectedUpdateVersion);
                    });
                }
            });


        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        currentProgress = 0;
        progressBar = null;
        try {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFirmwareVersionPicker(ArrayList<FirmwareVersionItem> versions) {
        ArrayList<String> versionStrings = new ArrayList<>();
        for (FirmwareVersionItem versionItem : versions) {
            versionStrings.add(Integer.toString(versionItem.version));
        }
        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(requireContext(), R.layout.drop_down_list_item, versionStrings);
        versionPicker.setThreshold(199);
        versionPicker.setAdapter(adapter);
        versionPicker.setInputType(EditorInfo.TYPE_NULL);
        versionPicker.performCompletion();
    }


}
