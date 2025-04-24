package com.example.ui.model

import com.example.ui.R.string as RString

enum class UploadErrorModel(val message: Int) {
    Timeout(message = RString.upload_state_timeout_error),
    Unknown(message = RString.upload_state_unkwnown_error)
}