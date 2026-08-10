package org.microg.installer.updater

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.installer.updater.data.ReleaseChecker
import org.microg.installer.updater.data.ReleaseInfo
import org.microg.installer.updater.databinding.ActivityMainBinding
import org.microg.installer.updater.installer.SystemInstaller
import org.microg.installer.updater.worker.UpdateWorker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentRelease: ReleaseInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.swipeRefresh.setOnRefreshListener {
            checkForUpdates()
        }

        binding.btnCheckNow.setOnClickListener {
            checkForUpdates()
        }

        binding.btnUpdateGms.setOnClickListener {
            val url = currentRelease?.gmsUrl
            if (url != null) {
                installComponent(url, "com.google.android.gms", binding.btnUpdateGms)
            }
        }

        binding.btnUpdateVending.setOnClickListener {
            val url = currentRelease?.vendingUrl
            if (url != null) {
                installComponent(url, "com.android.vending", binding.btnUpdateVending)
            }
        }

        UpdateWorker.scheduleWork(this)
        refreshInstalledVersions()
        checkForUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshInstalledVersions() {
        val gmsVer = getInstalledVersion("com.google.android.gms") ?: "Not installed"
        val vendingVer = getInstalledVersion("com.android.vending") ?: "Not installed"

        binding.gmsInstalledVer.text = getString(R.string.installed_version, gmsVer)
        binding.vendingInstalledVer.text = getString(R.string.installed_version, vendingVer)
    }

    private fun getInstalledVersion(packageName: String): String? {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun checkForUpdates() {
        binding.swipeRefresh.isRefreshing = true
        binding.statusTitle.text = getString(R.string.status_checking)

        lifecycleScope.launch {
            val release = ReleaseChecker.fetchLatestRelease()
            binding.swipeRefresh.isRefreshing = false
            currentRelease = release

            if (release == null) {
                binding.statusTitle.text = "Failed to connect"
                binding.statusSubtitle.text = "Could not check GitHub releases."
                Toast.makeText(this@MainActivity, "Failed to check GitHub releases", Toast.LENGTH_SHORT).show()
                return@launch
            }

            binding.gmsLatestVer.text = getString(R.string.latest_version, release.gmsVersionName ?: "N/A")
            binding.vendingLatestVer.text = getString(R.string.latest_version, release.vendingVersionName ?: "N/A")

            val gmsInstalled = getInstalledVersion("com.google.android.gms")
            val vendingInstalled = getInstalledVersion("com.android.vending")

            val gmsNeedsUpdate = release.gmsUrl != null && (gmsInstalled == null || gmsInstalled != release.gmsVersionName)
            val vendingNeedsUpdate = release.vendingUrl != null && (vendingInstalled == null || vendingInstalled != release.vendingVersionName)

            if (gmsNeedsUpdate) {
                binding.btnUpdateGms.isEnabled = true
                binding.btnUpdateGms.text = getString(R.string.btn_update)
            } else {
                binding.btnUpdateGms.isEnabled = false
                binding.btnUpdateGms.text = getString(R.string.btn_up_to_date)
            }

            if (vendingNeedsUpdate) {
                binding.btnUpdateVending.isEnabled = true
                binding.btnUpdateVending.text = getString(R.string.btn_update)
            } else {
                binding.btnUpdateVending.isEnabled = false
                binding.btnUpdateVending.text = getString(R.string.btn_up_to_date)
            }

            if (gmsNeedsUpdate || vendingNeedsUpdate) {
                binding.statusTitle.text = getString(R.string.status_update_available)
                binding.statusSubtitle.text = "New release v${release.tagName} is available."
                binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.status_amber_bg))
                binding.statusTitle.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_amber))
                binding.statusIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.status_amber))
            } else {
                binding.statusTitle.text = getString(R.string.status_up_to_date)
                binding.statusSubtitle.text = "Installed microG components are at the latest version."
                binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.status_green_bg))
                binding.statusTitle.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_green))
                binding.statusIcon.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.status_green))
            }
        }
    }

    private fun installComponent(url: String, packageName: String, button: com.google.android.material.button.MaterialButton) {
        button.isEnabled = false
        button.text = "Downloading..."

        lifecycleScope.launch {
            val success = SystemInstaller.downloadAndInstall(this@MainActivity, url, packageName) { progress ->
                runOnUiThread {
                    button.text = "$progress%"
                }
            }

            if (success) {
                Toast.makeText(this@MainActivity, "Install request submitted", Toast.LENGTH_SHORT).show()
                refreshInstalledVersions()
            } else {
                Toast.makeText(this@MainActivity, "Installation failed", Toast.LENGTH_SHORT).show()
                button.isEnabled = true
                button.text = getString(R.string.btn_update)
            }
        }
    }
}
