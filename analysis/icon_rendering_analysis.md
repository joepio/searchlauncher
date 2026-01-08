# Icon Rendering & Persistence Analysis

## 1. Problem Description
Users are experiencing "half-rendered" or "empty" icons in the favorites bar. When this occurs, the same icons appear corrupted in other parts of the app (Search Results, App Drawer), while other icons remain fine. This suggests a persistent corruption of the cached icon data.

## 2. Logic Inspection

### 2.1 The Cycle
The icon rendering and caching logic involves a few key components in `SearchRepository.kt`:

1.  **Triggering Updates**:
    The `initialize()` method monitors `favoriteIds`, `historyIds`, and `indexUpdated`. When any of these change, it fetches the latest results and calls `saveResultsToCache`.
    ```kotlin
    // SearchRepository.kt:184
    .collect { (favIds, histIds) ->
      getResults(favIds).let {
        _favorites.value = it
        saveResultsToCache(it, isFavorites = true) // <--- Writes to disk
      }
      // ...
    }
    ```

2.  **Saving to Disk (`saveResultsToCache`)**:
    This function serializes the metadata to JSON and **unconditionally rewrites the icon for every item**.
    ```kotlin
    // SearchRepository.kt:1958
    saveIconToDisk(res.id, res.icon)
    ```

3.  **Writing the Icon (`saveIconToDisk`)**:
    This function writes the bitmap directly to the target file. **It is not atomic.**
    ```kotlin
    // SearchRepository.kt:2098
    val file = File(getIconDir(), "${sanitizeId(id)}.png")
    // ...
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    ```
    Opening `FileOutputStream(file)` immediately truncates the file to 0 bytes. The `compress` method then writes the data chunk by chunk.

4.  **Reading the Icon (`loadIconFromDisk`)**:
    Used by `convertDocumentToResult` (when generating results for Search or App Drawer) to load cached icons.
    ```kotlin
    // SearchRepository.kt:2134
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    ```

### 2.2 The Race Condition
The "half-rendered" artifacts are a classic symptom of a **read-while-write race condition**:

1.  **Write Start**: `saveResultsToCache` starts. It iterates through favorites (e.g., WhatsApp). It calls `saveIconToDisk`.
2.  **Truncate**: `FileOutputStream` opens "com.whatsapp.png", truncating it to 0 bytes.
3.  **Writing**: `bitmap.compress` starts writing PNG data.
4.  **Concurrent Read**: At this exact moment, another part of the app (e.g., `getAllApps` for the drawer, or a re-render of `FavoritesRow` triggering a fetch, or `searchApps`) calls `convertDocumentToResult` -> `loadIconFromDisk("com.whatsapp")`.
5.  **Partial Read**: `BitmapFactory.decodeFile` reads the partially written (or empty) file.
    *   If empty: Returns null (or fallback).
    *   If partial: Attempts to decode. PNG is a stream format; a truncated stream might result in a partial image (top half visible, bottom missing/grey) or a decode failure.
6.  **Cache Poisoning**: The partial/corrupted bitmap is successfully decoded and placed into `iconCache` (memory).
7.  **Persistence**: Now, even if the write completes later, the app holds a corrupted bitmap in memory. Subsequent UI updates use this cached corrupted bitmap.

### 2.3 Why it affects Favorites specifically
Favorites are saved frequently (on reorder, on init, on index update). `saveResultsToCache` rewrites *all* favorite icons every time. This increases the window of opportunity for a collision significantly compared to rarely accessed apps.

## 3. Proposed Fix
To resolve this, we must ensure that:
1.  **Atomic Writes**: Icons are written to a temporary file first, then renamed. This ensures a reader either sees the old valid file or the new valid file, never a partial file.
2.  **Avoid Redundant Writes**: We should not rewrite the icon file if it already exists and hasn't changed.

### Implementation Details
Modify `saveIconToDisk` to:
1.  Check if the target file exists. If so, skips writing (unless we want to force update, but usually the ID maps to the same package/icon).
2.  Use a `.tmp` file for writing.
3.  Use `File.renameTo` (atomic on most FS) to replace the target.

```kotlin
private fun saveIconToDisk(id: String, drawable: Drawable?) {
    if (drawable == null) return
    val targetFile = File(getIconDir(), "${sanitizeId(id)}.png")
    // Optimization: Don't rewrite if exists.
    // If icons can update (e.g. adaptive clock), we might need a versioning strategy or just rely on atomic overwrite.
    // For now, atomic overwrite is safer than skipping if we want updates.

    val tmpFile = File(getIconDir(), "${sanitizeId(id)}.tmp")

    try {
        // Render and write to tmp
        // ... (existing bitmap generation logic) ...
        FileOutputStream(tmpFile).use { out ->
             bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Atomic swap
        if (tmpFile.exists() && tmpFile.length() > 0) {
            tmpFile.renameTo(targetFile)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        tmpFile.delete()
    }
}
```
