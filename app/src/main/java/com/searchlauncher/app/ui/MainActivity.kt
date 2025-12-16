package com.searchlauncher.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.searchlauncher.app.SearchLauncherApp
import com.searchlauncher.app.service.OverlayService
import com.searchlauncher.app.ui.theme.SearchLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

  private val exportBackupLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      uri?.let { lifecycleScope.launch { performExport(it) } }
    }

  private val importBackupLauncher =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let { lifecycleScope.launch { performImport(it) } }
    }

  lateinit var appWidgetManager: android.appwidget.AppWidgetManager
  lateinit var appWidgetHost: android.appwidget.AppWidgetHost
  private val APPWIDGET_HOST_ID = 1002
  private val REQUEST_CREATE_APPWIDGET = 5
  private val REQUEST_PICK_APPWIDGET = 9

  /*
  private val pickWidgetLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        val data = result.data
        val extras = data?.extras
        val appWidgetId =
          extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId != -1) {
          configureWidget(appWidgetId)
        }
      } else {
        // If canceled, we might need to delete the allocated ID, but for default picker it usually
        // handles it.
        // But if we allocated it beforehand, we should delete it.
        // In our flow, we allocate ID BEFORE picking for simplicity if using allocateAppWidgetId
      }
    }
    */

  private val createWidgetLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        val data = result.data
        val extras = data?.extras
        val appWidgetId =
          extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId != -1) {
          lifecycleScope.launch {
            (application as SearchLauncherApp).widgetRepository.addWidgetId(appWidgetId)
          }
        }
      } else {
        // If cancelled, delete the allocated ID
        val data = result.data
        val extras = data?.extras
        val appWidgetId =
          extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId != -1) {
          appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
      }
    }

  private var pendingAppWidgetId = -1

  private val bindWidgetLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      android.util.Log.d("MainActivity", "bindWidgetLauncher result: ${result.resultCode}")

      val data = result.data
      val extras = data?.extras
      var appWidgetId =
        extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

      // Fallback to pending ID if result stripped it
      if (appWidgetId == -1 && pendingAppWidgetId != -1) {
        android.util.Log.d("MainActivity", "Recovered pending ID: $pendingAppWidgetId")
        appWidgetId = pendingAppWidgetId
      }

      pendingAppWidgetId = -1 // Reset pending ID

      if (result.resultCode == RESULT_OK || appWidgetId != -1) {
        // Check if bound (handling both OK result and False Negative cancelled result)
        if (appWidgetId != -1) {
          val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
          if (appWidgetInfo != null) {
            android.util.Log.d(
              "MainActivity",
              "Widget $appWidgetId verified bound. Configuring/Adding.",
            )
            try {
              configureWidget(appWidgetId)
            } catch (e: Exception) {
              android.util.Log.e("MainActivity", "Config failed, force adding", e)
              lifecycleScope.launch {
                (application as SearchLauncherApp).widgetRepository.addWidgetId(appWidgetId)
              }
              Toast.makeText(this, "Configuration skipped.", Toast.LENGTH_SHORT).show()
            }
          } else {
            android.util.Log.e("MainActivity", "Widget $appWidgetId NOT bound.")
            appWidgetHost.deleteAppWidgetId(appWidgetId)
            // Only show error if we really expected it to work (OK result) or user cancelled
            if (result.resultCode == RESULT_OK) {
              Toast.makeText(this, "Binding verification failed.", Toast.LENGTH_SHORT).show()
            }
          }
        } else {
          Toast.makeText(this, "Binding failed (No ID).", Toast.LENGTH_SHORT).show()
        }
      } else {
        // Explicit cancellation with no recovered ID (unlikely if pending logic works)
        android.util.Log.d("MainActivity", "bindWidgetLauncher cancelled/failed")
      }
    }

  fun requestWidgetPick() {
    queryState = "widgets "
    focusTrigger = System.currentTimeMillis()
  }

  private fun onWidgetProviderSelected(providerInfo: android.appwidget.AppWidgetProviderInfo) {
    var appWidgetId = -1
    try {
      appWidgetId = appWidgetHost.allocateAppWidgetId()
      val allowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
      if (allowed) {
        configureWidget(appWidgetId)
      } else {
        val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(
          android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
          providerInfo.provider,
        )
        bindWidgetLauncher.launch(intent)
      }
    } catch (e: SecurityException) {
      android.util.Log.e("MainActivity", "SecurityException adding widget", e)
      if (appWidgetId != -1) {
        Toast.makeText(
            this,
            "Restricted Settings detected. Attempting manual bind...",
            Toast.LENGTH_SHORT,
          )
          .show()

        // Fallback: Try to launch the system bind dialog
        try {
          val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND)
          intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
          intent.putExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
            providerInfo.provider,
          )
          pendingAppWidgetId = appWidgetId // Track ID in case result strips it
          bindWidgetLauncher.launch(intent)
        } catch (innerE: Exception) {
          android.util.Log.e("MainActivity", "Fallback failed", innerE)
          Toast.makeText(
              this,
              "Permission denied. Enable 'Restricted Settings' in App Info, then FORCE STOP the app.",
              Toast.LENGTH_LONG,
            )
            .show()
        }
      } else {
        Toast.makeText(this, "Allocation failed due to restriction.", Toast.LENGTH_SHORT).show()
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Error adding widget", e)
      Toast.makeText(this, "Error adding widget: ${e.message}", Toast.LENGTH_SHORT).show()
    }
    showWidgetPicker.value = false
  }

  // State to control custom picker visibility
  private val showWidgetPicker = androidx.compose.runtime.mutableStateOf(false)

  private fun configureWidget(appWidgetId: Int) {
    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
    if (appWidgetInfo?.configure != null) {
      val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
      intent.component = appWidgetInfo.configure
      intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      createWidgetLauncher.launch(intent)
    } else {
      lifecycleScope.launch {
        (application as SearchLauncherApp).widgetRepository.addWidgetId(appWidgetId)
      }
    }
  }

  private var queryState by mutableStateOf("")
  private var currentScreenState by mutableStateOf(Screen.Search)
  private var focusTrigger by mutableStateOf(0L)
  private var homeToAppList = false

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent) {
    // Check if we should open settings directly
    if (intent.getBooleanExtra("open_settings", false)) {
      currentScreenState = Screen.Settings
      return
    }

    if (intent.hasCategory(Intent.CATEGORY_HOME) && intent.action == Intent.ACTION_MAIN) {
      queryState = ""
      if (homeToAppList) {
        currentScreenState = Screen.AppList
      } else {
        currentScreenState = Screen.Search
        focusTrigger = System.currentTimeMillis()
      }
    }
  }

  fun exportBackup() {
    val timestamp =
      java.text
        .SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        .format(java.util.Date())
    val fileName = "searchlauncher_backup_$timestamp.searchlauncher"

    exportBackupLauncher.launch(fileName)
  }

  fun importBackup() {
    importBackupLauncher.launch(arrayOf("*/*"))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appWidgetManager = android.appwidget.AppWidgetManager.getInstance(applicationContext)
    appWidgetHost = android.appwidget.AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)
    enableEdgeToEdge()
    // Ensure keyboard opens automatically
    @Suppress("DEPRECATION")
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    lifecycleScope.launch {
      dataStore.data
        .map { it[PreferencesKeys.HOME_TO_APPLIST] ?: false }
        .collect {
          // homeToAppList = it -- Disabled for now per user request
          homeToAppList = false
        }
    }

    setContent {
      val themeColor =
        remember { dataStore.data.map { it[PreferencesKeys.THEME_COLOR] ?: 0xFF5E6D4E.toInt() } }
          .collectAsState(initial = 0xFF5E6D4E.toInt())

      val themeSaturation =
        remember { dataStore.data.map { it[PreferencesKeys.THEME_SATURATION] ?: 50f } }
          .collectAsState(initial = 50f)

      val darkMode =
        remember { dataStore.data.map { it[PreferencesKeys.DARK_MODE] ?: 0 } }
          .collectAsState(initial = 0)

      val isOled =
        remember { dataStore.data.map { it[PreferencesKeys.OLED_MODE] ?: false } }
          .collectAsState(initial = false)

      SearchLauncherTheme(
        themeColor = themeColor.value,
        darkThemeMode = darkMode.value,
        chroma = themeSaturation.value,
        isOled = isOled.value,
      ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainScreen()
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    appWidgetHost.startListening()
  }

  override fun onStop() {
    super.onStop()
    try {
      appWidgetHost.stopListening()
    } catch (e: Exception) {
      // Ignore
    }
  }

  private enum class Screen {
    Search,
    Settings,
    CustomShortcuts,
    AppList,
  }

  @Composable
  private fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPractice by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Hoist wallpaper state
    // Hoist wallpaper state
    val backgroundFolderUriString by
      remember { context.dataStore.data.map { it[PreferencesKeys.BACKGROUND_FOLDER_URI] } }
        .collectAsState(initial = null)

    val backgroundUriString by
      remember { context.dataStore.data.map { it[PreferencesKeys.BACKGROUND_URI] } }
        .collectAsState(initial = null)

    val lastImageUriString by
      remember { context.dataStore.data.map { it[PreferencesKeys.BACKGROUND_LAST_IMAGE_URI] } }
        .collectAsState(initial = null)

    var folderImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(backgroundFolderUriString, backgroundUriString) {
      if (backgroundFolderUriString != null) {
        withContext(Dispatchers.IO) {
          try {
            val folderUri = Uri.parse(backgroundFolderUriString)
            val documentFile =
              androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)
            val files = documentFile?.listFiles()
            val images =
              files?.filter { file ->
                val mimeType = file.type
                mimeType != null && mimeType.startsWith("image/")
              }
            val uris = images?.sortedBy { it.name }?.map { it.uri } ?: emptyList()
            withContext(Dispatchers.Main) { folderImages = uris }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      } else if (backgroundUriString != null) {
        try {
          folderImages = listOf(Uri.parse(backgroundUriString))
        } catch (e: Exception) {
          e.printStackTrace()
        }
      } else {
        // Load default assets
        withContext(Dispatchers.IO) {
          try {
            val assets = context.assets.list("")
            val defaults =
              assets
                ?.filter { it.startsWith("BG") && (it.endsWith(".jpg") || it.endsWith(".png")) }
                ?.sorted() // Ensure consistent order
                ?.map { Uri.parse("file:///android_asset/$it") } ?: emptyList()
            withContext(Dispatchers.Main) { folderImages = defaults }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }

    val showHistory =
      remember { context.dataStore.data.map { it[booleanPreferencesKey("show_history")] ?: false } }
        .collectAsState(initial = false)

    // Handle back press
    BackHandler(enabled = currentScreenState != Screen.Search) {
      if (currentScreenState == Screen.CustomShortcuts) {
        currentScreenState = Screen.Settings
      } else {
        currentScreenState = Screen.Search
        focusTrigger = System.currentTimeMillis()
      }
    }

    val swipeGestureEnabled =
      remember { context.dataStore.data.map { it[PreferencesKeys.SWIPE_GESTURE_ENABLED] ?: false } }
        .collectAsState(initial = false)

    if (showPractice) {
      PracticeGestureScreen(onBack = { showPractice = false })
    } else {
      // Auto-start service if enabled in settings AND permissions are granted
      LaunchedEffect(swipeGestureEnabled.value) {
        if (swipeGestureEnabled.value) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            startOverlayService()
          }
        } else {
          stopOverlayService()
        }
      }

      // Hide keyboard when opening App List
      LaunchedEffect(currentScreenState) {
        if (currentScreenState == Screen.AppList) {
          keyboardController?.hide()
          focusManager.clearFocus()
        }
      }

      Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Base Content (Search, Settings, etc.)
        // When AppList is open, we want the base layer to be Search
        val baseState =
          if (currentScreenState == Screen.AppList) Screen.Search else currentScreenState

        androidx.compose.animation.AnimatedContent(
          targetState = baseState,
          transitionSpec = {
            androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
          },
          label = "BaseScreenTransition",
        ) { targetState ->
          when (targetState) {
            Screen.Search -> {
              val app = application as SearchLauncherApp
              SearchScreen(
                query = queryState,
                onQueryChange = { queryState = it },
                onDismiss = { queryState = "" },
                onOpenSettings = {
                  keyboardController?.hide()
                  currentScreenState = Screen.Settings
                },
                onOpenAppDrawer = { currentScreenState = Screen.AppList },
                searchRepository = app.searchRepository,
                focusTrigger = focusTrigger,
                showHistory = showHistory.value,
                showBackgroundImage = true,
                folderImages = folderImages,
                lastImageUriString = lastImageUriString,
                onAddWidget = { requestWidgetPick() },
                isActive = currentScreenState == Screen.Search,
              )
            }
            Screen.Settings -> {
              SettingsScreen(
                onStartService = { startOverlayService() },
                onStopService = { stopOverlayService() },
                onOpenPractice = { showPractice = true },
                onOpenCustomShortcuts = { currentScreenState = Screen.CustomShortcuts },
                onBack = { currentScreenState = Screen.Search },
              )
            }
            Screen.CustomShortcuts -> {
              CustomShortcutsScreen(onBack = { currentScreenState = Screen.Settings })
            }
            else -> {
              /* No-op */
            }
          }
        }

        // Layer 2: App List Overlay
        androidx.compose.animation.AnimatedVisibility(
          visible = currentScreenState == Screen.AppList,
          enter =
            androidx.compose.animation.slideInVertically { height -> height } +
              androidx.compose.animation.fadeIn(),
          exit =
            androidx.compose.animation.slideOutVertically { height -> height } +
              androidx.compose.animation.fadeOut(),
          modifier = Modifier.fillMaxSize(),
        ) {
          val app = application as SearchLauncherApp
          AppListScreen(
            searchRepository = app.searchRepository,
            onAppClick = { result ->
              launchApp(context, result)
              currentScreenState = Screen.Search
              queryState = ""
            },
            onBack = {
              currentScreenState = Screen.Search
              focusTrigger = System.currentTimeMillis()
            },
          )
        }
      }

      if (showWidgetPicker.value) {
        WidgetPicker(
          appWidgetManager = appWidgetManager,
          onWidgetSelected = { info -> onWidgetProviderSelected(info) },
          onDismiss = { showWidgetPicker.value = false },
        )
      }
    }
  }

  private fun startOverlayService() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (Settings.canDrawOverlays(this)) {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          startForegroundService(intent)
        } else {
          startService(intent)
        }
      }
    }
  }

  private fun stopOverlayService() {
    val intent = Intent(this, OverlayService::class.java)
    stopService(intent)
  }

  private fun launchApp(context: Context, result: com.searchlauncher.app.data.SearchResult) {
    try {
      if (result is com.searchlauncher.app.data.SearchResult.Shortcut) {
        val intent = Intent.parseUri(result.intentUri, Intent.URI_INTENT_SCHEME)

        // Intercept Widget Binding
        if (intent.action == "com.searchlauncher.action.BIND_WIDGET") {
          val componentNameStr = intent.getStringExtra("component")
          if (componentNameStr != null) {
            val component = android.content.ComponentName.unflattenFromString(componentNameStr)
            if (component != null) {
              val providers =
                appWidgetManager.getInstalledProvidersForProfile(android.os.Process.myUserHandle())
              val providerInfo = providers.find { it.provider == component }
              if (providerInfo != null) {
                onWidgetProviderSelected(providerInfo)
                return
              }
            }
          }
        }

        // Intercept Widget Add Intent
        if (intent.action == "com.searchlauncher.action.ADD_WIDGET") {
          queryState = "widgets "
          focusTrigger = System.currentTimeMillis()
          return
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      } else if (result is com.searchlauncher.app.data.SearchResult.App) {
        val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
    }
  }

  fun handleWidgetIntent(intent: Intent) {
    if (intent.action == "com.searchlauncher.action.BIND_WIDGET") {
      val componentStr = intent.getStringExtra("component") ?: return
      val component = android.content.ComponentName.unflattenFromString(componentStr) ?: return
      val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
      val providerInfo = appWidgetManager.installedProviders.find { it.provider == component }
      if (providerInfo != null) {
        onWidgetProviderSelected(providerInfo)
      }
    }
  }

  object PreferencesKeys {
    val THEME_COLOR = intPreferencesKey("theme_color")
    val THEME_SATURATION = floatPreferencesKey("theme_saturation")
    val DARK_MODE = intPreferencesKey("dark_mode")
    val OLED_MODE = booleanPreferencesKey("oled_mode")
    val BACKGROUND_URI =
      androidx.datastore.preferences.core.stringPreferencesKey(
        "background_uri"
      ) // 0: System, 1: Light, 2: Dark
    val BACKGROUND_FOLDER_URI =
      androidx.datastore.preferences.core.stringPreferencesKey("background_folder_uri")
    val BACKGROUND_LAST_IMAGE_URI =
      androidx.datastore.preferences.core.stringPreferencesKey("background_last_image_uri")
    val SWIPE_GESTURE_ENABLED = booleanPreferencesKey("swipe_gesture_enabled")
    val HOME_TO_APPLIST = booleanPreferencesKey("home_to_applist")
    val SHOW_WIDGETS = booleanPreferencesKey("show_widgets")
  }
}

@Composable
private fun SnippetsCard() {
  val context = LocalContext.current
  val app = context.applicationContext as SearchLauncherApp
  val snippetItems = app.snippetsRepository.items.collectAsState()
  val scope = rememberCoroutineScope()

  var showDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<com.searchlauncher.app.data.SnippetItem?>(null) }

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Snippets", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Quick access to frequently used text snippets",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = {
            editingItem = null
            showDialog = true
          }
        ) {
          Text("Add")
        }
      }

      // List existing items
      if (snippetItems.value.isNotEmpty()) {
        Text(
          text = "${snippetItems.value.size} item${if (snippetItems.value.size != 1) "s" else ""}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        snippetItems.value.forEach { item ->
          Card(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.alias,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Text(
                  text = item.content.take(50) + if (item.content.length > 50) "..." else "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row {
                IconButton(
                  onClick = {
                    editingItem = item
                    showDialog = true
                  }
                ) {
                  Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Edit",
                  )
                }
                IconButton(
                  onClick = { scope.launch { app.snippetsRepository.deleteItem(item.alias) } }
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    SnippetDialog(
      item = editingItem,
      onDismiss = { showDialog = false },
      onSave = { alias, content ->
        scope.launch {
          if (editingItem != null) {
            app.snippetsRepository.updateItem(editingItem!!.alias, alias, content)
          } else {
            app.snippetsRepository.addItem(alias, content)
          }
          showDialog = false
        }
      },
    )
  }
}

@Composable
private fun SnippetDialog(
  item: com.searchlauncher.app.data.SnippetItem?,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit,
) {
  var alias by remember { mutableStateOf(item?.alias ?: "") }
  var content by remember { mutableStateOf(item?.content ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (item != null) "Edit Snippet" else "Add Snippet") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = alias,
          onValueChange = { alias = it },
          label = { Text("Alias") },
          placeholder = { Text("e.g., 'bank', 'meet'") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
        )

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Content") },
          placeholder = { Text("The text to copy") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 6,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (alias.isNotBlank() && content.isNotBlank()) {
            onSave(alias.trim(), content.trim())
          }
        },
        enabled = alias.isNotBlank() && content.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun BackupRestoreCard() {
  val context = LocalContext.current
  val activity = context as? MainActivity

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = "Backup & Restore", style = MaterialTheme.typography.titleMedium)
      Text(
        text =
          "Export all your data (Snippets, Shortcuts, Favorites, Background) to a .searchlauncher file",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { activity?.exportBackup() }, modifier = Modifier.weight(1f)) {
          Text("Export")
        }

        OutlinedButton(onClick = { activity?.importBackup() }, modifier = Modifier.weight(1f)) {
          Text("Import")
        }
      }
    }
  }
}

