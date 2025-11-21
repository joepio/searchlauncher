package com.searchlauncher.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickCopyDialog(
        initialAlias: String,
        initialContent: String,
        isEditMode: Boolean,
        onDismiss: () -> Unit,
        onConfirm: (String, String) -> Unit
) {
 var alias by remember { mutableStateOf(initialAlias) }
 var content by remember { mutableStateOf(initialContent) }
 var aliasError by remember { mutableStateOf(false) }

 AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text(if (isEditMode) "Edit Quick Copy" else "New Quick Copy") },
         text = {
          Column {
           OutlinedTextField(
                   value = alias,
                   onValueChange = {
                    alias = it
                    aliasError = false
                   },
                   label = { Text("Alias") },
                   isError = aliasError,
                   singleLine = true,
                   modifier = Modifier.fillMaxWidth()
           )
           if (aliasError) {
            Text(
                    text = "Alias cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
            )
           }
           Spacer(modifier = Modifier.height(8.dp))
           OutlinedTextField(
                   value = content,
                   onValueChange = { content = it },
                   label = { Text("Content") },
                   modifier = Modifier.fillMaxWidth(),
                   minLines = 3
           )
          }
         },
         confirmButton = {
          TextButton(
                  onClick = {
                   if (alias.isBlank()) {
                    aliasError = true
                   } else {
                    onConfirm(alias, content)
                   }
                  }
          ) { Text("Save") }
         },
         dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
 )
}
