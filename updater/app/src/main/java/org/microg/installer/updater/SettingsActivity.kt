package org.microg.installer.updater

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.microg.installer.updater.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences("updater_settings", Context.MODE_PRIVATE)

        binding.switchAutoCheck.isChecked = prefs.getBoolean("auto_check_updates", true)
        binding.switchAutoInstall.isChecked = prefs.getBoolean("auto_install_updates", false)
        binding.switchWifiOnly.isChecked = prefs.getBoolean("wifi_only", true)

        binding.switchAutoCheck.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_check_updates", isChecked).apply()
        }

        binding.switchAutoInstall.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_install_updates", isChecked).apply()
        }

        binding.switchWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("wifi_only", isChecked).apply()
        }
    }
}
