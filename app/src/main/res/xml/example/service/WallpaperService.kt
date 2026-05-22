package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.TriggerType
import com.example.data.WallpaperRepository
import com.example.data.WallpaperRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class WallpaperService : Service(), SensorEventListener {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var repository: WallpaperRepository
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private var acceleration = 10f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                checkRulesAndApply(TriggerType.UNLOCK)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = WallpaperRepository(AppDatabase.getDatabase(this).wallpaperDao())
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
        
        startForegroundService()
        startTimeChecker()
    }

    private fun startForegroundService() {
        val channelId = "wallpaper_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Wallpaper Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dynamic Wallpaper")
            .setContentText("Monitoring triggers for wallpaper changes")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun startTimeChecker() {
        serviceScope.launch {
            while (true) {
                checkRulesAndApply(TriggerType.TIME)
                delay(60000)
            }
        }
    }

    private fun checkRulesAndApply(trigger: TriggerType) {
        serviceScope.launch {
            val rules = repository.allRules.firstOrNull() ?: return@launch
            val activeRules = rules.filter { it.isEnabled && it.triggerType == trigger }
            
            for (rule in activeRules) {
                var shouldApply = false
                when (trigger) {
                    TriggerType.UNLOCK -> shouldApply = true
                    TriggerType.SHAKE -> shouldApply = true
                    TriggerType.TIME -> {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val currentTime = sdf.format(Date())
                        if (currentTime == rule.triggerValue) {
                            shouldApply = true
                        }
                    }
                    else -> {}
                }

                if (shouldApply) {
                    applyWallpaper(rule)
                    break
                }
            }
        }
    }
    
    private var lastShakeTime = 0L

    private suspend fun applyWallpaper(rule: WallpaperRule) {
        val ids = rule.imageIds.split(",").mapNotNull { it.toIntOrNull() }
        if (ids.isEmpty()) return
        
        val nextIndex = (rule.currentImageIndex + 1) % ids.size
        val imageId = ids[nextIndex]
        
        repository.updateRule(rule.copy(currentImageIndex = nextIndex))
        
        val image = repository.getImageById(imageId) ?: return
        val file = File(image.localUri)
        if (!file.exists()) return

        val active = com.example.data.ActiveWallpaper(1, image.localUri, rule.transitionAnimation)
        repository.setActiveWallpaper(active)

        val wallpaperManager = android.app.WallpaperManager.getInstance(applicationContext)
        val info = wallpaperManager.wallpaperInfo
        if (info != null && info.packageName == packageName) {
            Log.d("WallpaperService", "Live wallpaper engine handled transition.")
        } else {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                wallpaperManager.setBitmap(bitmap)
                Log.d("WallpaperService", "Applied static fallback from rule: ${rule.id}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 12) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 2000) {
                    lastShakeTime = now
                    checkRulesAndApply(TriggerType.SHAKE)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockReceiver)
        sensorManager.unregisterListener(this)
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
