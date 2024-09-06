package com.example.caprurescreenshot

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class MyScreenCaptureService : Service() {

    private val CHANNEL_ID = "ScreenCaptureChannel"
    private val NOTIFICATION_ID = 1

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.getParcelableExtra<Intent>(DATA)
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
        mediaProjection =
            (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(
                intent?.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED)
                    ?: Activity.RESULT_CANCELED,
                data ?: return START_NOT_STICKY
            )
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>> :: ${mediaProjection}")
//        createNotificationChannel()
        setupVirtualDisplay()
        return START_STICKY_COMPATIBILITY
    }


    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java) // Replace with your main activity
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Active")
            .setContentText("Capturing screenshots...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun setupVirtualDisplay() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val densityDpi = displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    fun captureAndSaveScreenshot(filename: String) {
        val bitmap = captureScreenshot()
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>..1   $bitmap")
        if (bitmap != null) {
            saveScreenshot(bitmap, filename)
        }
    }

    private fun captureScreenshot(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        return bitmap
    }

    private fun saveScreenshot(bitmap: Bitmap, filename: String) {
        val file = File(filesDir, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }

    private fun stopScreenCapture() {
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    companion object {
        const val RESULT_CODE = "result_code"
        const val DATA = "data"
    }

    inner class LocalBinder : Binder() {
        fun getService(): MyScreenCaptureService = this@MyScreenCaptureService
    }
}