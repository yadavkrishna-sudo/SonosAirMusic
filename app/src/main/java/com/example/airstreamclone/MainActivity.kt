package com.example.airstreamclone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var ipInput: EditText
    private lateinit var statusText: TextView

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val sonosIp = ipInput.text.toString().trim()
        if (result.resultCode == Activity.RESULT_OK && result.data != null && sonosIp.isNotEmpty()) {
            val serviceIntent = Intent(this, AudioStreamService::class.java).apply {
                action = AudioStreamService.ACTION_START
                putExtra(AudioStreamService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioStreamService.EXTRA_RESULT_DATA, result.data)
                putExtra(AudioStreamService.EXTRA_SONOS_IP, sonosIp)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            statusText.text = "Status: Streaming live WAV to Sonos ($sonosIp)..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply {
            text = "Status: Searching for Sonos Play:1..."
            textSize = 16f
        }

        ipInput = EditText(this).apply {
            hint = "Sonos Play:1 IP (e.g. 192.168.1.50)"
        }

        val btnDiscover = Button(this).apply { text = "Auto-Discover Sonos Play:1" }
        val btnStart = Button(this).apply { text = "Start Streaming to Sonos" }
        val btnStop = Button(this).apply { text = "Stop Stream" }

        btnDiscover.setOnClickListener { discoverSonos() }

        btnStart.setOnClickListener {
            if (ipInput.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Enter or discover Sonos IP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (checkPermissions()) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
        }

        btnStop.setOnClickListener {
            val stopIntent = Intent(this, AudioStreamService::class.java).apply {
                action = AudioStreamService.ACTION_STOP
            }
            startService(stopIntent)
            statusText.text = "Status: Stream Stopped"
        }

        layout.addView(statusText)
        layout.addView(ipInput)
        layout.addView(btnDiscover)
        layout.addView(btnStart)
        layout.addView(btnStop)
        setContentView(layout)

        discoverSonos()
    }

    private fun discoverSonos() {
        statusText.text = "Status: Scanning Wi-Fi for Sonos Play:1 (SSDP)..."
        thread {
            val foundIp = SonosController.discoverSonosIp()
            runOnUiThread {
                if (foundIp != null) {
                    ipInput.setText(foundIp)
                    statusText.text = "Status: Found Sonos at $foundIp"
                } else {
                    statusText.text = "Status: Auto-discovery timed out. Enter Sonos IP manually."
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
            return false
        }
        return true
    }
}
