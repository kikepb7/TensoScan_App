package com.example.tensoscan.domain.feature.permissions.usecase

import android.app.Activity
import com.example.tensoscan.domain.feature.permissions.repository.PermissionsRepository

class RequestPermissionUseCase(private val permissionsRepository: PermissionsRepository) {
    operator fun invoke(activity: Activity, permissions: Array<String>) {
        permissionsRepository.requestPermissions(activity = activity, permissions = permissions)
    }
}