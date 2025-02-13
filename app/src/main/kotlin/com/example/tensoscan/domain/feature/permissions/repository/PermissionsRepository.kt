package com.example.tensoscan.domain.feature.permissions.repository

import android.app.Activity

interface PermissionsRepository {

    fun arePermissionsGranted(permissions: Array<String>): Boolean

    fun requestPermissions(activity: Activity, permissions: Array<String>)
}