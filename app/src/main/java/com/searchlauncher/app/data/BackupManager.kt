package com.searchlauncher.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.searchlauncher.app.ui.dataStore
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
  private val context: Context,
  private val snippetsRepository: SnippetsRepository,
  private val searchShortcutRepository: SearchShortcutRepository,
  private val favoritesRepository: FavoritesRepository,
) {
  companion object {
    const val BACKUP_VERSION = 2 // Bumped version for new format
    private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB limit
  }

  suspend fun exportBackup(outputStream: OutputStream, backgroundUri: String?): Result<Int> =
    withContext(Dispatchers.IO) {
      try {
        val backupData = JSONObject()
        backupData.put("version", BACKUP_VERSION)

        // Export Snippets items
        val snippetsArray = JSONArray()
        snippetsRepository.items.value.forEach { item ->
          val obj = JSONObject()
          obj.put("alias", item.alias)
          obj.put("content", item.content)
          snippetsArray.put(obj)
        }
        backupData.put("snippets", snippetsArray)

        // Export Search Shortcuts
        val shortcutsArray = JSONArray()
        searchShortcutRepository.items.value.forEach { shortcut ->
          val obj = JSONObject()
          obj.put("id", shortcut.id)
          obj.put("alias", shortcut.alias)
          obj.put("urlTemplate", shortcut.urlTemplate)
          obj.put("description", shortcut.description)
          shortcut.color?.let { obj.put("color", it) }
          shortcut.suggestionUrl?.let { obj.put("suggestionUrl", it) }
          shortcut.packageName?.let { obj.put("packageName", it) }
          shortcutsArray.put(obj)
        }
        backupData.put("searchShortcuts", shortcutsArray)

        // Export Favorites
        val favoritesArray = JSONArray()
        favoritesRepository.getFavoriteIds().forEach { id -> favoritesArray.put(id) }
        backupData.put("favorites", favoritesArray)

        // Export Background Image
        if (backgroundUri != null) {
          val imageBase64 = encodeImageToBase64(Uri.parse(backgroundUri))
          backupData.put("backgroundImage", imageBase64)
        } else {
          backupData.put("backgroundImage", JSONObject.NULL)
        }

        outputStream.write(backupData.toString(2).toByteArray())
        outputStream.flush()

        val totalItems =
          snippetsRepository.items.value.size +
            searchShortcutRepository.items.value.size +
            favoritesRepository.getFavoriteIds().size

        Result.success(totalItems)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun importBackup(inputStream: InputStream): Result<ImportStats> =
    withContext(Dispatchers.IO) {
      try {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val backupData = JSONObject(jsonString)

        val version = backupData.getInt("version")
        // We can support older versions if needed, but for now let's just
        // check
        if (version > BACKUP_VERSION) {
          return@withContext Result.failure(
            Exception(
              "Backup file version ($version) is newer than supported version ($BACKUP_VERSION)"
            )
          )
        }

        var snippetsCount = 0
        var shortcutsCount = 0
        var favoritesCount = 0
        var backgroundRestored = false

        // Import Snippets items (support both old and new format)
        val snippetsKey =
          when {
            backupData.has("snippets") -> "snippets"
            backupData.has("quickCopy") -> "quickCopy"
            else -> null
          }

        if (snippetsKey != null) {
          val snippetsArray = backupData.getJSONArray(snippetsKey)
          for (i in 0 until snippetsArray.length()) {
            val obj = snippetsArray.getJSONObject(i)
            val alias = obj.getString("alias")
            val content = obj.getString("content")
            snippetsRepository.addItem(alias, content)
            snippetsCount++
          }
        }

        // Import Search Shortcuts
        if (backupData.has("searchShortcuts")) {
          val shortcutsArray = backupData.getJSONArray("searchShortcuts")
          val newShortcuts = mutableListOf<SearchShortcut>()

          for (i in 0 until shortcutsArray.length()) {
            val obj = shortcutsArray.getJSONObject(i)
            val shortcut =
              SearchShortcut(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                alias = obj.getString("alias"),
                urlTemplate = obj.getString("urlTemplate"),
                description = obj.getString("description"),
                color = if (obj.has("color")) obj.getLong("color") else null,
                suggestionUrl = obj.optString("suggestionUrl").takeIf { it.isNotEmpty() },
                packageName = obj.optString("packageName").takeIf { it.isNotEmpty() },
              )
            newShortcuts.add(shortcut)
            shortcutsCount++
          }
          searchShortcutRepository.replaceAll(newShortcuts)
        } else if (backupData.has("customShortcuts")) {
          // Legacy backup support
          val shortcutsArray = backupData.getJSONArray("customShortcuts")
          val newShortcuts = mutableListOf<SearchShortcut>()
          for (i in 0 until shortcutsArray.length()) {
            val obj = shortcutsArray.getJSONObject(i)
            if (obj.getString("type") == "search") {
              val shortcut =
                SearchShortcut(
                  id = java.util.UUID.randomUUID().toString(),
                  alias = obj.getString("trigger"),
                  urlTemplate = obj.getString("urlTemplate"),
                  description = obj.getString("description"),
                  color = obj.optLong("color"),
                  suggestionUrl = obj.optString("suggestionUrl").takeIf { it.isNotEmpty() },
                  packageName = obj.optString("packageName").takeIf { it.isNotEmpty() },
                )
              newShortcuts.add(shortcut)
              shortcutsCount++
            }
          }
          searchShortcutRepository.replaceAll(newShortcuts)
        }

        // Import Favorites
        if (backupData.has("favorites")) {
          val favoritesArray = backupData.getJSONArray("favorites")
          val favoriteIds = mutableListOf<String>()
          for (i in 0 until favoritesArray.length()) {
            favoriteIds.add(favoritesArray.getString(i))
            favoritesCount++
          }
          favoritesRepository.replaceAll(favoriteIds)
        }

        // Import Background Image
        if (backupData.has("backgroundImage") && !backupData.isNull("backgroundImage")) {
          val imageBase64 = backupData.getString("backgroundImage")
          val uri = decodeBase64ToImage(imageBase64)
          if (uri != null) {
            saveBackgroundUri(uri.toString())
            backgroundRestored = true
          }
        }

        Result.success(
          ImportStats(
            snippetsCount = snippetsCount,
            shortcutsCount = shortcutsCount,
            favoritesCount = favoritesCount,
            backgroundRestored = backgroundRestored,
          )
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun encodeImageToBase64(uri: Uri): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val bytes = inputStream.readBytes()
        if (bytes.size > MAX_IMAGE_SIZE) {
          throw Exception("Image too large (max 10MB)")
        }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun decodeBase64ToImage(base64: String): Uri? {
    return try {
      val bytes = Base64.decode(base64, Base64.NO_WRAP)
      val fileName = "background_${System.currentTimeMillis()}.jpg"
      val file = java.io.File(context.filesDir, fileName)
      file.writeBytes(bytes)
      Uri.fromFile(file)
    } catch (e: Exception) {
      null
    }
  }

  private suspend fun saveBackgroundUri(uri: String) {
    context.dataStore.edit { preferences ->
      preferences[stringPreferencesKey("background_uri")] = uri
    }
  }

  data class ImportStats(
    val snippetsCount: Int,
    val shortcutsCount: Int,
    val favoritesCount: Int,
    val backgroundRestored: Boolean,
  )
}
