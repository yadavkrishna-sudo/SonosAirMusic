package com.example.airstreamclone

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class AudioStreamService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var serverSocket: ServerSocket? = null
    private var isStreaming = false
    private var currentSonosIp: String? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        const val EXTRA_SONOS_IP = "EXTRA_SONOS_IP"

        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 2
        private const val BITS_PER_SAMPLE = 16
        private const val HTTP_PORT = 8080
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "SonosStreamChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundServiceNotification()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                currentSonosIp = intent.getStringExtra(EXTRA_SONOS_IP)

                if (resultData != null && currentSonosIp != null) {
                    startSonosStreaming(resultCode, resultData, currentSonosIp!!)
                }
            }
            ACTION_STOP -> stopStreaming()
        }
        return START_NOT_STICKY
    }

    private fun startSonosStreaming(resultCode: Int, resultData: Intent, sonosIp: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = minBufferSize * 2

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            audioRecord?.startRecording()
            isStreaming = true

            thread(start = true, name = "HttpWavServerThread") {
                runHttpWavServer(bufferSize)
            }

            thread(start = true, name = "SonosControlThread") {
                Thread.sleep(500)
                val phoneIp = getLocalIpAddress()
                SonosController.startSonosStream(sonosIp, phoneIp, HTTP_PORT)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopStreaming()
        }
    }

    private fun runHttpWavServer(bufferSize: Int) {
        try {
            serverSocket = ServerSocket(HTTP_PORT)
            val pcmBuffer = ByteArray(bufferSize)

            while (isStreaming) {
                val client = serverSocket?.accept() ?: break
                thread(start = true) {
                    try {
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        val requestLine = reader.readLine() ?: ""
                        while (reader.readLine().isNullOrEmpty().not()) {}

                        val output = client.getOutputStream()

                        if (requestLine.startsWith("HEAD")) {
                            val headResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: audio/wav\r\n" +
                                    "Connection: close\r\n\r\n"
                            output.write(headResponse.toByteArray())
                            output.flush()
                            client.close()
                            return@thread
                        }

                        val httpHeaders = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: audio/wav\r\n" +
                                "Cache-Control: no-cache\r\n" +
                                "Connection: keep-alive\r\n\r\n"
                        output.write(httpHeaders.toByteArray())
                        output.write(createInfiniteWavHeader())
                        output.flush()

                        while (isStreaming && !client.isClosed) {
                            val read = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                            if (read > 0) {
                                output.write(pcmBuffer, 0, read)
                                output.flush()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        client.close()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createInfiniteWavHeader(): ByteArray {
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = (CHANNELS * BITS_PER_SAMPLE / 8).toShort()
        val maxDataSize = Int.MAX_VALUE - 36

        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(Int.MAX_VALUE)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(CHANNELS.toShort())
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(maxDataSize)
        return buffer.array()
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    private fun stopStreaming() {
        isStreaming = false
        currentSonosIp?.let { ip ->
            thread { SonosController.stopSonosStream(ip) }
        }
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Sonos Audio Streamer", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Streaming to Sonos Play:1")
            .setContentText("Live HTTP WAV Audio Capture Active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }
}