private suspend fun MainActivity.performExport(uri: android.net.Uri) {
  withContext(Dispatchers.IO) {
    try {
      val app = applicationContext as SearchLauncherApp
      val backgroundUri =
        dataStore.data.map { it[MainActivity.PreferencesKeys.BACKGROUND_URI] }.first()

      val backupManager =
        com.searchlauncher.app.data.BackupManager(
          context = this@performExport,
          snippetsRepository = app.snippetsRepository,
          searchShortcutRepository = app.searchShortcutRepository,
          favoritesRepository = app.favoritesRepository,
        )

      contentResolver.openOutputStream(uri)?.use { outputStream ->
        val result = backupManager.exportBackup(outputStream, backgroundUri)
        withContext(Dispatchers.Main) {
          if (result.isSuccess) {
            android.widget.Toast.makeText(
                this@performExport,
                "Backup exported successfully (${result.getOrNull()} items)",
                android.widget.Toast.LENGTH_LONG,
              )
              .show()
          } else {
            android.widget.Toast.makeText(
                this@performExport,
                "Export failed: ${result.exceptionOrNull()?.message}",
                android.widget.Toast.LENGTH_LONG,
              )
              .show()
          }
        }
      }
    } catch (e: Exception) {
      withContext(Dispatchers.Main) {
        android.widget.Toast.makeText(
            this@performExport,
            "Export failed: ${e.message}",
            android.widget.Toast.LENGTH_LONG,
          )
          .show()
      }
    }
  }
}

