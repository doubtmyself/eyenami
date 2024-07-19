package team.eyenami

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import timber.log.Timber

class ForegroundService : Service() {
    companion object {
        private const val ACTION_CAMERA = "ACTION_CAMERA"
        private const val ACTION_HOME = "ACTION_HOME"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAMERA -> {
                Timber.d("Start Foreground Service")
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
                // 서비스 시작 시 수행할 작업
                startRecording()
            }
            ACTION_HOME -> {

            }
            else -> {
                Timber.d("Default Service Command")
                // 기본 동작
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null



    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Companion.CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.remote_view)

        val cameraIntent = Intent(this, ForegroundService::class.java).apply {
            action = ACTION_CAMERA
        }
        val cameraPendingIntent = PendingIntent.getService(this, 0, cameraIntent, PendingIntent.FLAG_IMMUTABLE)

        val homeIntent = Intent(this, ForegroundService::class.java).apply {
            action = ACTION_HOME
        }
        val homePendingIntent = PendingIntent.getService(this, 0, homeIntent, PendingIntent.FLAG_IMMUTABLE)

        notificationLayout.setOnClickPendingIntent(R.id.btn_camera, cameraPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.btn_home, homePendingIntent)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(notificationLayout)
            .build()
    }

    private fun startRecording() {
        // TODO 카메라 작업
    }



}