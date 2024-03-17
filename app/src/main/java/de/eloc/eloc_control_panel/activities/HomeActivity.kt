package de.eloc.eloc_control_panel.activities

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AppState
import de.eloc.eloc_control_panel.data.adapters.ElocInfoAdapter
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.databinding.ActivityHomeBinding
import de.eloc.eloc_control_panel.databinding.LayoutNavHeaderBinding
import de.eloc.eloc_control_panel.receivers.BluetoothDeviceReceiver
import de.eloc.eloc_control_panel.services.StatusUploadService

// todo: use cached profile picture

class HomeActivity : ThemableActivity() {

    private var firstScanDone = false
    private lateinit var binding: ActivityHomeBinding
    private lateinit var leftHeaderBinding: LayoutNavHeaderBinding
    private lateinit var rightHeaderBinding: LayoutNavHeaderBinding
    private var isFirstRun = true
    private val elocAdapter = ElocInfoAdapter(this::onListUpdated, this::showDevice)
    private val elocReceiver = BluetoothDeviceReceiver(elocAdapter::add)
    private lateinit var preferencesLauncher: ActivityResultLauncher<Intent>
    private val backPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val leftHeader = binding.leftDrawer.getHeaderView(0)
        val rightHeader = binding.rightDrawer.getHeaderView(0)
        leftHeaderBinding = LayoutNavHeaderBinding.bind(leftHeader)
        rightHeaderBinding = LayoutNavHeaderBinding.bind(rightHeader)

        setLaunchers()
        setListeners()

