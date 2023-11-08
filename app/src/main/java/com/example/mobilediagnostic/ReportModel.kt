package com.example.mobilediagnostic

data class ReportModel(
    var FrontCameraStatus: String? = null,
    var RearCameraStatus: String? = null,
    var BluetoothStatus: String? = null,
    var MicroPhonePrimaryStatus: String? = null,
    var RootedStatus: String? = null,
    var WifiStatus: String? = null,
    var MagneticStatus: String? = null,
    var GameRotationStatus: String? = null,
    var RotationVectorStatus: String? = null,
    var GyroscopeStatus: String? = null,
    var GeomagneticRotationStatus: String? = null,
    var LinearAccelerationStatus: String? = null,
    var SimCardStatus: String? = null,
    var InternalStorageStatus: String? = null,
    var BrightnessStatus: String? = null,
    var GPSStatusStatus: String? = null
)
