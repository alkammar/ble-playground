package ble.playground.central.ui

import android.app.*
import android.content.Intent
import android.os.Build
import ble.playground.central.R
import ble.playground.central.data.sensor.repository.SensorRepository
import ble.playground.central.entity.Sensor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_NOTIFICATION_ID = 101

@AndroidEntryPoint
class BleBackgroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    lateinit var sensorRepository: SensorRepository

    private var isServiceStarted = false

    override fun onBind(intent: Intent?): Nothing? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (!isServiceStarted) {
                isServiceStarted = true
                startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification()
                )
                observeSensor()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeSensor() {
        scope.launch {
            sensorRepository.data().collect { sensors ->
                sensors
                    .firstAvailable()
                    ?.let { sensor ->
                        startForeground(
                            SERVICE_NOTIFICATION_ID,
                            notification(sensor.data)
                        )
                    }
            }
        }
    }

    private fun Set<Sensor>.firstAvailable() = filterIsInstance<Sensor.Available>().firstOrNull()

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }

    private fun notification(value: String? = null) = notificationChannelBuilder()
        .setContentIntent(startIntent())
        .setSmallIcon(R.drawable.ic_ble)
        .setOngoing(true)
        .setContentTitle(getString(R.string.ble_background_service_notification_title))
        .setContentText(value?.let { getString(R.string.ble_background_service_notification_message_data, value) } ?: getString(R.string.ble_background_service_notification_message_no_data))
        .build()

    private fun notificationChannelBuilder() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
            packageName.toString() + "ble.service",
            "BLE channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(notificationChannel)
        Notification.Builder(applicationContext, notificationChannel.id)
    } else {
        Notification.Builder(applicationContext)
    }

    private fun startIntent() = PendingIntent.getActivity(
        applicationContext,
        0,
        Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
    )
}