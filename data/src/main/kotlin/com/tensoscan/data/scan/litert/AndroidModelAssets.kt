package com.tensoscan.data.scan.litert

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidModelAssets @Inject constructor(@ApplicationContext private val context: Context) : ModelAssets {
    override fun list(directory: String): List<String> = context.assets.list(directory)?.toList().orEmpty()

    override fun read(path: String): ByteArray = context.assets.open(path).use { it.readBytes() }
}
