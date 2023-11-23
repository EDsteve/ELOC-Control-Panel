package de.eloc.eloc_control_panel.ng3.activities

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.SNTPClient
import de.eloc.eloc_control_panel.UploadFileAsync
import de.eloc.eloc_control_panel.activities.TerminalActivity
import de.eloc.eloc_control_panel.databinding.ActivityHomeBinding
import de.eloc.eloc_control_panel.databinding.LayoutNavHeaderBinding
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper
import de.eloc.eloc_control_panel.ng2.models.ElocInfoAdapter
import de.eloc.eloc_control_panel.ng2.models.HttpHelper
import de.eloc.eloc_control_panel.ng2.receivers.BluetoothDeviceReceiver
import de.eloc.eloc_control_panel.ng3.data.PreferencesHelper
import de.eloc.eloc_control_panel.ng3.data.UserAccountViewModel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// todo: use cached profile picture

class HomeActivity : ThemableActivity() {
    private var firstScanDone = false
    private lateinit var binding: ActivityHomeBinding
    private lateinit var leftHeaderBinding: LayoutNavHeaderBinding
    private lateinit var rightHeaderBinding: LayoutNavHeaderBinding
    private lateinit var viewModel: UserAccountViewModel
    private lateinit var rangerName: String
    private val bluetoothHelper = BluetoothHelper.instance
    private val elocAdapter = ElocInfoAdapter(this::onListUpdated, this::showDevice)
    private val elocReceiver = BluetoothDeviceReceiver(elocAdapter::add)
    private var gUploadEnabled = false
    private var gLastTimeDifferenceMillisecond = 0L
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
        setViewModel()

        binding.swipeRefreshLayout.setColorSchemeColors(Color.WHITE)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimary)

        binding.drawer.visibility = View.GONE
        binding.progressLayout.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        viewModel.getProfileAsync()
        setAppBar()
        runFirstScan()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
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
        val hasRangerName = ::rangerName.isInitialized
        if (hasNoDevices && hasRangerName) {
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
        bluetoothHelper.stopScan(this::scanUpdate)
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
        binding.progressLayout.visibility = View.GONE
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

    private fun setViewModel() {
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        viewModel.profile.observe(this) {
            if (it != null) {
                val isFirstRun = (!::rangerName.isInitialized)
                rangerName = it.userId

                leftHeaderBinding.profilePictureImageView.setImageUrl(
                    it.profilePictureUrl,
                    HttpHelper.getInstance().imageLoader
                )
                rightHeaderBinding.profilePictureImageView.setImageUrl(
                    it.profilePictureUrl,
                    HttpHelper.getInstance().imageLoader
                )
                leftHeaderBinding.userIdTextView.text = it.userId
                rightHeaderBinding.userIdTextView.text = it.userId
                leftHeaderBinding.emailAddressTextView.text = it.emailAddress
                rightHeaderBinding.emailAddressTextView.text = it.emailAddress
                binding.toolbar.subtitle = getString(R.string.eloc_user, it.userId)

                if (isFirstRun) {
                    initialize()
                }
            }
        }
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
        val isOn = BluetoothHelper.instance.isAdapterOn()
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
        if (!bluetoothHelper.isScanning) {
            elocAdapter.clear()
        }

        onListUpdated(false)
        val error = bluetoothHelper.startScan(this::scanUpdate)
        if (!TextUtils.isEmpty(error)) {
            showSnack(binding.coordinator, error!!)
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

    private fun fileToString(file: File): String {
        val text = StringBuilder()
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(file))
            while (true) {
                try {
                    val line = br.readLine() ?: break
                    text.append(line)
                    text.append('\n')
                } catch (_: IOException) {
                    break
                }
            }
        } catch (_: IOException) {
        } finally {
            br?.close()
        }
        return text.toString()
    }

    private fun uploadElocStatus() {
        var filecounter = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val filestring = "update " + sdf.format(Date()) + ".upd"

        var fileout: OutputStreamWriter? = null
        try {
            val files = filesDir.listFiles()
            fileout = OutputStreamWriter(openFileOutput(filestring, Context.MODE_PRIVATE))
            if (files != null) {
                for (file in files) {
                    if (!file.isDirectory) {
                        if (file.name.endsWith(".txt")) {
                            filecounter++
                            fileout.write(fileToString(file) + "\n\n\n")
                        }
                    }
                }
                fileout.write("\n\n\n end of updates")
            }
        } catch (_: Exception) {
        } finally {
            fileout?.close()
        }

        if (filecounter > 0) {
            val filename = filesDir.absolutePath + "/" + filestring
            UploadFileAsync.run(filename, filesDir) { message ->
                runOnUiThread {
                    if (message.trim().isNotEmpty()) {
                        showSnack(binding.coordinator, message)
                    }
                }
            }
        } else {
            showSnack(binding.coordinator, getString(R.string.nothing_to_upload))
        }
    }

    private fun showDevice(deviceName: String, address: String) {
        BluetoothHelper.instance.stopScan(this::scanUpdate)
        val old = false
        if (old) {
            val intent = Intent(this, TerminalActivity::class.java)
            intent.putExtra(TerminalActivity.EXTRA_RANGER_NAME, rangerName)
            startActivity(intent)
        } else {
            val intent = Intent(this, DeviceActivity::class.java)
                .apply {
                    putExtra(DeviceActivity.EXTRA_RANGER_NAME, rangerName)
                    putExtra(DeviceActivity.EXTRA_DEVICE_ADDRESS, address)
                    putExtra(DeviceActivity.EXTRA_DEVICE_NAME, deviceName)
                }
            startActivity(intent)
        }
    }

    private fun synchronizeClock() {
        val timeoutMS = 5000
        val showMessage = true
        SNTPClient.getDate(
            timeoutMS,
            Calendar.getInstance().timeZone
        ) { _,
            _,
            googletimestamp,
            _
            ->
            run {
                if (googletimestamp == 0L) {
                    gUploadEnabled = false
                    invalidateOptionsMenu()
                    if (showMessage) {
                        showSnack(
                            binding.coordinator,
                            getString(R.string.sync_failed_check_internet_connection)
                        )
                    }
                } else {
                    gLastTimeDifferenceMillisecond = System.currentTimeMillis() - googletimestamp
                    PreferencesHelper.instance.saveTimestamps(
                        SystemClock.elapsedRealtime(),
                        googletimestamp
                    )
                    gUploadEnabled = true
                    invalidateOptionsMenu()
                    if (showMessage) {
                        val message =
                            getString(R.string.sync_template, gLastTimeDifferenceMillisecond)
                        showSnack(binding.coordinator, message)
                    }
                }
            }
        }
    }

    private fun showMap() {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra(MapActivity.EXTRA_RANGER_NAME, rangerName)
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

            R.id.mnu_sync_clock -> {
                synchronizeClock()
                return true
            }

            R.id.mnu_preferences -> {
                openUserPrefs()
                return true
            }

            R.id.mnu_bluetooth_settings -> {
                bluetoothHelper.openSettings(this)
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
                uploadElocStatus()
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

    private fun signOut() {
        viewModel.signOut()
        onSignOut()
    }

    private fun onSignOut() {
        open(LoginActivity::class.java, true)
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
