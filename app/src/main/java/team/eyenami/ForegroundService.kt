package team.eyenami

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import team.eyenami.obj.SettingManager
import team.eyenami.ui.MainActivity
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ForegroundService : LifecycleService() {
    companion object {
        private const val ACTION_START_CAMERA = "ACTION_START_CAMERA"
        private const val ACTION_STOP_CAMERA = "ACTION_STOP_CAMERA"
        private const val ACTION_HOME = "ACTION_HOME"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        var isCapturing = false
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraController: LifecycleCameraController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeCamera()
    }

    private fun initializeCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_CAMERA -> {
                startRecording()
            }
            ACTION_HOME -> {

            }
            else -> {
                Timber.d("Default Service Command")
                // 기본 동작
                val notification = createNotification(true)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }
//    override fun onBind(intent: Intent?): IBinder? = null



    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(isCapturing: Boolean): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.remote_view)

        val cameraIntent = Intent(this, ForegroundService::class.java).apply {
            action = if (isCapturing) ACTION_STOP_CAMERA else ACTION_START_CAMERA
        }
        val cameraPendingIntent = PendingIntent.getService(this, 0, cameraIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val homeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_HOME
        }
        val homePendingIntent = PendingIntent.getActivity(this, 0, homeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        notificationLayout.setOnClickPendingIntent(R.id.btn_camera, cameraPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.btn_home, homePendingIntent)

        // drawableTint 변경
        if (isCapturing) {
            notificationLayout.setInt(R.id.btn_camera, "setImageTintList", ContextCompat.getColor(this, R.color.service_ui_off))
        } else {
            notificationLayout.setInt(R.id.btn_camera, "setImageTintList", ContextCompat.getColor(this, R.color.service_ui))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setSilent(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .build()
    }

    private fun startRecording() {
        if (!isCapturing) {
            isCapturing = true
            lifecycleScope.launch {
                while (isCapturing) {
                    StorageManager.manageStorage(MainApplication.appContext)
                    capturePhoto()
                    delay(SettingManager.getDetectionCountMS()) // 5초 대기
                }
            }
        }
    }

    private suspend fun capturePhoto() {
        return suspendCoroutine { continuation ->
            val photoFile = StorageManager.createFile(MainApplication.appContext,"jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            cameraController.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Timber.d("사진 저장 성공: ${outputFileResults.savedUri}")
                        continuation.resume(Unit)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Timber.e(exception, "사진 촬영 실패")
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}