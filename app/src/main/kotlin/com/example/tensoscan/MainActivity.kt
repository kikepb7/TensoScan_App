package com.example.tensoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.example.tensoscan.ui.common.navigation.NavigationWrapper
import com.example.tensoscan.ui.theme.TensoScanTheme
import com.example.tensoscan.ui.utils.Constants.CAMERA_PERMISSION
import com.example.tensoscan.ui.utils.PermissionUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PermissionUtils.arePermissionsGranted(this, CAMERA_PERMISSION)) {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, 100)
        }

        enableEdgeToEdge()
        setContent {
            TensoScanTheme {
                NavigationWrapper()
            }
        }
    }
}
