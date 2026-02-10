package com.searchlauncher.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class MdBlock {
  data class Heading(val level: Int, val content: String) : MdBlock()

  data class Paragraph(val content: String) : MdBlock()

  data class BulletItem(val content: String) : MdBlock()
}

private fun parseMarkdownBlocks(text: String): List<MdBlock> {
  val blocks = mutableListOf<MdBlock>()
  val paragraphBuffer = StringBuilder()

  fun flushParagraph() {
    val content = paragraphBuffer.toString().trim()
    if (content.isNotEmpty()) {
      blocks.add(MdBlock.Paragraph(content))
    }
    paragraphBuffer.clear()
  }

  for (line in text.lines()) {
    val trimmed = line.trimEnd()
    when {
      trimmed.startsWith("###") -> {
        flushParagraph()
        blocks.add(MdBlock.Heading(3, trimmed.removePrefix("###").trim()))
      }
      trimmed.startsWith("##") -> {
        flushParagraph()
        blocks.add(MdBlock.Heading(2, trimmed.removePrefix("##").trim()))
      }
      trimmed.startsWith("#") -> {
        flushParagraph()
        blocks.add(MdBlock.Heading(1, trimmed.removePrefix("#").trim()))
      }
      trimmed.startsWith("- ") -> {
        flushParagraph()
        blocks.add(MdBlock.BulletItem(trimmed.removePrefix("- ").trim()))
      }
      trimmed.isEmpty() -> {
        flushParagraph()
      }
      else -> {
        if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(' ')
        paragraphBuffer.append(trimmed)
      }
    }
  }
  flushParagraph()
  return blocks
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
  return buildAnnotatedString {
    var i = 0
    while (i < text.length) {
      if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
        val end = text.indexOf("**", i + 2)
        if (end != -1) {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
          i = end + 2
          continue
        }
      }
      append(text[i])
      i++
    }
  }
}

@Composable
private fun MarkdownContent(text: String) {
  val blocks = remember(text) { parseMarkdownBlocks(text) }
  val colorScheme = MaterialTheme.colorScheme

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    for ((index, block) in blocks.withIndex()) {
      when (block) {
        is MdBlock.Heading -> {
          if (index > 0) Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = parseInlineMarkdown(block.content),
            style =
              when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
              },
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(2.dp))
        }
        is MdBlock.Paragraph -> {
          Text(
            text = parseInlineMarkdown(block.content),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
          )
        }
        is MdBlock.BulletItem -> {
          Row(modifier = Modifier.padding(start = 8.dp)) {
            Text(
              text = "•",
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(end = 8.dp),
            )
            Text(
              text = parseInlineMarkdown(block.content),
              style = MaterialTheme.typography.bodyMedium,
              color = colorScheme.onSurfaceVariant,
              lineHeight = 20.sp,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit, policyText: String) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Privacy Policy") },
    text = { Box(modifier = Modifier.heightIn(max = 400.dp)) { MarkdownContent(policyText) } },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
fun ConsentDialog(onConsentGiven: (Boolean) -> Unit, onViewPrivacyPolicy: () -> Unit) {
  AlertDialog(
    onDismissRequest = { /* Don't dismiss without choice */},
    title = { Text("Help improvement SearchLauncher?") },
    text = {
      Column {
        Text(
          "By enabling error reporting, you help us identify and fix bugs. No personal data or search queries are collected."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "SearchLauncher also shows live search suggestions from third-party services (e.g., Google) as you type. You can disable suggestions anytime in Settings → Privacy.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
