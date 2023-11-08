package com.example.mobilediagnostic


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.mobilediagnostic.databinding.ActivityDiagnosticBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID


class DiagnosticActivity : AppCompatActivity() {


    //    var item:ReportModel
    private lateinit var database: DatabaseReference
    private lateinit var PhoneImei: String
    private lateinit var item: ReportModel

    private lateinit var FrontCameraStatus: String
    private lateinit var RearCameraStatus: String
    private lateinit var BluetoothStatus: String
    private lateinit var MicroPhonePrimaryStatus: String
    private lateinit var RootedStatus: String
    private lateinit var WifiStatus: String
    private lateinit var MagneticStatus: String
    private lateinit var GameRotationStatus: String
    private lateinit var RotationVectorStatus: String
    private lateinit var GyroscopeStatus: String
    private lateinit var GeomagneticRotationStatus: String
    private lateinit var LinearAccelerationStatus: String
    private lateinit var SimCardStatus: String
    private lateinit var BrightnessStatus: String
    private lateinit var GPSStatusStatus: String
    private lateinit var InternalStorageStatus: String

    //stuff for viewpager
    private lateinit var viewPager2: ViewPager2
    private var sliderHandler: Handler = Handler()

    private lateinit var count: TextView
    var total: Int = 0
    private lateinit var linearProgressIndicator: LinearProgressIndicator

    //front camera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var preview: Preview
    private var camera: Camera? = null


