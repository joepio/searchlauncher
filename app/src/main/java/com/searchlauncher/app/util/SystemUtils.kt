package com.searchlauncher.app.util

import android.annotation.SuppressLint
import android.content.Context
import java.lang.reflect.Method

object SystemUtils {

  @SuppressLint("WrongConstant")
  fun expandNotifications(context: Context) {
    try {
      val statusBarService = context.getSystemService("statusbar")
      val statusBarManager = Class.forName("android.app.StatusBarManager")
      val method: Method = statusBarManager.getMethod("expandNotificationsPanel")
      method.invoke(statusBarService)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @SuppressLint("WrongConstant")
  fun expandQuickSettings(context: Context) {
    try {
      val statusBarService = context.getSystemService("statusbar")
      val statusBarManager = Class.forName("android.app.StatusBarManager")
      val method: Method = statusBarManager.getMethod("expandSettingsPanel")
      method.invoke(statusBarService)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun logError(tag: String, message: String, e: Throwable) {
    if (e is kotlinx.coroutines.CancellationException) throw e
    android.util.Log.e(tag, message, e)
    io.sentry.Sentry.captureException(e)
  }
}
