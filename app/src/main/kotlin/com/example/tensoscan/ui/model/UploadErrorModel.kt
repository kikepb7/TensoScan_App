package com.example.tensoscan.ui.model

import com.example.tensoscan.R.string as RString

enum class UploadErrorModel(val message: Int) {
    Timeout(message = RString.upload_state_timeout_error),
    Unknown(message = RString.upload_state_unkwnown_error)
}