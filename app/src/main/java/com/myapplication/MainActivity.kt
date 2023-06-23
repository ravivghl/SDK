package com.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geokey.mylibrary.masterlock.MockLockDataProvider
import com.geokey.mylibrary.masterlock.adapter.LockListAdapter
import com.geokey.mylibrary.masterlock.adapter.LockListAdapter.ListType
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLLockScannerDelegate
import com.geokey.mylibrary.masterlock.bluetoothdelegates.MLProductDelegate
import com.geokey.mylibrary.masterlock.bluetoothdelegates.models.LockData
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents.ActivityPaused
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents.ActivityResumed
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents.LocationPermissionDenied
import com.geokey.mylibrary.masterlock.presenter.MasterLockEvents.UserGrantedPermission
import com.geokey.mylibrary.masterlock.presenter.MasterLockPresenter
import com.geokey.mylibrary.masterlock.viewmodel.MasterLockViewModel
import com.geokey.mylibrary.noke.Check
import java.util.Objects

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity(), MasterLockPresenter.LockActivityLauncher,{

    lateinit var masterLockPresenter: MasterLockPresenter

    var locationPermissionsList: Array<String> = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var android12PermissionList: Array<String> = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private var apiErrorDialog: AlertDialog? = null
    private var rationaleDialog: AlertDialog? = null
    private val sdkLicenseErrorDialog: AlertDialog? = null

    // UI Elements
    private var locksList: RecyclerView? = null
    private var shacklesList: RecyclerView? = null
    private var singleLocksList: RecyclerView? = null
    private var headingLocksList: TextView? = null
    private var wakeLockWarning: TextView? = null
    private var warningPermission: TextView? = null
    private var statusScanning: TextView? = null
    private var warningBluetooth: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpKeys(BuildConfig.CPAPIURL, BuildConfig.CPAPILICENSE, BuildConfig.CPAPIPASSWORD)



        masterLockPresenter = MasterLockPresenter(
            this,
            MLLockScannerDelegate(),
            MLProductDelegate()
        )
        masterLockPresenter.initMasterLock(BuildConfig.MLSDKLICENSE, this)

        initializeLayout()


    }

    private fun setUpKeys(cpapiurl: String, cpapilicense: String, cpapipassword: String) {
        MockLockDataProvider.MLBaseURL = cpapiurl
        MockLockDataProvider.MLLicense = cpapilicense
        MockLockDataProvider.MLLicensePassword = cpapipassword
    }

    override fun getLocationPermission() {
        checkShouldShowPermissionRationale(true)
    }

    override fun hasBluetoothScanningPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            hasLegacyBluetoothPermissions()
        } else hasAndroid12BluetoothPermissions()
    }

    private fun hasLegacyBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAndroid12BluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else false
    }

    override fun showPermissionRationale() {
        if (rationaleDialog == null || !rationaleDialog?.isShowing!!) {
            val builder = AlertDialog.Builder(this)
            rationaleDialog = builder.setTitle(R.string.location_permission_required)
                .setMessage(R.string.this_app_needs_your_permission)
                .setPositiveButton(R.string.ok) { dialog, w -> showPermissionRequestDialog() }
                .show()
        }
    }

    override fun showCPAPIError(message: String?) {
        if (apiErrorDialog == null || !apiErrorDialog!!.isShowing) {
            val builder = AlertDialog.Builder(this)
            apiErrorDialog = builder.setTitle(R.string.connected_products_api_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { dialog, w -> dialog.dismiss() }
                .show()
        }
    }

    override fun initializeLockScanner(delegate: MLLockScannerDelegate?) {
        masterLockPresenter.initializeLockScanner(delegate)

    }

    override fun showSDKLicenseError() {

    }

    override fun updateUi(viewModel: MasterLockViewModel?) {
        runOnUiThread {
            updateLocationPermissionUI(viewModel!!)
            updateStatusBanner(viewModel.isSdkScanning, statusScanning!!)
            updateStatusBanner(viewModel.isBluetoothDown, warningBluetooth!!)
            updateLockList(locksList!!, viewModel)
            updateLockList(shacklesList!!, viewModel)
            updateLockList(singleLocksList!!, viewModel)
            if (Objects.requireNonNull<RecyclerView.Adapter<*>?>(shacklesList!!.adapter)
                    .itemCount == 0 && Objects.requireNonNull<RecyclerView.Adapter<*>?>(
                    singleLocksList!!.adapter
                ).itemCount == 0 && shacklesList!!.adapter!!.itemCount == 0
            ) {
                updateStatusBanner(viewModel.isSdkScanning, wakeLockWarning!!)
                updateStatusBanner(!viewModel.isSdkScanning, headingLocksList!!)
            } else {
                updateStatusBanner(false, wakeLockWarning!!)
                updateStatusBanner(true, headingLocksList!!)
            }
        }

    }

    private fun updateLockList(locksList: RecyclerView, viewModel: MasterLockViewModel) {
        (Objects.requireNonNull(locksList.adapter) as LockListAdapter).setItems(
            viewModel.productHashMap,
            viewModel.lockDataHashMap
        )
    }

    private fun updateStatusBanner(statusElement: Boolean, uiElement: TextView) {
        val uiElementVisibility = if (statusElement) View.VISIBLE else View.GONE
        uiElement.visibility = uiElementVisibility
    }

    private fun updateLocationPermissionUI(viewModel: MasterLockViewModel) {
        updateStatusBanner(viewModel.isPermissionDenied, warningPermission!!)
        if (viewModel.isPermissionDenied) {
            warningPermission!!.setOnClickListener { v: View? -> goToSettings() }
        }
    }

    override fun launchFirmwareUpdateDialog(deviceId: String?) {

    }


    override fun showToast(text: String?) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    private val android12LocationPermissionLauncher =
        registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback<Map<String, Boolean>> { map: Map<String, Boolean> ->
                if (!map.values.isEmpty() && map.values.stream()
                        .noneMatch { granted: Boolean? -> granted!! }
                ) {
                    // Location isn't required for Android 12 when targeting the latest API,
                    // Lock Location will not be included in encounter data unless this permission is granted
                }
            })
    private val bluetoothPermissionRequestLauncher =
        registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback<Map<String, Boolean>> { map: Map<String, Boolean> ->
                if (!map.values.isEmpty() && map.values.stream()
                        .allMatch { granted: Boolean? -> granted!! }
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        android12LocationPermissionLauncher.launch(locationPermissionsList)
                    }
                    masterLockPresenter.onNext(UserGrantedPermission(), null)
                } else {
                    checkShouldShowPermissionRationale(false)
                }
            })

    private fun checkShouldShowPermissionRationale(canAskAgain: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            checkSystemPermissionState(
                canAskAgain,
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSystemPermissionState(
                canAskAgain,
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) || shouldShowRequestPermissionRationale(
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            showPermissionRequestDialog()
        }
    }

    private fun checkSystemPermissionState(canAskAgain: Boolean, shouldShowRationale: Boolean) {
        if (shouldShowRationale) {
            showPermissionRationale()
        } else if (canAskAgain) {
            showPermissionRequestDialog()
        } else {
            masterLockPresenter.onNext(LocationPermissionDenied(), null)
        }
    }

    private fun showPermissionRequestDialog() {
        val permissions: Array<String> = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            locationPermissionsList
        } else {
            android12PermissionList
        }
        bluetoothPermissionRequestLauncher.launch(permissions)
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        masterLockPresenter.onNext(ActivityResumed(), null)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
        //    masterLockSDK.stopSDK()
        } catch (ignored: IllegalStateException) {
        }
        apiErrorDialog = null
    }

    override fun onPause() {
        super.onPause()
        masterLockPresenter.onNext(ActivityPaused(), null)
    }

    private fun initializeLayout() {
        locksList = findViewById<RecyclerView>(R.id.locksList)
        shacklesList = findViewById<RecyclerView>(R.id.shacklesList)
        singleLocksList = findViewById<RecyclerView>(R.id.singleLocksList)
        locksList?.layoutManager = getLayoutManager()
        shacklesList?.layoutManager = getLayoutManager()
        singleLocksList?.layoutManager = getLayoutManager()
        warningPermission = findViewById<TextView>(R.id.warning_location)
        statusScanning = findViewById<TextView>(R.id.scanning_status_bar)
        warningBluetooth = findViewById<TextView>(R.id.warning_bluetooth)
        wakeLockWarning = findViewById<TextView>(R.id.wake_lock_warning)
        headingLocksList = findViewById(R.id.heading_locks_list)
        setListAdapter(locksList, ListType.PortableLockBoxPrimary)
        setListAdapter(shacklesList, ListType.PortableLockBoxSecondary)
        setListAdapter(singleLocksList, ListType.SingleLock)
    }

    private fun getLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun setListAdapter(locksList: RecyclerView?, listType: ListType) {
        if (locksList?.adapter == null) {
            val lockListAdapter = LockListAdapter(listType, ArrayList(), HashMap<String, LockData>(), masterLockPresenter)
            locksList?.adapter = lockListAdapter
        }
    }

}