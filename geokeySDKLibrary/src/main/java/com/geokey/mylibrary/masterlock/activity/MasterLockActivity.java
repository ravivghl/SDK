package com.geokey.mylibrary.masterlock.activity;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.geokey.mylibrary.R;
import com.geokey.mylibrary.masterlock.adapter.LockListAdapter;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLLockScannerDelegate;
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLProductDelegate;
import com.geokey.mylibrary.masterlock.dialog.FirmwareUpdateDialogFragment;
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents;
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter;
import com.geokey.mylibrary.masterlock.viewmodel.MasterLockViewModel;
import com.masterlock.mlbluetoothsdk.Interfaces.IMLLockScannerDelegate;
import com.masterlock.mlbluetoothsdk.MLBluetoothSDK;
import com.masterlock.mlbluetoothsdk.MLProduct;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author Aaron Hartman
 * A demo of an Android Activity for interacting with Master Lock bluetooth locks
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class MasterLockActivity extends AppCompatActivity implements MasterLockPresenter.LockActivityLauncher {

    private MLBluetoothSDK masterLockSDK;
    private MasterLockPresenter presenter;

    // UI Elements
    private RecyclerView locksList;
    private RecyclerView shacklesList;
    private RecyclerView singleLocksList;
    private TextView headingLocksList;
    private TextView wakeLockWarning;
    private TextView warningPermission;
    private TextView statusScanning;
    private TextView warningBluetooth;

    String[] locationPermissionsList = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    String[] android12PermissionList = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
    };

    private AlertDialog apiErrorDialog;
    private AlertDialog rationaleDialog;
    private AlertDialog sdkLicenseErrorDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        presenter = new MasterLockPresenter(this,
                MLLockScannerDelegate.getInstance(),
                MLProductDelegate.getInstance());


        initializeLayout();

        try {
            masterLockSDK = MLBluetoothSDK.getInstance("BuildConfig.MLSDKLICENSE", this);
            Log.e("Version", masterLockSDK.version());
            presenter.setMasterLockSDK(masterLockSDK);
        } catch (Exception e) {
            presenter.onNext(new MasterLockEvents.SDKLicenseError(), null);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onNext(new MasterLockEvents.ActivityResumed(), null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            masterLockSDK.stopSDK();
        } catch (IllegalStateException ignored) {
        }
        apiErrorDialog = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onNext(new MasterLockEvents.ActivityPaused(), null);
    }

    @Override
    public void getLocationPermission() {
        checkShouldShowPermissionRationale(true);
    }

    @Override
    public boolean hasBluetoothScanningPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return hasLegacyBluetoothPermissions();
        }

        return hasAndroid12BluetoothPermissions();
    }

    private boolean hasLegacyBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAndroid12BluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN

            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    public void showPermissionRationale() {
        if (rationaleDialog == null || !rationaleDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            rationaleDialog = builder.setTitle(R.string.location_permission_required)
                    .setMessage(R.string.this_app_needs_your_permission)
                    .setPositiveButton(R.string.ok, (dialog, w) -> showPermissionRequestDialog())
                    .show();
        }
    }

    @Override
    public void showCPAPIError(String message) {
        if (apiErrorDialog == null || !apiErrorDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            apiErrorDialog = builder.setTitle(R.string.connected_products_api_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, w) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    @Override
    public void initializeLockScanner(MLLockScannerDelegate delegate) {

    }

    @Override
    public void showSDKLicenseError() {
        if (sdkLicenseErrorDialog == null || !sdkLicenseErrorDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            sdkLicenseErrorDialog = builder.setTitle(R.string.error)
                    .setMessage(R.string.sdk_license_error_body)
                    .setPositiveButton(R.string.ok, (dialog, w) -> dialog.dismiss())
                    .setOnDismissListener((d) -> {
                        finish();
                    }).show();
        }
    }

    @Override
    public void updateUi(MasterLockViewModel viewModel) {
        runOnUiThread(() -> {
            updateLocationPermissionUI(viewModel);
            updateStatusBanner(viewModel.isSdkScanning, statusScanning);
            updateStatusBanner(viewModel.isBluetoothDown, warningBluetooth);
            updateLockList(locksList, viewModel);
            updateLockList(shacklesList, viewModel);
            updateLockList(singleLocksList, viewModel);
            if (Objects.requireNonNull(shacklesList.getAdapter()).getItemCount() == 0
                    && Objects.requireNonNull(singleLocksList.getAdapter()).getItemCount() == 0
                    && shacklesList.getAdapter().getItemCount() == 0) {
                updateStatusBanner(viewModel.isSdkScanning, wakeLockWarning);
                updateStatusBanner(!viewModel.isSdkScanning, headingLocksList);
            } else {
                updateStatusBanner(false, wakeLockWarning);
                updateStatusBanner(true, headingLocksList);
            }
        });

    }

    @Override
    public void launchFirmwareUpdateDialog(String deviceId) {
        FirmwareUpdateDialogFragment firmwareUpdateDialogFragment = FirmwareUpdateDialogFragment.newInstance(deviceId, presenter);
        String tag = FirmwareUpdateDialogFragment.class.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(firmwareUpdateDialogFragment, tag).commit();
    }

    @Override
    public void showToast(String text) {
        runOnUiThread(() -> {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateLockList(RecyclerView locksList, MasterLockViewModel viewModel) {
        ((LockListAdapter) Objects.requireNonNull(locksList.getAdapter())).setItems(viewModel.productHashMap, viewModel.lockDataHashMap);


    }

    private void updateStatusBanner(boolean statusElement, TextView uiElement) {
        int uiElementVisibility = (statusElement) ? View.VISIBLE : View.GONE;
        uiElement.setVisibility(uiElementVisibility);
    }

    private void updateLocationPermissionUI(MasterLockViewModel viewModel) {
        updateStatusBanner(viewModel.isPermissionDenied, warningPermission);

        if (viewModel.isPermissionDenied) {
            warningPermission.setOnClickListener((v) -> {
                goToSettings();
            });
        }
    }

    private final ActivityResultLauncher<String[]> android12LocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), map -> {
                if (!map.values().isEmpty() && map.values().stream().noneMatch(granted -> {
                    return granted;
                })) {
                    // Location isn't required for Android 12 when targeting the latest API,
                    // Lock Location will not be included in encounter data unless this permission is granted
                }
            });

    private final ActivityResultLauncher<String[]> bluetoothPermissionRequestLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), map -> {
                if (!map.values().isEmpty() && map.values().stream().allMatch(granted -> {
                    return granted;
                })) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        android12LocationPermissionLauncher.launch(locationPermissionsList);
                    }
                    presenter.onNext(new MasterLockEvents.UserGrantedPermission(), null);
                } else {
                    checkShouldShowPermissionRationale(false);
                }
            });

    private void checkShouldShowPermissionRationale(boolean canAskAgain) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            checkSystemPermissionState(canAskAgain, shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSystemPermissionState(canAskAgain, shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) || shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN));
        } else {
            showPermissionRequestDialog();
        }
    }

    private void checkSystemPermissionState(boolean canAskAgain, boolean shouldShowRationale) {
        if (shouldShowRationale) {
            showPermissionRationale();
        } else if (canAskAgain) {
            showPermissionRequestDialog();
        } else {
            presenter.onNext(new MasterLockEvents.LocationPermissionDenied(), null);
        }
    }

    private void showPermissionRequestDialog() {
        String[] permissions;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions = locationPermissionsList;
        } else {
            permissions = android12PermissionList;
        }

        bluetoothPermissionRequestLauncher.launch(permissions);

    }

    private void goToSettings() {
        Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + "BuildConfig.APPLICATION_ID"));
        startActivity(intent);
    }

    private void initializeLayout() {
        locksList = findViewById(R.id.locksList);
        shacklesList = findViewById(R.id.shacklesList);
        singleLocksList = findViewById(R.id.singleLocksList);
        locksList.setLayoutManager(getLayoutManager());
        shacklesList.setLayoutManager(getLayoutManager());
        singleLocksList.setLayoutManager(getLayoutManager());
        warningPermission = findViewById(R.id.warning_location);
        statusScanning = findViewById(R.id.scanning_status_bar);
        warningBluetooth = findViewById(R.id.warning_bluetooth);
        wakeLockWarning = findViewById(R.id.wake_lock_warning);
        headingLocksList = findViewById(R.id.heading_locks_list);
        setListAdapter(locksList, LockListAdapter.ListType.PortableLockBoxPrimary);
        setListAdapter(shacklesList, LockListAdapter.ListType.PortableLockBoxSecondary);
        setListAdapter(singleLocksList, LockListAdapter.ListType.SingleLock);
    }

    @NotNull
    private LinearLayoutManager getLayoutManager() {
        return new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    }

    private void setListAdapter(RecyclerView locksList, LockListAdapter.ListType listType) {
        if (locksList.getAdapter() == null) {
            LockListAdapter lockListAdapter = new LockListAdapter(listType, new ArrayList<MLProduct>(), new HashMap<>(), presenter);
            locksList.setAdapter(lockListAdapter);
        }
    }
}