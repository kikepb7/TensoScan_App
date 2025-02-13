package com.example.tensoscan.domain.feature.permissions.usecase

import com.example.tensoscan.domain.feature.permissions.repository.PermissionsRepository

class CheckPermissionsUseCase(private val permissionsRepository: PermissionsRepository) {
    operator fun invoke(permissions: Array<String>): Boolean {
        return permissionsRepository.arePermissionsGranted(permissions = permissions)
    }
}