private suspend fun MainActivity.performImport(uri: android.net.Uri) {
  withContext(Dispatchers.IO) {
    try {
      val app = applicationContext as SearchLauncherApp

      val backupManager =
        com.searchlauncher.app.data.BackupManager(
          context = this@performImport,
          snippetsRepository = app.snippetsRepository,
          searchShortcutRepository = app.searchShortcutRepository,
          favoritesRepository = app.favoritesRepository,
        )

      contentResolver.openInputStream(uri)?.use { inputStream ->
        val result = backupManager.importBackup(inputStream)
        withContext(Dispatchers.Main) {
          if (result.isSuccess) {
            val stats = result.getOrNull()!!
            val message = buildString {
              append("Import successful:\n")
              append("- ${stats.snippetsCount} Snippets\n")
              append("- ${stats.shortcutsCount} Custom Shortcuts\n")
              append("- ${stats.favoritesCount} Favorites")
              if (stats.backgroundRestored) {
                append("\n- Background image restored")
              }
            }
            android.widget.Toast.makeText(
                this@performImport,
                message,
                android.widget.Toast.LENGTH_LONG,
              )
              .show()
          } else {
            android.widget.Toast.makeText(
                this@performImport,
                "Import failed: ${result.exceptionOrNull()?.message}",
                android.widget.Toast.LENGTH_LONG,
              )
              .show()
          }
        }
      }
    } catch (e: Exception) {
      withContext(Dispatchers.Main) {
        android.widget.Toast.makeText(
            this@performImport,
            "Import failed: ${e.message}",
            android.widget.Toast.LENGTH_LONG,
          )
          .show()
      }
    }
  }
}
