package com.example.tensoscan.ui.feature.permissions

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.example.tensoscan.domain.feature.permissions.usecase.CheckPermissionsUseCase
import com.example.tensoscan.domain.feature.permissions.usecase.RequestPermissionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionsViewModel(
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val requestPermissionsViewModel: RequestPermissionUseCase
): ViewModel() {

    private val _state = MutableStateFlow(false)
    val state = _state.asStateFlow()

    fun checkPermissions(permissions: Array<String>) {
        _state.update { checkPermissionsUseCase(permissions = permissions) }
    }

    fun requestPermissions(activity: Activity, permissions: Array<String>) {
        requestPermissionsViewModel(activity = activity, permissions = permissions)
    }
}