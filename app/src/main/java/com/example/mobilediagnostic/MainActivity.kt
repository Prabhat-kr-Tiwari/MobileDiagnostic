package com.example.mobilediagnostic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.LocationRequest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.mobilediagnostic.databinding.ActivityMainBinding
import com.google.android.gms.location.Priority


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var permissionsDenied = false
    // Define a request code for permission request.
    private val locationPermissionRequestCode = 1001
    // Permission request launcher.


    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,

    )

    private val requestCode = 123  // You can choose any integer as your request code

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        val gps=TurnOnGps(this)
        gps.startGps(resultLauncher)



        // Check if the "Manage write settings" permission is already granted
        if (!Settings.System.canWrite(this)) {
            // The permission is not granted, request it
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            checkAndRequestPermissions()


        }
        binding.button.setOnClickListener {
            if (areAllPermissionsGranted()) {
                val intent= Intent(this@MainActivity,DiagnosticActivity::class.java)

                startActivity(intent)

            }else if(!areAllPermissionsGranted()){
                checkAndRequestPermissions()
            }

        }
        if (!checkAndPromptToConnectToWiFi(this)){
            promptToConnectToWiFi(this)
        }







    }
    private fun areAllPermissionsGranted(): Boolean {
        for (permission in permissions) {
            val permissionStatus = ContextCompat.checkSelfPermission(this, permission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    private fun checkAndRequestPermissions() {
        val ungrantedPermissions = ArrayList<String>()

        for (permission in permissions) {
            val permissionStatus = ContextCompat.checkSelfPermission(this, permission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                ungrantedPermissions.add(permission)
            }
        }

        if (ungrantedPermissions.isNotEmpty()) {
            val permissionsArray = ungrantedPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, permissionsArray, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == this.requestCode) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    val intent= Intent(this@MainActivity,DiagnosticActivity::class.java)

                    startActivity(intent)
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    Log.d("PRABHAT", "onRequestPermissionsResult: Permission denied")
                    permissionsDenied = true
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (permissionsDenied) {
            // Check for permission status again and handle it
            checkAndRequestPermissions()
            permissionsDenied = false
        }
    }
    fun checkAndPromptToConnectToWiFi(context: Context) :Boolean{
        var ans:Boolean=false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check network connection for devices running Android 10 (API level 29) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // Device is connected to Wi-Fi
                    ans=  true
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // Device is using mobile data
//                    promptToConnectToWiFi(context)
                    ans= false
                }
            }
        } else {
            // Check network connection for devices running below Android 10.
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    // Device is connected to Wi-Fi
                    ans= true
                } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                    // Device is using mobile data
                    promptToConnectToWiFi(context)
                    ans= false
                }
            }
        }
        return ans

    }

    fun promptToConnectToWiFi(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Connect to Wi-Fi")
        builder.setMessage("To save data, we recommend connecting to a Wi-Fi network.")
        builder.setPositiveButton("Connect to Wi-Fi") { dialog, which ->
            // Open the Wi-Fi settings page
            val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(wifiIntent)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            // Handle the user's choice to cancel
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->

        if (activityResult.resultCode == RESULT_OK) {
            Toast.makeText(this, "Gps is on", Toast.LENGTH_SHORT).show()
        } else if (activityResult.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Request is Canceled", Toast.LENGTH_SHORT).show()
        }

    }






}