package com.searchlauncher.app.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import coil.compose.AsyncImage
import com.searchlauncher.app.service.GestureAccessibilityService
import com.searchlauncher.app.ui.MainActivity
import com.searchlauncher.app.ui.WidgetHostViewFactory
import com.searchlauncher.app.ui.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun WallpaperBackground(
  showBackgroundImage: Boolean,
  bottomPadding: Dp,
  modifier: Modifier = Modifier,
  folderImages: List<Uri> = emptyList(),
  lastImageUriString: String? = null,
  onOpenAppDrawer: () -> Unit = {},
  onLongPress: (Offset) -> Unit = {},
  onTap: () -> Unit = {},
) {
  val context = LocalContext.current

  val contentModifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)
  val pagerState =
    rememberPagerState(pageCount = { if (folderImages.isNotEmpty()) Int.MAX_VALUE else 0 })

  // Scroll to saved page or random page initially when images are loaded
  LaunchedEffect(folderImages) {
    if (folderImages.isNotEmpty()) {
      val targetIndex =
        if (lastImageUriString != null) {
          val uri = Uri.parse(lastImageUriString)
          val index = folderImages.indexOf(uri)
          if (index != -1) index else 0
        } else {
          0
        }

      // Calculate a page in the middle of MAX_VALUE that maps to targetIndex
      val startIndex = Int.MAX_VALUE / 2
      val startOffset = startIndex % folderImages.size
      val initialPage = startIndex - startOffset + targetIndex

      pagerState.scrollToPage(initialPage)
    }
  }

  // Save current image URI when page changes
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
      .collect { page ->
        if (folderImages.isNotEmpty()) {
          val actualIndex = page % folderImages.size
          val currentUri = folderImages[actualIndex].toString()
          if (currentUri != lastImageUriString) {
            context.dataStore.edit { prefs ->
              prefs[MainActivity.PreferencesKeys.BACKGROUND_LAST_IMAGE_URI] = currentUri
            }
          }
        }
      }
  }

  // Visibility toggle for widgets
  val showWidgetsFlow =
    remember(context) {
      context.dataStore.data.map { preferences ->
        preferences[MainActivity.PreferencesKeys.SHOW_WIDGETS] ?: true
      }
    }
  val showWidgets by showWidgetsFlow.collectAsState(initial = true)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            if (dragAmount.y > 20) {
              val isLeft = change.position.x < size.width / 2
              if (isLeft) {
                if (!GestureAccessibilityService.openNotifications()) {
                  com.searchlauncher.app.util.SystemUtils.expandNotifications(context)
                }
              } else {
                if (!GestureAccessibilityService.openQuickSettings()) {
                  com.searchlauncher.app.util.SystemUtils.expandQuickSettings(context)
                }
              }
            } else if (dragAmount.y < -20) {
              onOpenAppDrawer()
            }
          }
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = {
              val newState = !showWidgets
              val scope = CoroutineScope(Dispatchers.IO)
              scope.launch {
                context.dataStore.edit { preferences ->
                  preferences[MainActivity.PreferencesKeys.SHOW_WIDGETS] = newState
                }
              }
              onTap()
            },
            onLongPress = { offset -> onLongPress(offset) },
          )
        }
  ) {
    if (showBackgroundImage && folderImages.isNotEmpty()) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = folderImages.size > 1,
      ) { page ->
        val imageIndex = page % folderImages.size
        Box(modifier = Modifier.fillMaxSize()) {
          AsyncImage(
            model = folderImages[imageIndex],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = contentModifier,
          )
        }
      }
    } else {
      // Solid background when no image - semi-transparent so app underneath shows through
      Box(
        modifier =
          contentModifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
      )
    }

    // Widget Layer

    val app = context.applicationContext as com.searchlauncher.app.SearchLauncherApp
    val widgets by app.widgetRepository.widgets.collectAsState(initial = emptyList())

    var activeWidgetId by remember { mutableIntStateOf(-1) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var resizeHeight by remember { mutableStateOf(400f) }

    val scope = rememberCoroutineScope()

    // Nested scroll for handling "Pull Down" on widgets to open notifications
    var accumulatedPull by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
      object : NestedScrollConnection {
        override fun onPostScroll(
          consumed: Offset,
          available: Offset,
          source: NestedScrollSource,
        ): Offset {
          if (available.y > 0) {
            accumulatedPull += available.y
            if (accumulatedPull > 150f) { // Threshold
              accumulatedPull = 0f
              com.searchlauncher.app.util.SystemUtils.expandNotifications(context)
            }
          } else {
            accumulatedPull = 0f
          }
          return super.onPostScroll(consumed, available, source)
        }
      }
    }

    if (widgets.isNotEmpty()) {
      AnimatedVisibility(visible = showWidgets, enter = fadeIn(), exit = fadeOut()) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = bottomPadding + 80.dp)
              .nestedScroll(nestedScrollConnection)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          widgets.forEachIndexed { index, widget ->
            androidx.compose.runtime.key(widget.id) {
              class WidgetContainerView(context: android.content.Context) :
                android.widget.FrameLayout(context) {
                private var onLongPressListener: (() -> Unit)? = null
                private val gestureDetector =
                  android.view.GestureDetector(
                    context,
                    object : android.view.GestureDetector.SimpleOnGestureListener() {
                      override fun onLongPress(e: android.view.MotionEvent) {
                        onLongPressListener?.invoke()
                      }
                    },
                  )

                fun setOnLongPressAction(action: () -> Unit) {
                  onLongPressListener = action
                }

                override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
                  gestureDetector.onTouchEvent(ev)
                  return super.onInterceptTouchEvent(ev)
                }

                override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
                  gestureDetector.onTouchEvent(ev)
                  return super.dispatchTouchEvent(ev)
                }
              }

              Box(modifier = Modifier.fillMaxWidth()) {
                val heightModifier =
                  if (widget.height != null) {
                    Modifier.height(widget.height.dp)
                  } else {
                    Modifier.wrapContentHeight()
                  }

                AndroidView(
                  factory = { ctx ->
                    val container = WidgetContainerView(ctx)
                    container.setOnLongPressAction { activeWidgetId = widget.id }

                    val activity = ctx as? MainActivity
                    if (activity != null) {
                      val widgetView =
                        WidgetHostViewFactory.createWidgetView(
                          ctx,
                          widget.id,
                          activity.appWidgetHost,
                          activity.appWidgetManager,
                        )
                      container.addView(widgetView)
                    }
                    container
                  },
                  update = { container ->
                    container.setOnLongPressAction { activeWidgetId = widget.id }
                  },
                  modifier = Modifier.fillMaxWidth().then(heightModifier),
                )

                DropdownMenu(
                  expanded = activeWidgetId == widget.id,
                  onDismissRequest = { activeWidgetId = -1 },
                ) {
                  DropdownMenuItem(
                    text = { Text("Resize") },
                    onClick = {
                      val currentHeight = widget.height?.toFloat() ?: 400f
                      resizeHeight = currentHeight
                      activeWidgetId = widget.id
                      showResizeDialog = true
                    },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.PhotoSizeSelectSmall,
                        contentDescription = "Resize",
                      )
                    },
                  )

                  if (index > 0) {
                    DropdownMenuItem(
                      text = { Text("Move Up") },
                      onClick = {
                        scope.launch {
                          app.widgetRepository.moveWidgetUp(widget.id)
                          activeWidgetId = -1
                        }
                      },
                      leadingIcon = {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up")
                      },
                    )
                  }

                  if (index < widgets.size - 1) {
                    DropdownMenuItem(
                      text = { Text("Move Down") },
                      onClick = {
                        scope.launch {
                          app.widgetRepository.moveWidgetDown(widget.id)
                          activeWidgetId = -1
                        }
                      },
                      leadingIcon = {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Down")
                      },
                    )
                  }

                  DropdownMenuItem(
                    text = { Text("Delete Widget") },
                    onClick = {
                      scope.launch {
                        app.widgetRepository.removeWidgetId(widget.id)
                        val activity = context as? MainActivity
                        activity?.appWidgetHost?.deleteAppWidgetId(widget.id)
                        activeWidgetId = -1
                      }
                    },
                    leadingIcon = {
                      Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    },
                  )
                }
              }
            }
          }
        }
      }

      if (showResizeDialog && activeWidgetId != -1) {
        AlertDialog(
          onDismissRequest = {
            showResizeDialog = false
            activeWidgetId = -1
          },
          title = { Text("Resize Widget") },
          text = {
            Column {
              Text("Height: ${resizeHeight.toInt()} dp")
              Slider(
                value = resizeHeight,
                onValueChange = { resizeHeight = it },
                valueRange = 50f..800f,
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                scope.launch {
                  app.widgetRepository.updateWidgetHeight(activeWidgetId, resizeHeight.toInt())
                  showResizeDialog = false
                  activeWidgetId = -1
                }
              }
            ) {
              Text("Save")
            }
          },
          dismissButton = {
            Button(
              onClick = {
                showResizeDialog = false
                activeWidgetId = -1
              }
            ) {
              Text("Cancel")
            }
          },
        )
      }
    }
  }
}
