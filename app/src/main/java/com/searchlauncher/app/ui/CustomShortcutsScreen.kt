package com.searchlauncher.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.searchlauncher.app.SearchLauncherApp
import com.searchlauncher.app.data.CustomShortcut
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomShortcutsScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        val app = context.applicationContext as SearchLauncherApp
        val shortcuts by app.customShortcutRepository.items.collectAsState()
        val scope = rememberCoroutineScope()

        var showDialog by remember { mutableStateOf(false) }
        var editingShortcut by remember { mutableStateOf<CustomShortcut?>(null) }

        val snackbarHostState = remember { SnackbarHostState() }
        var deletedShortcut by remember { mutableStateOf<CustomShortcut?>(null) }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Custom Shortcuts") },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                actions = {
                                        TextButton(
                                                onClick = {
                                                        scope.launch {
                                                                app.customShortcutRepository
                                                                        .resetToDefaults()
                                                        }
                                                }
                                        ) { Text("Reset Defaults") }
                                }
                        )
                },
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = {
                                        editingShortcut = null
                                        showDialog = true
                                }
                        ) { Icon(Icons.Default.Add, contentDescription = "Add Shortcut") }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        items(shortcuts) { shortcut ->
                                ShortcutItem(
                                        shortcut = shortcut,
                                        onEdit = {
                                                editingShortcut = shortcut
                                                showDialog = true
                                        },
                                        onDelete = {
                                                deletedShortcut = shortcut
                                                scope.launch {
                                                        app.customShortcutRepository.removeShortcut(
                                                                shortcut
                                                        )
                                                        val result =
                                                                snackbarHostState.showSnackbar(
                                                                        message =
                                                                                "Shortcut deleted",
                                                                        actionLabel = "Undo",
                                                                        duration =
                                                                                SnackbarDuration
                                                                                        .Short
                                                                )
                                                        if (result == SnackbarResult.ActionPerformed
                                                        ) {
                                                                deletedShortcut?.let {
                                                                        app.customShortcutRepository
                                                                                .addShortcut(it)
                                                                }
                                                        }
                                                        deletedShortcut = null
                                                }
                                        }
                                )
                        }
                }
        }

        if (showDialog) {
                ShortcutDialog(
                        shortcut = editingShortcut,
                        onDismiss = { showDialog = false },
                        onSave = { newShortcut ->
                                scope.launch {
                                        if (editingShortcut != null) {
                                                app.customShortcutRepository.updateShortcut(
                                                        editingShortcut!!,
                                                        newShortcut
                                                )
                                        } else {
                                                app.customShortcutRepository.addShortcut(
                                                        newShortcut
                                                )
                                        }
                                        showDialog = false
                                }
                        }
                )
        }
}

@Composable
fun ShortcutItem(shortcut: CustomShortcut, onEdit: () -> Unit, onDelete: () -> Unit) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = shortcut.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                if (shortcut is CustomShortcut.Search) {
                                        Text(
                                                text = "Trigger: ${shortcut.trigger}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                        Row {
                                IconButton(onClick = onEdit) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = onDelete) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                        )
                                }
                        }
                }
        }
}

@Composable
fun ShortcutDialog(
        shortcut: CustomShortcut?,
        onDismiss: () -> Unit,
        onSave: (CustomShortcut) -> Unit
) {
        // Default to Search type for new shortcuts
        var type by remember {
                mutableStateOf(if (shortcut is CustomShortcut.Action) "Action" else "Search")
        }

        // Search fields
        var trigger by remember {
                mutableStateOf((shortcut as? CustomShortcut.Search)?.trigger ?: "")
        }
        var urlTemplate by remember {
                mutableStateOf((shortcut as? CustomShortcut.Search)?.urlTemplate ?: "")
        }
        var description by remember { mutableStateOf(shortcut?.description ?: "") }
        var colorHex by remember {
                mutableStateOf(
                        ((shortcut as? CustomShortcut.Search)?.color ?: 0xFF000000).toString(16)
                )
        }

        // Action fields
        var intentUri by remember {
                mutableStateOf((shortcut as? CustomShortcut.Action)?.intentUri ?: "")
        }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(if (shortcut != null) "Edit Shortcut" else "New Shortcut") },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Type Selector (only for new shortcuts or if we want to allow
                                // changing type)
                                // For simplicity, let's stick to the current type if editing

                                OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth()
                                )

                                if (type == "Search") {
                                        OutlinedTextField(
                                                value = trigger,
                                                onValueChange = { trigger = it },
                                                label = { Text("Trigger (e.g., 'r')") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                                value = urlTemplate,
                                                onValueChange = { urlTemplate = it },
                                                label = { Text("URL Template (use %s for query)") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                } else {
                                        OutlinedTextField(
                                                value = intentUri,
                                                onValueChange = { intentUri = it },
                                                label = { Text("Intent URI") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                }

                                OutlinedTextField(
                                        value = colorHex,
                                        onValueChange = { colorHex = it },
                                        label = { Text("Color (Hex)") },
                                        modifier = Modifier.fillMaxWidth()
                                )
                        }
                },
                confirmButton = {
                        Button(
                                onClick = {
                                        val color =
                                                try {
                                                        colorHex.toLong(16)
                                                } catch (e: Exception) {
                                                        0xFF000000
                                                }

                                        val newShortcut =
                                                if (type == "Search") {
                                                        CustomShortcut.Search(
                                                                trigger = trigger,
                                                                urlTemplate = urlTemplate,
                                                                description = description,
                                                                color = color,
                                                                suggestionUrl =
                                                                        (shortcut as?
                                                                                        CustomShortcut.Search)
                                                                                ?.suggestionUrl,
                                                                packageName =
                                                                        (shortcut as?
                                                                                        CustomShortcut.Search)
                                                                                ?.packageName
                                                        )
                                                } else {
                                                        CustomShortcut.Action(
                                                                intentUri = intentUri,
                                                                description = description
                                                        )
                                                }
                                        onSave(newShortcut)
                                }
                        ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}
