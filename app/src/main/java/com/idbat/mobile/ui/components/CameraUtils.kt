package com.idbat.mobile.ui.components

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal fun createCameraUri(context: Context): Uri {
    val file = File(context.cacheDir, "images/photo_${System.currentTimeMillis()}.jpg")
        .also { it.parentFile?.mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