    //BLUETOTH API
    private lateinit var bluetoothAdapter: BluetoothAdapter;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mBroadCastReceiver1 = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "onReceive: Bluetooth is off")
                        Toast.makeText(
                            this@DiagnosticActivity,
                            "Bluetooth is off",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d(TAG, "onReceive: Bluetooth Turning off")
                        Toast.makeText(
                            this@DiagnosticActivity,
                            "Bluetooth Turning off",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "onReceive: Bluetooth is on")
                        Toast.makeText(
                            this@DiagnosticActivity,
                            "Bluetooth is on",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Log.d(TAG, "onReceive: Bluetooth Turning on")
                        Toast.makeText(
                            this@DiagnosticActivity,
                            "Bluetooth Turning on",
                            Toast.LENGTH_SHORT
                        ).show()

                    }


                }

            }
        }
    }
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PHONE_STATE_PERMISSION = 123342

    private val enableBluetoothLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth has been enabled by the user
                Log.d(TAG, "Bluetooth enabled by the user.")
            } else {
                // The user declined to enable Bluetooth, or the request was canceled
                Log.d(TAG, "Bluetooth enabling request canceled by the user.")
            }
        }

    //microphones


    private val audioRecordPermissionCode = 101
    private val bufferSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )


    private val recordPermissionCode = 101
    private val outputFileName =
        Environment.getExternalStorageDirectory().absolutePath + "/recorded_audio.wav"





    private lateinit var binding: ActivityDiagnosticBinding


    private lateinit var mlist: ArrayList<model>
    private val handler = android.os.Handler()


    private lateinit var myAdapter: DiagnosticAdapter

    private lateinit var cancelButton: TextView

    //unregisterReceiver of bluetooth
    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mBroadCastReceiver1)
        //to handle memory leaks
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_diagnostic)

        //bluetooth
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter


        //going back to previous button
        cancelButton = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        PhoneImei = ""

        database = FirebaseDatabase.getInstance().getReference("DiagnosisReport")
        // Check if you have the READ_PHONE_STATE permission.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission if it is not granted.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_PHONE_STATE_PERMISSION
            )
        } else {
            // Permission is granted, so you can get the IMEI.
            getIMEI()
        }


        FrontCameraStatus = ""
        RearCameraStatus = ""
        BluetoothStatus = ""
        MicroPhonePrimaryStatus = ""
        RootedStatus = ""
        WifiStatus = ""
        MagneticStatus = ""
        GameRotationStatus = ""
        RotationVectorStatus = ""
        GyroscopeStatus = ""
        GeomagneticRotationStatus = ""
        LinearAccelerationStatus = ""
        SimCardStatus = ""
        InternalStorageStatus = ""
        BrightnessStatus = ""
        GPSStatusStatus = ""


        viewPager2 = findViewById(R.id.viewPager2)
        count = findViewById(R.id.count);
        total = 16
        count.text = total.toString()
        linearProgressIndicator = findViewById(R.id.progress_horizontal)



        try {


            mlist = arrayListOf<model>()
            mlist.add(model("Front Camera", R.drawable.baseline_camera_front_24, true, true))
            mlist.add(model("Rear Camera", R.drawable.camera_rear, true, false))
            mlist.add(model("Bluetooth", R.drawable.bluetooth, true, false))
            mlist.add(model("MicroPhone(Primary)", R.drawable.baseline_mic_24, true, false))
            mlist.add(model("Rooted status)", R.drawable.baseline_send_to_mobile_24, true, false))
            mlist.add(model("Wifi status)", R.drawable.baseline_network_wifi_24, true, false))
            mlist.add(model("Magnetic Sensor)", R.drawable.img, true, false))
            mlist.add(model("Game Rotation sensor", R.drawable.gamerotationsensor, true, false))
            mlist.add(model("Rotation Vector sensor", R.drawable.rotationvectorsensor, true, false))
            mlist.add(model("Gyroscope", R.drawable.gyroscope, true, false))
            mlist.add(model("Geomagnetic Rotation", R.drawable.geomagneticrotation, true, false))
            mlist.add(model("Linear Acceleration", R.drawable.linearacceleration, true, false))
            mlist.add(model("Sim Card", R.drawable.baseline_sim_card_24, true, false))
            mlist.add(model("Internal Storage", R.drawable.baseline_storage_24, true, false))
            mlist.add(model("Brightness", R.drawable.baseline_brightness_7_24, true, false))
            mlist.add(model("GPS Status", R.drawable.baseline_gps_fixed_24, true, false))








            myAdapter = DiagnosticAdapter(this, mlist, viewPager2)
            binding.viewPager2.adapter =
                myAdapter


            //new start

            viewPager2.offscreenPageLimit = 3
            viewPager2.clipChildren = false
            viewPager2.clipToPadding = false
            viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            val transformer: CompositePageTransformer = CompositePageTransformer()
            transformer.addTransformer(MarginPageTransformer(5))
            val customtransformer = ViewPager2.PageTransformer({ page, position ->
                val r = 1 - Math.abs(position)
                page.scaleY = 0.85f + r * 0.14f


            })
            transformer.addTransformer(customtransformer)
            viewPager2.setPageTransformer(transformer)

            viewPager2.isUserInputEnabled = false
            total = myAdapter.itemCount
            viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

                @Override
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Log.d(TAG, "Position: $position")
                    processAndScrollToNext(position)
                    createReport()
                    total--
                    count.text = total.toString()
                    linearProgressIndicator.progress = position

                    Log.d(TAG, "onPageSelected:MYADAPTER ITEM COUNT ${myAdapter.itemCount - 1}")
                    if (position == myAdapter.itemCount - 1) {
                        item = ReportModel(

                            FrontCameraStatus,
                            RearCameraStatus,
                            BluetoothStatus,
                            MicroPhonePrimaryStatus,
                            RootedStatus,
                            WifiStatus,
                            MagneticStatus,
                            GameRotationStatus,
                            RotationVectorStatus,
                            GyroscopeStatus,
                            GeomagneticRotationStatus,
                            LinearAccelerationStatus,
                            SimCardStatus,
                            InternalStorageStatus,
                            BrightnessStatus,
                            GPSStatusStatus
                        )




                        getResultOutput(this@DiagnosticActivity)


                    }
                    sliderHandler.removeCallbacks(sliderRunnable)
                    sliderHandler.postDelayed(sliderRunnable, 2000)
                }
            })


        } catch (e: Exception) {
            Log.d("Prabhat", "onCreate: catch block " + e.printStackTrace())
            e.printStackTrace()
        }


    }


    private fun processAndScrollToNext(currentItemPosition: Int) {
        if (currentItemPosition == 0) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            val hasFrontCamera = checkIfDeviceHasFrontCamera()
            if (hasFrontCamera) {
                FrontCameraStatus = "Pass"
//
                bindCameraUseCases()

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else if (!hasFrontCamera) {
                FrontCameraStatus = "Fail"



                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)
//
            }
        }

        //rear camera
        else if (currentItemPosition == 1) {

//

            // Initialize the cameraProviderFuture
            cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            // Check if the device has a front camera.
            val hasFrontCamera = checkIfDeviceHasRearCamera()

            // If the device has a front camera, display a preview of the camera feed.
            if (hasFrontCamera) {
                RearCameraStatus = "Pass"
                bindCameraUseCasesRear()

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else if (!hasFrontCamera) {
                RearCameraStatus = "Fail"


                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }

        }
        //bluetooth
        else if (currentItemPosition == 2) {
            binding.previewView.visibility= View.INVISIBLE


            Log.d(TAG, "processAndScrollToNext:  BLUETOOTH ${enableDisableBluetooth()}")

            if (enableDisableBluetooth()) {

                BluetoothStatus = "Pass"
                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)


            } else {
                BluetoothStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)


            }


        }
        //primary microphones
        else if (currentItemPosition == 3) {


            Log.d(TAG, "processAndScrollToNext: ${currentItemPosition}")

            if (checkAudioPermissionsAndGenerateBeep()) {
                MicroPhonePrimaryStatus = "Pass"

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)


            } else {
                MicroPhonePrimaryStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)


            }


        }
        //rooted status
        else if (currentItemPosition == 4) {

            Log.d(TAG, "processAndScrollToNext: ${currentItemPosition}")

            val rootUtil = RootUtil()
            if (!rootUtil.isDeviceRooted()) {
                RootedStatus = "Pass"

                Log.d(TAG, "processAndScrollToNext: Device is not rooted")
                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                RootedStatus = "Fail"

                Log.d(TAG, "processAndScrollToNext: Device is not rooted")
                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }

        }
        //wifi
        else if (currentItemPosition == 5) {


            if (isWiFiConnectedAndInternetWorking(this)) {
                WifiStatus = "Pass"

                // Wi-Fi is connected and internet is working

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                WifiStatus = "Fail"

                // Wi-Fi is not connected or internet is not working

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }


        } else if (currentItemPosition == 6) {

            val magneticSensorChecker = MagneticSensorChecker(this)
            if (magneticSensorChecker.isMagneticSensorWorking()) {
                MagneticStatus = "Pass"


                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                MagneticStatus = "Fail"


                myAdapter.setImageViewVisibility(currentItemPosition, true, false, true)

            }
        } else if (currentItemPosition == 7) {

            val gameRotationSensorHelper = GameRotationSensorHelper(this)
            if (gameRotationSensorHelper.isGameRotationSensorWorking()) {
                GameRotationStatus = "Pass"


                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                GameRotationStatus = "Fail"


                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)


            }

        } else if (currentItemPosition == 8) {

            val rotationVectorSensorHelper = RotationVectorSensorHelper(this)
            if (rotationVectorSensorHelper.isRotationVectorSensorWorking()) {
                RotationVectorStatus = "Pass"

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                RotationVectorStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }
        } else if (currentItemPosition == 9) {

            val gyroscopeSensorHelper = GyroscopeSensorHelper(this)
            if (gyroscopeSensorHelper.isGyroscopeSensorWorking()) {
                GyroscopeStatus = "Pass"

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)
            } else {
                GyroscopeStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }
        } else if (currentItemPosition == 10) {
            val geomagneticSensorHelper = GeomagneticSensorHelper(this)
            if (geomagneticSensorHelper.isGeomagneticSensorAvailable()) {
                GeomagneticRotationStatus = "Pass"

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)
            } else {
                GeomagneticRotationStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }
        } else if (currentItemPosition == 11) {
            val linearAccelerationSensorHelper = LinearAccelerationSensorHelper(this)
            if (linearAccelerationSensorHelper.isLinearAccelerationSensorAvailable()) {
                LinearAccelerationStatus = "Pass"


                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                LinearAccelerationStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)
            }
        } else if (currentItemPosition == 12) {

            val isSimWorking = isSimCardWorking(this) // "this" refers to the current context

            if (isSimWorking) {
                SimCardStatus = "Pass"
                // SIM card is working

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)
            } else {
                SimCardStatus = "Fail"


                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }

        } else if (currentItemPosition == 13) {

            val isInternalStorageOK = isInternalStorageWorking()

            if (isInternalStorageOK) {
                InternalStorageStatus = "Pass"

                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                InternalStorageStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)
            }


        } else if (currentItemPosition == 14) {

            val contentResolver = applicationContext.contentResolver
            val testBrightnessValue = 500 // Adjust to your desired brightness level

            if (isBrightnessControlWorking(contentResolver, testBrightnessValue)) {
                BrightnessStatus = "Pass"


                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)
            } else {
                BrightnessStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)
            }

        } else if (currentItemPosition == 15) {

            if (isGPSLocationEnabled(this)) {
                GPSStatusStatus = "Pass"
                myAdapter.setImageViewVisibility(currentItemPosition, false, true, true)

            } else {
                GPSStatusStatus = "Fail"

                myAdapter.setImageViewVisibility(currentItemPosition, true, false, false)

            }

        }
        myAdapter.notifyDataSetChanged()


    }


    //FRONT
    private fun checkIfDeviceHasFrontCamera(): Boolean {
        val cameraProvider = cameraProviderFuture.get()
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    private fun bindCameraUseCases() {


        // CameraX setup for displaying the camera preview
        val previewView = findViewById<PreviewView>(R.id.previewView)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        var cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        cameraProvider = cameraProviderFuture.get()


        // Bind the camera preview to the PreviewView
        preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        cameraProvider.bindToLifecycle(this, cameraSelector, preview)
    }

    //rear camera
    private fun checkIfDeviceHasRearCamera(): Boolean {
        val cameraProvider = cameraProviderFuture.get()
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    private fun bindCameraUseCasesRear() {

        // CameraX setup for displaying the camera preview
        val previewView = findViewById<PreviewView>(R.id.previewView)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        var cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        cameraProvider = cameraProviderFuture.get()

        // Bind the camera preview to the PreviewView
        preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        cameraProvider.bindToLifecycle(this, cameraSelector, preview)
    }


    private fun enableDisableBluetooth(): Boolean {
        var ans = false
        if (bluetoothAdapter.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBluetooth: i am here")

            Log.d(Companion.TAG, "enableDisableBluetooth: Does not have bluetooth support")
            ans = false

        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBTintent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "enableDisableBluetooth: Asking permission true")
                ans = true


            }

            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadCastReceiver1, BTIntent)
            if (bluetoothAdapter.isEnabled) {
                Log.d(TAG, "enableDisableBluetooth: after permission bluettoth is on   true")
                ans = true

            }


        }
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(mBroadCastReceiver1, BTIntent)
            if (!bluetoothAdapter.isEnabled) {
                ans = true
                Log.d(TAG, "enableDisableBluetooth: blueetooth is off  true")

            }

        } else {
            // Bluetooth is currently off, so request the user to enable it
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBTIntent)

            if (bluetoothAdapter.isEnabled) {
                ans = true
                Log.d(TAG, "enableDisableBluetooth: blueettoh is off asking to on   true")

            }
        }
        return ans


    }


    private fun checkAudioPermissionsAndGenerateBeep(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    audioRecordPermissionCode
                )
            } else {
                return generateBeepAndPlay()
            }
        } else {
            return generateBeepAndPlay()
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == audioRecordPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateBeepAndPlay()
            }
        } else if (requestCode == recordPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                copyRawAudioToFileAndStartRecording()
            }
        } else if (requestCode == REQUEST_PHONE_STATE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can get the IMEI.
                getIMEI()
            } else {
                // Permission denied. Handle this case as needed.
            }
        }
    }

    private fun generateBeepAndPlay(): Boolean {
        var ans = false
        Log.d(TAG, "generateBeepAndPlay: Generating and playing")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val primaryMicrophone =
            audioDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }

        if (primaryMicrophone == null) {
            Log.d(TAG, "generateBeepAndPlay: microphone error")
            ans = false
            // Primary microphone not found
        }


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "generateBeepAndPlay: Permission denied")

        } else {


            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            val buffer = ShortArray(bufferSize)

            audioRecord.startRecording()
            audioTrack.play()

            handler.postDelayed({
                audioRecord.read(buffer, 0, bufferSize)
                audioTrack.write(buffer, 0, bufferSize)
            }, 9000)
            ans = true

        }
        return ans

    }


    private fun copyRawAudioToFileAndStartRecording() {
        try {
            val rawResourceId = R.raw.beep2
            val inputStream = resources.openRawResource(rawResourceId)
            val file = File(outputFileName)
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.close()

            startRecording()
        } catch (e: IOException) {
            Log.e("Recording", "Failed to copy raw audio to file")
        }
    }

    private var mediaRecorder: MediaRecorder? = null

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFileName)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recording", "prepare() failed")
            }

            start()
        }
    }


    //wifi
    fun isWiFiConnectedAndInternetWorking(context: Context): Boolean {
//        val context=Context()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

        if (networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
            return true
        }

        return false
    }


    fun isSimCardWorking(context: Context): Boolean {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.simState == TelephonyManager.SIM_STATE_READY
    }


    fun isInternalStorageWorking(): Boolean {
        // Check if external storage (SD card) is available
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return false // External storage is not available
        }

        // Get the path to the primary external storage directory
        val externalStorageDirectory = Environment.getExternalStorageDirectory()

        // Check the capacity of the external storage
        val stat = StatFs(externalStorageDirectory.absolutePath)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        // Calculate the total storage capacity in bytes
        val totalStorageCapacity = totalBlocks * blockSize

        // Calculate the available storage capacity in bytes
        val availableStorageCapacity = availableBlocks * blockSize

        // Check if there's enough available storage
        // You can set a threshold here as needed
        val storageThreshold = 1024 * 1024 * 100 // For example, 100 MB
        return availableStorageCapacity > storageThreshold
    }

    //brightness testing

    fun setScreenBrightness(contentResolver: ContentResolver, brightnessValue: Int) {
        try {
            Log.d(TAG, "setScreenBrightness: called")
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting screen brightness: ${e.message}")
        }
    }

    fun isBrightnessControlWorking(
        contentResolver: ContentResolver,
        testBrightnessValue: Int
    ): Boolean {
        Log.d(TAG, "isBrightnessControlWorking: called")
        val currentBrightness =
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)

        Log.d(TAG, "isBrightnessControlWorking: CURRENT BRIGHTNESS $currentBrightness")

        try {
            // Set the brightness to a test value
            setScreenBrightness(contentResolver, testBrightnessValue)

            // Check if the brightness setting has changed
            val newBrightness =
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            Log.d(TAG, "isBrightnessControlWorking: NEW BRIGHTNESS $newBrightness")

            // Restore the original brightness
            setScreenBrightness(contentResolver, currentBrightness)
            Log.d(TAG, "isBrightnessControlWorking: Restored brightness to $currentBrightness")

            return newBrightness == testBrightnessValue
        } catch (e: Exception) {
            Log.e(TAG, "Error checking brightness control: ${e.message}")
            return false
        }
    }

    // Function to check if GPS is enabled (status)
    fun isGPSLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun openGPSSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
    }


    fun createReport() {
        val pdfpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .toString()
        val randomString = UUID.randomUUID().toString()
        val file = File(pdfpath, "DiagnosticReport.pdf")
        val outputStream = FileOutputStream(file)
        val writer = PdfWriter(file)
        val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdfDocument)

        val columnWidths = arrayOf(
            UnitValue.createPointValue(62f),
            UnitValue.createPointValue(162f),
            UnitValue.createPointValue(112f),
            UnitValue.createPointValue(112f),
            UnitValue.createPointValue(112f)
        )
        val table1 = Table(columnWidths)

        //Table1--01
        //five cell for one row
        table1.addCell(Cell().add(Paragraph("S no")))
        table1.addCell(Cell().add(Paragraph("Diagnosis For")))
        table1.addCell(Cell().add(Paragraph("Status")))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))

        //Table1--02
        table1.addCell(Cell().add(Paragraph("1.")))
        table1.addCell(Cell().add(Paragraph("Front Camera")))
        table1.addCell(Cell().add(Paragraph(FrontCameraStatus)))
        table1.addCell(Cell().add(Paragraph()))
        table1.addCell(Cell().add(Paragraph()))

        //Table1--03
        table1.addCell(Cell().add(Paragraph("2.")))
        table1.addCell(Cell().add(Paragraph("Rear Camera")))
        table1.addCell(Cell().add(Paragraph(RearCameraStatus)))
        table1.addCell(Cell().add(Paragraph()))
        table1.addCell(Cell().add(Paragraph()))

        //Table1--04
        table1.addCell(Cell().add(Paragraph("3.")))
        table1.addCell(Cell().add(Paragraph("Bluetooth")))
        table1.addCell(Cell().add(Paragraph(BluetoothStatus)))
        table1.addCell(Cell().add(Paragraph()))
        table1.addCell(Cell().add(Paragraph()))

        //Table1--05
        table1.addCell(Cell().add(Paragraph("4.")))
        table1.addCell(Cell().add(Paragraph("MicroPhone(Primary)")))
        table1.addCell(Cell().add(Paragraph(MicroPhonePrimaryStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))

        //Table1--06
        table1.addCell(Cell().add(Paragraph("5.")))
        table1.addCell(Cell().add(Paragraph("Rooted status")))
        table1.addCell(Cell().add(Paragraph(RootedStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--07
        table1.addCell(Cell().add(Paragraph("6.")))
        table1.addCell(Cell().add(Paragraph("Wifi status")))
        table1.addCell(Cell().add(Paragraph(WifiStatus)))
        table1.addCell(Cell().add(Paragraph()))
        table1.addCell(Cell().add(Paragraph()))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("7.")))
        table1.addCell(Cell().add(Paragraph("Magnetic Sensor")))
        table1.addCell(Cell().add(Paragraph(MagneticStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("8.")))
        table1.addCell(Cell().add(Paragraph("Game Rotation sensor")))
        table1.addCell(Cell().add(Paragraph(GameRotationStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("9.")))
        table1.addCell(Cell().add(Paragraph("Rotation Vector sensor")))
        table1.addCell(Cell().add(Paragraph(RotationVectorStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("10.")))
        table1.addCell(Cell().add(Paragraph("Gyroscope")))
        table1.addCell(Cell().add(Paragraph(GyroscopeStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("11.")))
        table1.addCell(Cell().add(Paragraph("Geomagnetic Rotation")))
        table1.addCell(Cell().add(Paragraph(GeomagneticRotationStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("12.")))
        table1.addCell(Cell().add(Paragraph("Linear Acceleration")))
        table1.addCell(Cell().add(Paragraph(LinearAccelerationStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("13.")))
        table1.addCell(Cell().add(Paragraph("Sim Card")))
        table1.addCell(Cell().add(Paragraph(SimCardStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("14.")))
        table1.addCell(Cell().add(Paragraph("Internal Storage")))
        table1.addCell(Cell().add(Paragraph(InternalStorageStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))

        //Table1--08
        table1.addCell(Cell().add(Paragraph("15.")))
        table1.addCell(Cell().add(Paragraph("Brightness")))
        table1.addCell(Cell().add(Paragraph(BrightnessStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))


        //Table1--08
        table1.addCell(Cell().add(Paragraph("16.")))
        table1.addCell(Cell().add(Paragraph("GPS Status")))
        table1.addCell(Cell().add(Paragraph(GPSStatusStatus)))
        table1.addCell(Cell().add(Paragraph("")))
        table1.addCell(Cell().add(Paragraph("")))



        document.add(table1)
        document.close()


    }


    private fun getIMEI() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = telephonyManager.imei

        // Do something with the IMEI (e.g., display it in a TextView).
        if (imei != null) {
            PhoneImei = imei
            Log.d("DARLING", "getIMEI: $PhoneImei")
            // IMEI is available.
            // You can display it in a TextView or use it as needed.

        } else {
            Log.d("DARLING", "getIMEI: NO imei")
            // IMEI is not available (e.g., on devices that don't have telephony capabilities).
            Toast.makeText(this, "No IMEI AVAILABLE", Toast.LENGTH_SHORT).show()
        }
    }

    fun getResultOutput(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Diagnostic Result")
        builder.setMessage("Result Output After Running Diagnostics")
        builder.setPositiveButton(" Send the data to Server (Firebase)") { dialog, which ->
            // Open the Wi-Fi settings page
            uploadDataToFirebase()


        }
        builder.setNegativeButton(" Generate a PDF report") { dialog, which ->
            // Handle the user's choice to cancel
            createReport()

        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun uploadDataToFirebase() {
        database.child(PhoneImei).setValue(item).addOnSuccessListener {
            Toast.makeText(this@DiagnosticActivity, "Data Saved Successfully", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "onPageSelected Firebase: Saved data")
        }.addOnFailureListener { e ->
            Log.d(TAG, "onPageSelected Firebase: Failed to Upload data" + e)
        }
    }


    companion object {
        private const val TAG = "PRABHAT"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }

    private val sliderRunnable: Runnable = Runnable {
        // Your code to be executed when the Runnable runs

        viewPager2.setCurrentItem(viewPager2.currentItem + 1)
    }

    @Override
    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)


    }

    @Override
    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 2000)
    }


}