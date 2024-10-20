package com.subhajitrajak.assignmentpointo

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("Check", "Service started with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        if(checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            start()
                        } else {
                            stopSelf()
                        }
                    } else {
                        start()
                    }
                } else {
                    Log.e("Check", "Missing permissions. Stopping service.")
                    stopSelf()
                }
            }
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Your current Location")
            .setContentText("Waiting for location...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        locationClient.getLocationUpdates(3000L)
            .catch { e ->
                e.printStackTrace()
            }
            .onEach { location ->
                val latitude = location.latitude.toString()
                val longitude = location.longitude.toString()
                val updatedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Your current Location")
                    .setContentText("Latitude: $latitude\nLongitude: $longitude")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build()
                notificationManager.notify(1, updatedNotification)
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "location"
    }
}