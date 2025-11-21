package com.searchlauncher.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.searchlauncher.app.data.SearchResult
import com.searchlauncher.app.ui.toImageBitmap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesRow(
        favorites: List<SearchResult>,
        onLaunch: (SearchResult) -> Unit,
        onRemoveFavorite: (SearchResult) -> Unit
) {
 val isCrowded = favorites.size > 5
 val iconSize = if (isCrowded) 40.dp else 48.dp
 val spacing = 4.dp

 LazyRow(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.Start),
         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
 ) {
  items(favorites) { result ->
   var showMenu by remember { mutableStateOf(false) }

   Box(
           modifier =
                   Modifier.size(iconSize)
                           .clip(RoundedCornerShape(12.dp))
                           .combinedClickable(
                                   onClick = { onLaunch(result) },
                                   onLongClick = { showMenu = true }
                           ),
           contentAlignment = Alignment.Center
   ) {
    val imageBitmap = result.icon?.toImageBitmap()
    if (imageBitmap != null) {
     val imageSize = if (isCrowded) 32.dp else 40.dp
     Image(
             bitmap = imageBitmap,
             contentDescription = result.title,
             modifier = Modifier.size(imageSize)
     )
    }

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            properties = PopupProperties(focusable = false)
    ) {
     DropdownMenuItem(
             text = { Text("Remove from Favorites") },
             onClick = {
              onRemoveFavorite(result)
              showMenu = false
             },
             leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) }
     )

     if (result is SearchResult.App) {
      val context = LocalContext.current
      DropdownMenuItem(
              text = { Text("App Info") },
              onClick = {
               try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${result.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
               } catch (e: Exception) {
                Toast.makeText(context, "Cannot open App Info", Toast.LENGTH_SHORT).show()
               }
               showMenu = false
              },
              leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
      )
      DropdownMenuItem(
              text = { Text("Uninstall") },
              onClick = {
               try {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:${result.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
               } catch (e: Exception) {
                Toast.makeText(context, "Cannot start uninstall", Toast.LENGTH_SHORT).show()
               }
               showMenu = false
              },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
      )
     }
    }
   }
  }
 }
}
