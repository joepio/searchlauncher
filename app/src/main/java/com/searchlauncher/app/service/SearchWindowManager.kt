package com.searchlauncher.app.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.searchlauncher.app.data.SearchRepository
import com.searchlauncher.app.data.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchWindowManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var searchView: View? = null
    private var lifecycleOwner: MyLifecycleOwner? = null
    private val searchRepository = SearchRepository(context)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        scope.launch {
            searchRepository.initialize()
        }
    }

    fun show() {
        if (searchView != null) return

        lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner?.onCreate()
        lifecycleOwner?.onStart()
        lifecycleOwner?.onResume()

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            isFocusable = true
            isFocusableInTouchMode = true

            setContent {
                SearchUI(
                    onDismiss = { hide() },
                    searchRepository = searchRepository
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            dimAmount = 0.5f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        windowManager.addView(composeView, params)
        searchView = composeView
    }

    fun hide() {
        searchView?.let {
            windowManager.removeView(it)
            searchView = null

            lifecycleOwner?.onPause()
            lifecycleOwner?.onStop()
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
        }
    }

    @Composable
    private fun SearchUI(
        onDismiss: () -> Unit,
        searchRepository: SearchRepository
    ) {
        var query by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val focusRequester = remember { FocusRequester() }
        val view = androidx.compose.ui.platform.LocalView.current

        LaunchedEffect(Unit) {
            delay(100) // Give the window a moment to gain focus
            focusRequester.requestFocus()

            // Manually show keyboard
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }

        LaunchedEffect(query) {
            if (query.isEmpty()) {
                isLoading = true
                searchResults = searchRepository.searchApps("")
                isLoading = false
            } else {
                delay(300) // Debounce
                isLoading = true
                searchResults = searchRepository.search(query)
                isLoading = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) { }
            ) {
                // Search bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search apps and content…") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Results
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 500.dp)
                        ) {
                            items(searchResults) { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = {
                                        launchResult(context, result)
                                        onDismiss()
                                    }
                                )
                            }

                            if (searchResults.isEmpty() && query.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "No results found",
                                        modifier = Modifier.padding(32.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchResultItem(
        result: SearchResult,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon would be shown here if we had a way to render Drawable in Compose
            // For now, we'll use a placeholder

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = result.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                result.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private fun launchResult(context: Context, result: SearchResult) {
        when (result) {
            is SearchResult.App -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(result.packageName)
                launchIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            is SearchResult.Content -> {
                // Launch deep link if available
                result.deepLink?.let { deepLink ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(deepLink)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
