package com.searchlauncher.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit, policyText: String) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Privacy Policy") },
    text = {
      Box(modifier = Modifier.heightIn(max = 400.dp)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(policyText) }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun ConsentDialog(onConsentGiven: (Boolean) -> Unit, onViewPrivacyPolicy: () -> Unit) {
  AlertDialog(
    onDismissRequest = { /* Don't dismiss without choice */ },
    title = { Text("Help improvement SearchLauncher?") },
    text = {
      Column {
        Text(
          "By enabling error reporting, you help us identify and fix bugs. No personal data or search queries are collected."
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onViewPrivacyPolicy, contentPadding = PaddingValues(0.dp)) {
          Text("View Privacy Policy", style = MaterialTheme.typography.labelMedium)
        }
      }
    },
    confirmButton = { Button(onClick = { onConsentGiven(true) }) { Text("Enable") } },
    dismissButton = { TextButton(onClick = { onConsentGiven(false) }) { Text("No thanks") } },
  )
}
