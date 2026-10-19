package com.tensoscan.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Reversible cipher that does not need the Android Keystore. A value without the prefix cannot be decrypted. */
internal class FakeTokenCipher : TokenCipher {
    override fun encrypt(plain: String): String = "$PREFIX$plain"

    override fun decrypt(encoded: String): String? =
        if (encoded.startsWith(PREFIX)) encoded.removePrefix(PREFIX) else null

    private companion object {
        const val PREFIX = "enc:"
    }
}

internal fun sessionDataSource(
    directory: File,
    scope: CoroutineScope,
    cipher: TokenCipher = FakeTokenCipher(),
): SessionLocalDataSource {
    val store = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(directory, "session.preferences_pb") },
    )
    return SessionLocalDataSource(store = store, cipher = cipher)
}
