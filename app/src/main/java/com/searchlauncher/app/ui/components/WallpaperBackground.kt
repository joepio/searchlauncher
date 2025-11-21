package com.searchlauncher.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.edit
import coil.compose.AsyncImage
import com.searchlauncher.app.ui.MainActivity
import com.searchlauncher.app.ui.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Composable
fun WallpaperBackground(
    showBackgroundImage: Boolean,
    bottomPadding: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    folderImages: List<Uri> = emptyList(),
    lastImageUriString: String? = null
) {
    val context = LocalContext.current

    val backgroundUriString by
            context.dataStore
                    .data
                    .map {
                        if (showBackgroundImage) it[MainActivity.PreferencesKeys.BACKGROUND_URI]
                        else null
                    }
                    .collectAsState(initial = null)

    val contentModifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)
    val pagerState = rememberPagerState(pageCount = { folderImages.size })

    // Scroll to saved page or random page initially when images are loaded
    LaunchedEffect(folderImages) {
        if (folderImages.isNotEmpty()) {
            val initialIndex =
                    if (lastImageUriString != null) {
                        val uri = Uri.parse(lastImageUriString)
                        val index = folderImages.indexOf(uri)
                        if (index != -1) index else folderImages.indices.random()
                    } else {
                        folderImages.indices.random()
                    }
            pagerState.scrollToPage(initialIndex)
        }
    }

    // Save current image URI when page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    if (folderImages.isNotEmpty() && page in folderImages.indices) {
                        val currentUri = folderImages[page].toString()
                        if (currentUri != lastImageUriString) {
                            context.dataStore.edit { prefs ->
                                prefs[MainActivity.PreferencesKeys.BACKGROUND_LAST_IMAGE_URI] =
                                        currentUri
                            }
                        }
                    }
                }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (backgroundUriString != null) {
            AsyncImage(
                    model = Uri.parse(backgroundUriString),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = contentModifier.clickable { onDismiss() }
            )
        } else if (folderImages.isNotEmpty()) {
            HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // Allow swiping beyond bounds? Default is fine.
            ) { page ->
                // We need to handle the click here to allow dismissing
                Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
                    AsyncImage(
                            model = folderImages[page],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = contentModifier
                    )
                }
            }
        } else {
            // Solid background when no image - semi-transparent so app underneath shows through
            Box(
                    modifier =
                            contentModifier
                                    .background(
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                                    )
                                    .clickable { onDismiss() }
            )
        }
    }
}
