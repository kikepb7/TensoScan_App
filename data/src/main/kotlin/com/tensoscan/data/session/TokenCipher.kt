package com.tensoscan.data.session

/** Symmetric encryption of the secrets stored in DataStore. */
internal interface TokenCipher {
    fun encrypt(plain: String): String

    /** Returns null when the value cannot be decrypted (for example, because the key was invalidated). */
    fun decrypt(encoded: String): String?
}