        binding.swipeRefreshLayout.setColorSchemeColors(Color.WHITE)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimary)

        binding.drawer.visibility = View.GONE
        binding.progressIndicator.visibility = View.VISIBLE
        prepareStatusUpload(true)
    }

    override fun onResume() {
        super.onResume()
        setAppBar()
        setProfileInfo()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> toggleDrawer()
            R.id.mnu_overflow -> toggleDrawer()
        }
        return true
    }

    private fun runFirstScan() {
        if (firstScanDone) {
            return
        }
        val deviceCount = binding.devicesRecyclerView.adapter?.itemCount ?: 0
        val hasNoDevices = (deviceCount == 0)
        if (hasNoDevices) {
            startScan()
        }
    }

    private fun backPressed() {
        if (drawerOpen()) {
            closeDrawer()
        } else {
            backPressedHandler.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        BluetoothHelper.stopScan(this::scanUpdate)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(elocReceiver)
    }

    private fun openUserPrefs() {
        val intent = Intent(this, UserPrefsActivity::class.java)
        preferencesLauncher.launch(intent)
    }

    private fun initialize() {
        binding.drawer.visibility = View.VISIBLE
        binding.progressIndicator.visibility = View.GONE
        closeDrawer()
        registerReceiver(elocReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        setupListView()
        runFirstScan()
    }

    private fun drawerOpen(): Boolean {
        return binding.drawer.isDrawerOpen(GravityCompat.START) ||
                binding.drawer.isDrawerOpen(GravityCompat.END)
    }

    private fun toggleDrawer() {
        if (drawerOpen()) {
            closeDrawer()
        } else {
            openDrawer()
        }
    }

    private fun closeDrawer() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START)
        } else if (binding.drawer.isDrawerOpen(GravityCompat.END)) {
            binding.drawer.closeDrawer(GravityCompat.END)
        }
    }

    private fun openDrawer() {
        if (!drawerOpen()) {
            val direction = if (PreferencesHelper.instance.isMainMenuOnLeft())
                GravityCompat.START
            else
                GravityCompat.END
            binding.drawer.openDrawer(direction)
            backPressedHandler.isEnabled = true
        }
    }

    private fun setAppBar() {
        binding.toolbar.menu.clear()
        if (PreferencesHelper.instance.isMainMenuOnLeft()) {
            binding.toolbar.setNavigationIcon(R.drawable.menu)
            binding.toolbar.setNavigationIconTint(Color.WHITE)
        } else {
            binding.toolbar.navigationIcon = null
            menuInflater.inflate(R.menu.app_bar_more, binding.toolbar.menu)
        }
    }

    private fun setLaunchers() {
        preferencesLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val fontSizeChanged = result.data?.getBooleanExtra(
                        UserPrefsActivity.EXTRA_FONT_SIZE_CHANGED,
                        false
                    )
                        ?: false
                    if (fontSizeChanged) {
                        MaterialAlertDialogBuilder(this)
                            .setCancelable(false)
                            .setTitle(R.string.app_restart_required)
                            .setMessage(R.string.app_restart_message)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                finish()
                            }
                            .show()
                    }
                }
            }
    }

    private fun setListeners() {
        onBackPressedDispatcher.addCallback(backPressedHandler)
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.refreshListButton.setOnClickListener { startScan() }
        binding.swipeRefreshLayout.setOnRefreshListener { startScan() }

        leftHeaderBinding.editButton.setOnClickListener { editProfile() }
        rightHeaderBinding.editButton.setOnClickListener { editProfile() }

        binding.leftDrawer.setNavigationItemSelectedListener { onNavItemSelected(it) }
        binding.rightDrawer.setNavigationItemSelectedListener { onNavItemSelected(it) }

        binding.toolbar.setNavigationOnClickListener { toggleDrawer() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.mnu_overflow) {
                toggleDrawer()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setProfileInfo() {
        if (!AppState.hasValidProfile) {
            showModalAlert(
                getString(R.string.ranger_profile_missing),
                getString(R.string.ranger_profile_missing_details)
            )
            return
        }

        leftHeaderBinding.profilePictureImageView.setImageUrl(
            AppState.profilePictureUrl,
            HttpHelper.instance.imageLoader
        )
        rightHeaderBinding.profilePictureImageView.setImageUrl(
            AppState.profilePictureUrl,
            HttpHelper.instance.imageLoader
        )
        leftHeaderBinding.userIdTextView.text = AppState.rangerName
        rightHeaderBinding.userIdTextView.text = AppState.rangerName
        leftHeaderBinding.emailAddressTextView.text = AppState.emailAddress
        rightHeaderBinding.emailAddressTextView.text = AppState.emailAddress
        binding.toolbar.subtitle = getString(R.string.eloc_user, AppState.rangerName)

        if (isFirstRun) {
            initialize()
        }
        isFirstRun = false
    }

    private fun editProfile() {
        closeDrawer()
        open(ProfileActivity::class.java, false)
    }

    private fun setupListView() {
        binding.devicesRecyclerView.adapter = elocAdapter
        binding.devicesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun startScan() {
        closeDrawer()
        val isOn = BluetoothHelper.isAdapterOn
        binding.statusTextView.text =
            if (isOn)
                getString(R.string.scanning_eloc_devices)
            else
                "<bluetooth is disabled>"
        if (!isOn) {
            return
        }

        firstScanDone = true
        binding.swipeRefreshLayout.isRefreshing = true
        if (!BluetoothHelper.isScanning) {
            elocAdapter.clear()
        }

        onListUpdated(false)
        val error = BluetoothHelper.startScan(this::scanUpdate)
        if (error?.isNotEmpty() == true) {
            binding.coordinator.showSnack(error)
        }
    }

    private fun scanUpdate(remaining: Int) {
        runOnUiThread {
            var label = getString(R.string.refresh)
            if (remaining > 0) {
                label = getString(R.string.stop_template, remaining)
            } else {
                onListUpdated(scanFinished = true)
            }
            binding.refreshListButton.text = label
        }
    }

    private fun onListUpdated(scanFinished: Boolean) {
        runOnUiThread {
            val hasEmptyAdapter = binding.devicesRecyclerView.adapter?.itemCount == 0
            binding.refreshListButton.visibility = View.VISIBLE
            binding.devicesRecyclerView.visibility =
                if (hasEmptyAdapter) View.GONE else View.VISIBLE
            if (scanFinished) {
                binding.swipeRefreshLayout.isRefreshing = false
                if (hasEmptyAdapter) {
                    binding.statusTextView.visibility = View.VISIBLE
                    binding.statusTextView.text = getString(R.string.no_devices_found)
                } else {
                    binding.statusTextView.visibility = View.GONE
                }
            } else {
                binding.statusTextView.visibility = View.VISIBLE
                binding.statusTextView.text = getString(R.string.scanning_eloc_devices)
            }
        }
    }

    private fun showDevice(address: String) {
        BluetoothHelper.stopScan(this::scanUpdate)
        val intent = Intent(this, DeviceActivity::class.java)
            .apply {
                putExtra(DeviceActivity.EXTRA_DEVICE_ADDRESS, address)
            }
        startActivity(intent)
    }

    private fun showMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun onNavItemSelected(item: MenuItem): Boolean {
        closeDrawer()
        return onMenuItemSelected(item)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.mnu_sign_out -> {
                signOut()
                return true
            }

            R.id.mnu_preferences -> {
                openUserPrefs()
                return true
            }

            R.id.mnu_bluetooth_settings -> {
                BluetoothHelper.openSettings(this)
                return true
            }

            R.id.mnu_account -> {
                manageAccount()
                return true
            }

            R.id.mnu_about -> {
                showAboutApp()
                return true
            }

            R.id.mnu_upload_eloc_status -> {
                prepareStatusUpload()
                return true
            }

            R.id.mnu_find_my_eloc -> {
                showMap()
                return true
            }

            R.id.mnu_browse_eloc_status -> {
                showStatusUpdates()
                return true
            }
        }

        return false
    }

    private fun prepareStatusUpload(inBackground: Boolean = false) {
        if (StatusUploadService.isRunning) {
            binding.coordinator.showSnack(getString(R.string.upload_in_progress))
            return
        } else if (inBackground) {
            StatusUploadService.start(this, true)
        }
    }

    private fun signOut() {
        val title = getString(R.string.confirm)
        val message = getString(R.string.confirm_sign_out)
        showModalOptionAlert(
            title = title,
            message = message,
            positiveButtonLabel = getString(R.string.yes),
            positiveCallback = { AuthHelper.instance.signOut(this@HomeActivity) },
        )
    }

    private fun manageAccount() {
        open(AccountActivity::class.java, false)
    }

    private fun showAboutApp() {
        val title = getString(R.string.app_name)
        val message = getString(R.string.version_template, App.versionName)
        showModalAlert(title, message)
    }

    private fun showStatusUpdates() {
        openUrl(App.instance.getString(R.string.status_updates_url))
    }
}
