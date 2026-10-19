package com.tensoscan.data.feature.measurements.report

import android.content.Context
import com.tensoscan.domain.coroutines.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Where downloaded reports are kept until the user has shared them. */
internal interface ReportFileStore {
    /** Writes [content] as [name] and returns the path of the file. */
    suspend fun save(name: String, content: ByteArray): String
}

/**
 * Keeps reports in the cache directory, which Android may clear when space is needed: a report is a temporary export
 * that can always be downloaded again. It is shared with other apps through the `FileProvider` declared in the app.
 */
internal class CacheReportFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : ReportFileStore {
    override suspend fun save(name: String, content: ByteArray): String = withContext(dispatchers.io) {
        val directory = File(context.cacheDir, REPORTS_DIRECTORY).apply { mkdirs() }
        val file = File(directory, name)
        file.writeBytes(content)
        file.absolutePath
    }

    companion object {
        /** Must match the `cache-path` in the app's `file_paths.xml`. */
        const val REPORTS_DIRECTORY = "reports"
    }
}
