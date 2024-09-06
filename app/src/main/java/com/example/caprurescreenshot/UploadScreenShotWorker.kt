package com.example.caprurescreenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UploadScreenShotWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val name = workerParameters.inputData.getString(KEY_FILE_NAME) ?: ""
        val viewWidth = workerParameters.inputData.getInt(KEY_VIEW_WIDTH, 0)
        val viewHeight = workerParameters.inputData.getInt(KEY_VIEW_HEIGHT, 0)
        // Optionally, you can pass a bitmap representation of the view if neededreturn
        return try {
            createBitmapFromView(viewWidth, viewHeight)
            val screenshot = createBitmapFromView(viewWidth, viewHeight) // Modify this function
            if (screenshot != null) {
                saveScreenshot(screenshot, name)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createBitmapFromView(width: Int, height: Int): Bitmap? {
        if (width <= 0 || height <= 0) return null
        var screenshot: Bitmap? = null
        try {
            // inflate screenshot object
            // with Bitmap.createBitmap it
            // requires three parameters
            // width and height of the view and
            // the background color
            screenshot =
                Bitmap.createBitmap(
                    context.resources.displayMetrics.widthPixels,
                    context.resources.displayMetrics.heightPixels,
                    Bitmap.Config.ARGB_8888
                )
            // Now draw this bitmap on a canvas
            val canvas = Canvas(screenshot)
//            (context as MainActivity).getRootView().draw(canvas)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Failed to capture screenshot because:" + e.message)
        }
        return screenshot
    }

    private fun saveScreenshot(bitmap: Bitmap, filename: String) {
        val file = File(context.filesDir, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }

    companion object {
        private const val BACKOFF_DELAY = 1L
        const val KEY_FILE_NAME = "file_name"
        const val KEY_VIEW_WIDTH = "view_width"
        const val KEY_VIEW_HEIGHT = "view_height"
        // Add a key for the optional bitmap representation if needed

        fun createWorkRequest(name: String, viewWidth: Int, viewHeight: Int) =
            OneTimeWorkRequestBuilder<UploadScreenShotWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_FILE_NAME to name,
                        KEY_VIEW_WIDTH to viewWidth,
                        KEY_VIEW_HEIGHT to viewHeight
                        // Add data for the optional bitmap representation if needed
                    )
                )
                .apply {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()
    }
}
