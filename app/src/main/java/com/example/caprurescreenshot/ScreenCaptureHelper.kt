package com.example.caprurescreenshot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.core.content.ContextCompat.getSystemService
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureHelper(private val context: Context) {
    private val REQUEST_CODE_SCREEN_CAPTURE = 100
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    fun startScreenCapture() {
        val mediaProjectionManager = getSystemService(context, MediaProjectionManager::class.java)
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        (context as? Activity)?.startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            val mediaProjectionManager =
                getSystemService(context, MediaProjectionManager::class.java)
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data!!)
            setupVirtualDisplay()
        }
    }

    private fun setupVirtualDisplay() {
        val displayMetrics = context.resources.displayMetrics
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

    fun captureScreenshot(): Bitmap? {
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

    fun saveScreenshot(bitmap: Bitmap, filename: String) {
        val file = File(context.filesDir, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }

    fun stopScreenCapture() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}