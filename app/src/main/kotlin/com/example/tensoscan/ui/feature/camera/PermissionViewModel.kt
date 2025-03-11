package com.example.tensoscan.ui.feature.camera

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.tensoscan.ui.utils.Constants.CAMERA_PERMISSION
import com.example.tensoscan.ui.utils.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionViewModel(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsUiState())
    val state = _state.asStateFlow()

    fun checkPermissions(context: Context) {
        val granted = permissionManager.arePermissionsGranted(context, CAMERA_PERMISSION)
        _state.update { it.copy(hasPermissions = granted) }
    }

    fun requestPermissions(activity: Activity) {
        permissionManager.requestPermissions(activity = activity, permissions = CAMERA_PERMISSION, requestCode = 100)
    }
}

data class PermissionsUiState(
    val hasPermissions: Boolean = false
)