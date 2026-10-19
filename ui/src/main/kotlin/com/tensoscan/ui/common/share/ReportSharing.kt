package com.tensoscan.ui.common.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens the system share sheet for a report saved by `:data` (REQ-A003-14). The file is handed over through the app's
 * `FileProvider`, so the receiving app gets read access to that one file and nothing else.
 *
 * @return false when no app can handle it.
 */
internal fun shareReport(context: Context, path: String, mimeType: String, chooserTitle: String): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, chooserTitle))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
