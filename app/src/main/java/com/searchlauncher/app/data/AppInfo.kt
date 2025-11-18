package com.searchlauncher.app.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val lastUsedTime: Long = 0
)
