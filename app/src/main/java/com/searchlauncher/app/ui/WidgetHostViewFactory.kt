package com.searchlauncher.app.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

object WidgetHostViewFactory {
  fun createWidgetView(
    context: Context,
    appWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
  ): View {
    return try {
      val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return View(context)
      val hostView = appWidgetHost.createView(context, appWidgetId, appWidgetInfo)
      hostView.setAppWidget(appWidgetId, appWidgetInfo)

      // Enforce minimum height (often crucial for list widgets like Calendar)
      val density = context.resources.displayMetrics.density
      val minHeightDp = appWidgetInfo.minHeight
      val minHeightPx = (minHeightDp * density).toInt()

      hostView.minimumHeight = minHeightPx

      // Wrap in a FrameLayout for layout params or padding if needed
      val frameLayout = FrameLayout(context)
      frameLayout.addView(
        hostView,
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
      )
      frameLayout
    } catch (e: Exception) {
      android.util.Log.e("WidgetHostViewFactory", "Error creating widget view", e)
      val errorView = View(context)
      errorView.setBackgroundColor(android.graphics.Color.RED)
      errorView.layoutParams = ViewGroup.LayoutParams(100, 100)
      errorView
    }
  }
}
