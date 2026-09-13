package com.example.skbt_up_gibdd_eyewitness.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.skbt_up_gibdd_eyewitness.EyewitnessApplication
import com.example.skbt_up_gibdd_eyewitness.MainActivity
import com.example.skbt_up_gibdd_eyewitness.R
import kotlinx.coroutines.*

class LiveLocationService : Service(), LocationListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var locationManager: LocationManager
    private var latestLocation: Location? = null
    private var recordingJob: Job? = null
    private var expirationJob: Job? = null
    private var startJob: Job? = null
    private var liveMessageId: String? = null
    private val messageRepository get() = (application as EyewitnessApplication).container.messageRepository

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            requestStop()
            return START_NOT_STICKY
        }

        if (startJob?.isActive == true || liveMessageId != null) return START_NOT_STICKY

        val startedAt = System.currentTimeMillis()
        val endsAt = startedAt + LIVE_DURATION_MILLIS
        startForeground(NOTIFICATION_ID, createNotification())
        LiveLocationTracker.start(startedAt, endsAt)
        requestLocationUpdates()
        startJob = serviceScope.launch(Dispatchers.IO) {
            messageRepository.startLiveLocation()
                .onSuccess { liveMessageId = it.locationSessionId ?: it.id }
                .onFailure { requestStop(notifyBackend = false) }
        }
        recordingJob?.cancel()
        recordingJob = serviceScope.launch {
            while (isActive) {
                delay(UPDATE_INTERVAL_MILLIS)
                val location = latestLocation ?: continue
                val messageId = liveMessageId ?: continue
                launch(Dispatchers.IO) {
                    messageRepository.sendLiveLocationPoint(messageId, location.latitude, location.longitude)
                        .onSuccess { LiveLocationTracker.update(StaticLocation(location.latitude, location.longitude)) }
                }
            }
        }
        expirationJob?.cancel()
        expirationJob = serviceScope.launch {
            delay(LIVE_DURATION_MILLIS)
            requestStop()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!hasLocationPermission(this)) {
            requestStop(notifyBackend = false)
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(locationManager::isProviderEnabled)
        if (providers.isEmpty()) {
            requestStop(notifyBackend = false)
            return
        }
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL_MILLIS, 0f, this)
            locationManager.getLastKnownLocation(provider)?.let(::onLocationChanged)
        }
    }

    override fun onLocationChanged(location: Location) {
        latestLocation = location
        LiveLocationTracker.setLatest(StaticLocation(location.latitude, location.longitude))
    }

    override fun onProviderDisabled(provider: String) = Unit
    override fun onProviderEnabled(provider: String) = Unit

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        serviceScope.cancel()
        LiveLocationTracker.stop()
        super.onDestroy()
    }

    private fun requestStop(notifyBackend: Boolean = true) {
        recordingJob?.cancel()
        expirationJob?.cancel()
        serviceScope.launch(Dispatchers.IO) {
            if (notifyBackend) {
                liveMessageId?.let { messageRepository.stopLiveLocation(it) }
            }
            withContext(Dispatchers.Main.immediate) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Очевидец 44")
            .setContentText("Live-геолокация передаётся в течение 15 минут")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Остановить", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live-геолокация",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Передача местоположения в фоне" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "live_location"
        private const val NOTIFICATION_ID = 1501
        private const val ACTION_START = "live_location.start"
        private const val ACTION_STOP = "live_location.stop"
        private const val LIVE_DURATION_MILLIS = 15 * 60 * 1000L
        private const val UPDATE_INTERVAL_MILLIS = 1_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LiveLocationService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LiveLocationService::class.java).setAction(ACTION_STOP))
        }
    }
}

fun hasLocationPermission(context: Context): Boolean =
    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun liveLocationPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()
