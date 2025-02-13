package com.example.tensoscan.data.datasource.feature.permissions.repository

import android.app.Activity
import com.example.tensoscan.data.common.PermissionManager
import com.example.tensoscan.domain.feature.permissions.repository.PermissionsRepository

class PermissionsRepositoryImpl(private val permissionManager: PermissionManager): PermissionsRepository {
    override fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissionManager.arePermissionsGranted(permissions = permissions)
    }

    override fun requestPermissions(activity: Activity, permissions: Array<String>) {
        permissionManager.requestPermissions(activity = activity, permissions = permissions, 100)
    }
}