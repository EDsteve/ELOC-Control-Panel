package de.eloc.eloc_control_panel.ng2.activities

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
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.SNTPClient
import de.eloc.eloc_control_panel.UploadFileAsync
import de.eloc.eloc_control_panel.activities.TerminalActivity
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.databinding.ActivityHomeBinding
import de.eloc.eloc_control_panel.databinding.LayoutNavHeaderBinding
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper
import de.eloc.eloc_control_panel.ng2.models.ElocInfoAdapter
import de.eloc.eloc_control_panel.ng2.models.HttpHelper
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper
import de.eloc.eloc_control_panel.ng2.receivers.BluetoothDeviceReceiver
import de.eloc.eloc_control_panel.ng3.activities.LoginActivity
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity
import de.eloc.eloc_control_panel.ng3.activities.open
import de.eloc.eloc_control_panel.ng3.activities.showModalAlert
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
    private lateinit var binding: ActivityHomeBinding
    private lateinit var leftHeaderBinding: LayoutNavHeaderBinding
    private lateinit var rightHeaderBinding: LayoutNavHeaderBinding
    private lateinit var viewModel: UserAccountViewModel
    private lateinit var userId: String
    private val bluetoothHelper = BluetoothHelper.instance
    private val elocAdapter = ElocInfoAdapter(this::onListUpdated, this::showDevice)
    private val elocReceiver = BluetoothDeviceReceiver(elocAdapter::add)
    private var gUploadEnabled = false
    private var mainMenuButton: MenuItem? = null
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
        setViewModel()
        initialize()
        closeDrawer()

        binding.swipeRefreshLayout.setColorSchemeColors(Color.WHITE)
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
    }

    override fun onResume() {
        super.onResume()

        viewModel.getProfile()
        setToolbar()

        val hasNoDevices = binding.devicesRecyclerView.adapter?.itemCount == 0
        if (hasNoDevices) {
            startScan()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        mainMenuButton = menu?.findItem(R.id.mnu_main)
        setToolbar()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> toggleDrawer()
            R.id.mnu_main -> {
                if (drawerOpen()) {
                    closeDrawer()
                } else {
                    openDrawer()
                }
            }
        }
        return true
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
        registerReceiver(elocReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        setLaunchers()
        setListeners()
        setupListView()
        onBackPressedDispatcher.addCallback(backPressedHandler)
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
        mainMenuButton?.setIcon(R.drawable.menu)
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            restoreMenuIcon()
            binding.drawer.closeDrawer(GravityCompat.START)
        } else if (binding.drawer.isDrawerOpen(GravityCompat.END)) {
            binding.drawer.closeDrawer(GravityCompat.END)
        }
    }

    private fun openDrawer() {
        if (!drawerOpen()) {
            val forwardArrow = ContextCompat.getDrawable(this, R.drawable.arrow_forward)
            forwardArrow?.setTint(Color.WHITE)
            mainMenuButton?.icon = forwardArrow
            supportActionBar?.setHomeAsUpIndicator(0) // Show the 'Back/up' arrow
            supportActionBar?.setHomeActionContentDescription(R.string.close_drawer_menu)
            val direction = if (PreferencesHelper.instance.isMainMenuOnLeft())
                GravityCompat.START
            else
                GravityCompat.END
            binding.drawer.openDrawer(direction)
            backPressedHandler.isEnabled = true
        }
    }

    private fun restoreMenuIcon() {
        supportActionBar?.setHomeAsUpIndicator(R.drawable.menu)
        supportActionBar?.setHomeActionContentDescription(R.string.open_drawer_menu)
    }

    private fun setToolbar() {
        if (PreferencesHelper.instance.isMainMenuOnLeft()) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.menu)
            supportActionBar?.setHomeActionContentDescription(R.string.open_drawer_menu)
            mainMenuButton?.isVisible = false
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            mainMenuButton?.isVisible = true
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
        binding.instructionsButton.setOnClickListener { ActivityHelper.showInstructions(this@HomeActivity) }
        binding.refreshListButton.setOnClickListener { startScan() }
        binding.swipeRefreshLayout.setOnRefreshListener { startScan() }

        val leftHeader = binding.leftDrawer.getHeaderView(0)
        val rightHeader = binding.rightDrawer.getHeaderView(0)
        leftHeaderBinding = LayoutNavHeaderBinding.bind(leftHeader)
        rightHeaderBinding = LayoutNavHeaderBinding.bind(rightHeader)
        leftHeaderBinding.editButton.setOnClickListener { editProfile() }
        rightHeaderBinding.editButton.setOnClickListener { editProfile() }

        binding.drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                restoreMenuIcon()
            }
        })
        binding.leftDrawer.setNavigationItemSelectedListener { onNavItemSelected(it) }
        binding.rightDrawer.setNavigationItemSelectedListener { onNavItemSelected(it) }
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        viewModel.watchProfile().observe(this) {
            leftHeaderBinding.profilePictureImageView.setImageUrl(
                it.profilePictureUrl,
                HttpHelper.getInstance().imageLoader
            )
            rightHeaderBinding.profilePictureImageView.setImageUrl(
                it.profilePictureUrl,
                HttpHelper.getInstance().imageLoader
            )
            userId = it.userId
            leftHeaderBinding.userIdTextView.text = it.userId
            rightHeaderBinding.userIdTextView.text = it.userId
            leftHeaderBinding.emailAddressTextView.text = it.emailAddress
            rightHeaderBinding.emailAddressTextView.text = it.emailAddress
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
        binding.swipeRefreshLayout.isRefreshing = true
        val isOn = BluetoothHelper.instance.isAdapterOn()
        binding.statusTextView.text =
            if (isOn)
                getString(R.string.scanning_eloc_devices)
            else
                "<bluetooth is disabled>"
        if (!isOn) {
            return
        }

        if (!bluetoothHelper.isScanning) {
            elocAdapter.clear()
        }

        onListUpdated(false)
        val error = bluetoothHelper.startScan(this::scanUpdate)
        if (!TextUtils.isEmpty(error)) {
            ActivityHelper.showSnack(binding.coordinator, error!!)
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
                        ActivityHelper.showSnack(binding.coordinator, message)
                    }
                }
            }
        } else {
            ActivityHelper.showSnack(binding.coordinator, "Nothing to Upload!")
        }
    }

    private fun showDevice(name: String, address: String) {
        BluetoothHelper.instance.stopScan(this::scanUpdate)
        val intent = Intent(this, TerminalActivity::class.java)
        intent.putExtra(TerminalActivity.EXTRA_RANGER_NAME, userId)
        intent.putExtra(TerminalActivity.EXTRA_DEVICE, address)
        intent.putExtra(TerminalActivity.EXTRA_DEVICE_NAME, name)
        startActivity(intent)
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
                        ActivityHelper.showSnack(
                            binding.coordinator,
                            "sync FAILED\nCheck internet connection"
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
                        ActivityHelper.showSnack(binding.coordinator, message)
                    }
                }
            }
        }
    }

    private fun showMap() {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra(MapActivity.EXTRA_RANGER_NAME, userId)
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
                editAccount()
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
                ActivityHelper.showStatusUpdates(this@HomeActivity)
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

    private fun editAccount() {
        open(AccountActivity::class.java, false)
    }

    private fun showAboutApp() {
        val title = getString(R.string.app_name)
        val message = getString(R.string.version_template, App.versionName)
        showModalAlert(title, message)
    }
}
