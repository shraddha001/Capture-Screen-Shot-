package com.example.caprurescreenshot


/*
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var code = 1
        findViewById<Button>(R.id.btnCapture).setOnClickListener { view ->
            code = code + 1
          */
/*  val workRequest =
                UploadScreenShotWorker.createWorkRequest(
                    "screenshot _ $code.png",
                    this.getResources().displayMetrics.widthPixels,
                    this.getResources().displayMetrics.heightPixels,
                )
            WorkManager.getInstance(this).enqueue(workRequest)*//*

            val screenShot = getScreenShotFromView()
            if (screenShot!=null){
                saveScreenshot(screenShot,"screenshot _ $code.png")
            }
        }
    }

    fun getRootView(): View {
        return binding.root
    }

    private fun getScreenShotFromView(): Bitmap? {
        // create a bitmap object
        var screenshot: Bitmap? = null
        try {
            // inflate screenshot object
            // with Bitmap.createBitmap it
            // requires three parameters
            // width and height of the view and
            // the background color
            screenshot =
                Bitmap.createBitmap(
                    this.getResources().displayMetrics.widthPixels,
                    this.getResources().displayMetrics.heightPixels, Bitmap.Config.ARGB_8888
                )
            // Now draw this bitmap on a canvas
            val canvas = Canvas(screenshot)
            binding.btnCapture.rootView.draw(canvas)
        } catch (e: Exception) {
            Log.e(javaClass.name, "Failed to capture screenshot because:" + e.message)
        }
        // return the bitmap
        return screenshot
    }

    fun saveScreenshot(bitmap: Bitmap, filename: String) {
        val file = File(this.filesDir, filename)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }

}*/

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

/*
class MainActivity : AppCompatActivity() {

    private val SCREENSHOT_REQUEST_CODE = 1001
    var mediaProjection: MediaProjection? = null
    lateinit var mediaProjectionManager: MediaProjectionManager
    var id = UUID.randomUUID()
    var serviceIntent: Intent = Intent()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            id = UUID.randomUUID()
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                SCREENSHOT_REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Log the resultCode and data
            println("Result Code: $resultCode, Intent Data: $data")

            serviceIntent = Intent(this, MyScreenCaptureService::class.java)
            serviceIntent.putExtra(MyScreenCaptureService.RESULT_CODE, resultCode)
            serviceIntent.putExtra(MyScreenCaptureService.DATA, data)
            startForegroundService(serviceIntent)
            // Bind to the service to trigger capture
            val serviceConnection = object : ServiceConnection {
                // ... (Your existing ServiceConnection code)`
                override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                    println(">>>>>>>>>>>>>>>>>>>>>>>>>SERVICE_CONNECTED>>")
                    val binder = p1 as MyScreenCaptureService.LocalBinder
                    val screenCaptureService = binder.getService()
                    Handler(Looper.getMainLooper()).postDelayed({
                        mediaProjection =
                            data?.let { mediaProjectionManager.getMediaProjection(resultCode, it) }
                        screenCaptureService.captureAndSaveScreenshot("screenshot_$id.png") // Trigger capture
                    }, 1000)

//                    unbindService(this) // Unbind after triggering
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    println(">>>>>>>>>>>>>>>>>>>>>>>>>>DISCONNECTED")
                }
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            println("Request failed or was cancelled.")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
    }

}*/
class MainActivity : AppCompatActivity() {

    private val SCREENSHOT_REQUEST_CODE = 1001
    var mediaProjection: MediaProjection? = null
    lateinit var mediaProjectionManager: MediaProjectionManager
    private var screenCaptureService: MyScreenCaptureService? = null
    private var isBound = false
    private val captureIntervalMillis: Long = 100000 // 8 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Ask for permission to capture the screen
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            SCREENSHOT_REQUEST_CODE
        )

        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            println("Result Code: $resultCode, Intent Data: $data")

            val serviceIntent = Intent(this, MyScreenCaptureService::class.java)
            serviceIntent.putExtra(MyScreenCaptureService.RESULT_CODE, resultCode)
            serviceIntent.putExtra(MyScreenCaptureService.DATA, data)

            // Start the screen capture service
            startForegroundService(serviceIntent)

            // Bind to the service and start periodic capture
            bindAndStartPeriodicCapture(serviceIntent)
        } else {
            println("Request failed or was cancelled.")
        }
    }

    private fun bindAndStartPeriodicCapture(serviceIntent: Intent) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                println("Service Connected")
                val binder = p1 as MyScreenCaptureService.LocalBinder
                screenCaptureService = binder.getService()
                isBound = true

                // Start periodic screenshot capture
                startPeriodicCapture()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                println("Service Disconnected")
                isBound = false
            }
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startPeriodicCapture() {
        val handler = Handler(Looper.getMainLooper())

        val captureRunnable = object : Runnable {
            override fun run() {
                // Trigger screen capture every 8 seconds
                if (isBound && screenCaptureService != null) {
                    screenCaptureService?.captureAndSaveScreenshot("screenshot_${System.currentTimeMillis()}.png")
                    println("Screenshot captured")
                }

                // Schedule the next capture
                handler.postDelayed(this, captureIntervalMillis)
            }
        }

        // Start the first capture
        handler.post(captureRunnable)
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        screenCaptureService?.stopSelf()
        if (isBound) {
//            unbindService(serviceConnection)
            isBound = false
        }
    }
}

